package com.example.gopetalk_bot.domain.entities

/**
 * Entity representing audio level measurements
 */
data class AudioLevel(
    val rmsDb: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isSilence(thresholdDb: Float): Boolean = rmsDb < thresholdDb
    fun isSound(thresholdDb: Float): Boolean = rmsDb >= thresholdDb
}
