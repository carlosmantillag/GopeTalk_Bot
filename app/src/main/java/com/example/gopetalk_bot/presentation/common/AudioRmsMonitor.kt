package com.example.gopetalk_bot.presentation.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton for monitoring audio RMS levels across the app
 * This is a presentation layer utility for UI updates
 */
object AudioRmsMonitor {
    private val _rmsDbFlow = MutableStateFlow(0f)
    val rmsDbFlow = _rmsDbFlow.asStateFlow()

    fun updateRmsDb(rmsDb: Float) {
        _rmsDbFlow.value = rmsDb
    }
}
