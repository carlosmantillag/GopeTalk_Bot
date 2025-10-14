package com.example.gopetalk_bot.main

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AudioRmsMonitor {
    private val _rmsDbFlow = MutableSharedFlow<Float>(replay = 1)
    val rmsDbFlow = _rmsDbFlow.asSharedFlow()

    suspend fun updateRmsDb(rmsDb: Float) {
        _rmsDbFlow.emit(rmsDb)
    }
}
