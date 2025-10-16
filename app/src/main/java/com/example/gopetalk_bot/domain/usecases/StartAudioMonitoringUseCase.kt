package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository

/**
 * Use case for starting audio monitoring
 */
class StartAudioMonitoringUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute() {
        audioRepository.startMonitoring()
    }
}
