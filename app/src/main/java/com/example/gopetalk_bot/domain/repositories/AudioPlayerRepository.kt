package com.example.gopetalk_bot.domain.repositories

import java.io.File

interface AudioPlayerRepository {
    fun playAudio(audioFile: File, listener: PlaybackListener)
    fun stopPlayback()
    fun isPlaying(): Boolean
    fun release()
    
    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onPlaybackCompleted()
        fun onPlaybackError(error: String)
    }
}
