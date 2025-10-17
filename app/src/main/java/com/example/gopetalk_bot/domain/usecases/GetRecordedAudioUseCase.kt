package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.repositories.AudioRepository
import kotlinx.coroutines.flow.Flow

class GetRecordedAudioUseCase(
    private val audioRepository: AudioRepository
) {
    fun execute(): Flow<AudioData> {
        return audioRepository.getRecordedAudioStream()
    }
}
