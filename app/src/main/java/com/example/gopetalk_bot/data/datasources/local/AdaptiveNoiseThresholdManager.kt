package com.example.gopetalk_bot.data.datasources.local

import android.util.Log
import kotlin.math.max
import kotlin.math.min

class AdaptiveNoiseThresholdManager(
    private val minThresholdDb: Float = 60f,
    private val maxThresholdDb: Float = 85f,
    private val thresholdMarginDb: Float = 10f,
    private val calibrationSamples: Int = 30,
    private val adaptationRate: Float = 0.1f
) {
    
    private companion object {
        const val TAG = "AdaptiveNoiseThreshold"
        const val QUIET_ENVIRONMENT_DB = 50f
        const val NORMAL_ENVIRONMENT_DB = 60f
        const val NOISY_ENVIRONMENT_DB = 70f
        const val VERY_NOISY_ENVIRONMENT_DB = 80f
    }
    
    private val ambientNoiseHistory = mutableListOf<Float>()
    
    @Volatile
    private var currentAmbientNoiseDb = NORMAL_ENVIRONMENT_DB
    
    @Volatile
    private var dynamicThresholdDb = NORMAL_ENVIRONMENT_DB + thresholdMarginDb
    
    @Volatile
    private var isCalibrated = false
    
    private var samplesSinceLastUpdate = 0
    private val recalibrationInterval = 100

    fun processAudioLevel(rmsDb: Float, isSpeechDetected: Boolean) {
        if (!isSpeechDetected) {
            updateAmbientNoise(rmsDb)
            samplesSinceLastUpdate++
            
            if (samplesSinceLastUpdate >= recalibrationInterval) {
                recalibrate()
                samplesSinceLastUpdate = 0
            }
        }
    }

    private fun updateAmbientNoise(rmsDb: Float) {
        if (!isCalibrated) {
            ambientNoiseHistory.add(rmsDb)
            
            if (ambientNoiseHistory.size >= calibrationSamples) {
                performInitialCalibration()
            }
        } else {
            currentAmbientNoiseDb = (adaptationRate * rmsDb) +
                                   ((1 - adaptationRate) * currentAmbientNoiseDb)
            updateDynamicThreshold()
        }
    }

    private fun performInitialCalibration() {
        currentAmbientNoiseDb = ambientNoiseHistory.average().toFloat()
        
        updateDynamicThreshold()
        
        isCalibrated = true
        
        val environment = getEnvironmentType()
        Log.i(TAG, "Calibración inicial completada:")
        Log.i(TAG, "  - Ruido ambiente: %.1f dB".format(currentAmbientNoiseDb))
        Log.i(TAG, "  - Umbral dinámico: %.1f dB".format(dynamicThresholdDb))
        Log.i(TAG, "  - Tipo de ambiente: $environment")
        
        ambientNoiseHistory.clear()
    }

    private fun recalibrate() {
        updateDynamicThreshold()
        
        val environment = getEnvironmentType()
        Log.d(TAG, "Recalibración automática:")
        Log.d(TAG, "  - Ruido ambiente: %.1f dB".format(currentAmbientNoiseDb))
        Log.d(TAG, "  - Umbral ajustado: %.1f dB".format(dynamicThresholdDb))
        Log.d(TAG, "  - Ambiente: $environment")
    }

    private fun updateDynamicThreshold() {
        val calculatedThreshold = currentAmbientNoiseDb + thresholdMarginDb
        
        dynamicThresholdDb = max(minThresholdDb, min(maxThresholdDb, calculatedThreshold))
    }

    fun getCurrentThreshold(): Float {
        return if (isCalibrated) {
            dynamicThresholdDb
        } else {
            NORMAL_ENVIRONMENT_DB + thresholdMarginDb
        }
    }

    fun getAmbientNoiseLevel(): Float {
        return currentAmbientNoiseDb
    }

    fun isCalibrated(): Boolean {
        return isCalibrated
    }

    fun getEnvironmentType(): String {
        return when {
            currentAmbientNoiseDb < QUIET_ENVIRONMENT_DB -> "Silencioso"
            currentAmbientNoiseDb < NORMAL_ENVIRONMENT_DB -> "Normal"
            currentAmbientNoiseDb < NOISY_ENVIRONMENT_DB -> "Ruidoso"
            currentAmbientNoiseDb < VERY_NOISY_ENVIRONMENT_DB -> "Muy Ruidoso"
            else -> "Extremadamente Ruidoso"
        }
    }

}
