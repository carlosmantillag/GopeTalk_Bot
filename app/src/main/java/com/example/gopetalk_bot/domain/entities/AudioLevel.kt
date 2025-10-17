package com.example.gopetalk_bot.domain.entities

data class AudioLevel(
    val rmsDb: Float,
    val timestamp: Long = System.currentTimeMillis()
)
