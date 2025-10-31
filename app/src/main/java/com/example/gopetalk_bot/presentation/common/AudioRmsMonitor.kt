package com.example.gopetalk_bot.presentation.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioRmsMonitor {
    private val _rmsDbFlow = MutableStateFlow(0f)
    val rmsDbFlow = _rmsDbFlow.asStateFlow()

    fun updateRmsDb(rmsDb: Float) {
        _rmsDbFlow.value = rmsDb
    }
    
    
    internal fun reset() {
        _rmsDbFlow.value = 0f
    }
}
