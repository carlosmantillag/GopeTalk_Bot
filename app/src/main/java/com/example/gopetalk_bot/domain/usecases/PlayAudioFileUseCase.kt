package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import java.io.File

class PlayAudioFileUseCase(
    private val audioPlayerRepository: AudioPlayerRepository
) {
    fun execute(audioFile: File, listener: AudioPlayerRepository.PlaybackListener) {
        audioPlayerRepository.playAudio(audioFile, listener)
    }
}
