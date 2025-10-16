package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository

/**
 * Use case for pausing audio recording
 */
class PauseAudioRecordingUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute() {
        audioRepository.pauseRecording()
    }
}
