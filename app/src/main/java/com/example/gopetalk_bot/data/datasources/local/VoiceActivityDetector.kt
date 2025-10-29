package com.example.gopetalk_bot.data.datasources.local

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class VoiceActivityDetector(
    private val minVoiceDurationMs: Long = 250,
    private val maxContinuousDurationMs: Long = 5000,
    private val minConsecutiveSamples: Int = 4,
    private val variabilityThreshold: Float = 0.4f,
    private val zeroCrossingRateMin: Int = 30,
    private val zeroCrossingRateMax: Int = 200
) {
    
    private companion object {
        const val TAG = "VoiceActivityDetector"
        const val HISTORY_SIZE = 10
    }
    
    private val audioLevelHistory = mutableListOf<Float>()
    
    private var consecutiveSamplesAboveThreshold = 0
    
    private var firstSoundTime = 0L
    
    private var lastAudioData: ShortArray? = null

    fun isVoiceDetected(
        rmsDb: Float,
        isAboveThreshold: Boolean,
        audioData: ShortArray
    ): Boolean {
        lastAudioData = audioData
        
        if (!isAboveThreshold) {
            reset()
            return false
        }
        
        audioLevelHistory.add(rmsDb)
        if (audioLevelHistory.size > HISTORY_SIZE) {
            audioLevelHistory.removeAt(0)
        }
        
        consecutiveSamplesAboveThreshold++
        
        if (firstSoundTime == 0L) {
            firstSoundTime = System.currentTimeMillis()
        }
        
        return checkVoiceCriteria()
    }

    private fun checkVoiceCriteria(): Boolean {
        val duration = System.currentTimeMillis() - firstSoundTime
        var score = 0
        val reasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        if (duration < minVoiceDurationMs) {
            Log.v(TAG, "⏱️ Duración: ${duration}ms < ${minVoiceDurationMs}ms - Esperando...")
            return false
        }
        
        if (duration > maxContinuousDurationMs) {
            Log.d(TAG, "✗ RUIDO CONSTANTE - Duración: ${duration}ms > ${maxContinuousDurationMs}ms (ventilador)")
            return false
        }
        
        if (consecutiveSamplesAboveThreshold >= minConsecutiveSamples) {
            score++
            reasons.add("muestras:$consecutiveSamplesAboveThreshold")
        } else {
            warnings.add("muestras:$consecutiveSamplesAboveThreshold<$minConsecutiveSamples")
        }
        
        if (audioLevelHistory.size >= 5) {
            val variability = calculateVariability()
            if (variability >= variabilityThreshold) {
                score++
                reasons.add("variabilidad:${String.format("%.1f", variability)}dB")
            } else {
                warnings.add("variabilidad:${String.format("%.1f", variability)}dB<${variabilityThreshold}dB")
                Log.v(TAG, "⚠️ Baja variabilidad: ${String.format("%.1f", variability)} dB")
            }
        } else {
            warnings.add("datos-insuficientes:${audioLevelHistory.size}/5")
        }
        
        var zcrValid = false
        lastAudioData?.let { data ->
            val zcr = calculateZeroCrossingRate(data)
            if (zcr in zeroCrossingRateMin..zeroCrossingRateMax) {
                score++
                zcrValid = true
                reasons.add("zcr:$zcr")
            } else {
                warnings.add("zcr:$zcr fuera de [$zeroCrossingRateMin-$zeroCrossingRateMax]")
                Log.v(TAG, "⚠️ ZCR fuera de rango de voz: $zcr (ventilador/ruido mecánico)")
            }
        }

        val isVoice = zcrValid && score >= 2
        
        if (isVoice) {
            Log.d(TAG, "✓ VOZ CONFIRMADA - Duración: ${duration}ms, Score: $score/3")
            Log.d(TAG, "  ✓ ${reasons.joinToString(", ")}")
        } else {
            Log.d(TAG, "✗ NO ES VOZ - Score: $score/3 (necesita ≥2)")
            if (reasons.isNotEmpty()) {
                Log.d(TAG, "  ✓ ${reasons.joinToString(", ")}")
            }
            if (warnings.isNotEmpty()) {
                Log.d(TAG, "  ✗ ${warnings.joinToString(", ")}")
            }
        }
        
        return isVoice
    }

    private fun calculateVariability(): Float {
        if (audioLevelHistory.size < 2) return 0f
        
        val mean = audioLevelHistory.average().toFloat()
        val variance = audioLevelHistory.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    private fun calculateZeroCrossingRate(audioData: ShortArray): Int {
        var crossings = 0
        for (i in 1 until audioData.size) {
            if ((audioData[i] >= 0 && audioData[i - 1] < 0) ||
                (audioData[i] < 0 && audioData[i - 1] >= 0)) {
                crossings++
            }
        }
        return crossings
    }

    fun reset() {
        consecutiveSamplesAboveThreshold = 0
        firstSoundTime = 0L
        audioLevelHistory.clear()
    }

    fun getDebugInfo(): String {
        val duration = if (firstSoundTime > 0) {
            System.currentTimeMillis() - firstSoundTime
        } else 0
        
        val variability = if (audioLevelHistory.size >= 2) {
            String.format("%.1f", calculateVariability())
        } else "N/A"
        
        val zcr = lastAudioData?.let { calculateZeroCrossingRate(it) } ?: 0
        
        return buildString {
            appendLine("Voice Activity Detector:")
            appendLine("  - Duración: ${duration}ms")
            appendLine("  - Muestras consecutivas: $consecutiveSamplesAboveThreshold")
            appendLine("  - Variabilidad: $variability dB")
            appendLine("  - Zero Crossing Rate: $zcr")
            appendLine("  - Historial: ${audioLevelHistory.size} muestras")
        }
    }
}
