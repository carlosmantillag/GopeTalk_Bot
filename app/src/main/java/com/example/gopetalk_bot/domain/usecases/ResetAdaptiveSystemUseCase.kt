package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository

class ResetAdaptiveSystemUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute() {
        audioRepository.resetAdaptiveSystem()
    }
}
