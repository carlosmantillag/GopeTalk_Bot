package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository

/**
 * Use case for resuming audio recording
 */
class ResumeAudioRecordingUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute() {
        audioRepository.resumeRecording()
    }
}
