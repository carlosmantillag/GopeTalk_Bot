package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.AudioLevel
import com.example.gopetalk_bot.domain.repositories.AudioRepository
import kotlinx.coroutines.flow.Flow

class MonitorAudioLevelUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute(): Flow<AudioLevel> {
        return audioRepository.getAudioLevelStream()
    }
}
