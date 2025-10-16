package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository

/**
 * Use case for stopping audio monitoring
 */
class StopAudioMonitoringUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute() {
        audioRepository.stopMonitoring()
        audioRepository.release()
    }
}
