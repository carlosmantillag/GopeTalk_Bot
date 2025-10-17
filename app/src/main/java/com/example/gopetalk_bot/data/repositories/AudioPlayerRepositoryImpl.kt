package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.AudioPlayerDataSource
import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import java.io.File

class AudioPlayerRepositoryImpl(
    private val audioPlayerDataSource: AudioPlayerDataSource
) : AudioPlayerRepository {
    
    override fun playAudio(audioFile: File, listener: AudioPlayerRepository.PlaybackListener) {
        audioPlayerDataSource.playAudio(audioFile, object : AudioPlayerDataSource.PlaybackListener {
            override fun onPlaybackStarted() {
                listener.onPlaybackStarted()
            }
            
            override fun onPlaybackCompleted() {
                listener.onPlaybackCompleted()
            }
            
            override fun onPlaybackError(error: String) {
                listener.onPlaybackError(error)
            }
        })
    }
    
    override fun stopPlayback() {
        audioPlayerDataSource.stopPlayback()
    }
    
    override fun isPlaying(): Boolean {
        return audioPlayerDataSource.isPlaying()
    }
    
    override fun release() {
        audioPlayerDataSource.release()
    }
}
