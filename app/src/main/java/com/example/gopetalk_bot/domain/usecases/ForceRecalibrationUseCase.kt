package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository

class ForceRecalibrationUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute() {
        audioRepository.forceRecalibration()
    }
}
