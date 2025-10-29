package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository

class GetAdaptiveStatusUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute(): String {
        return audioRepository.getAdaptiveStatus()
    }
}
