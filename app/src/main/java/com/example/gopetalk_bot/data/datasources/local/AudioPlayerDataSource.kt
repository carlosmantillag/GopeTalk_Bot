package com.example.gopetalk_bot.data.datasources.local

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File

class AudioPlayerDataSource {
    private var mediaPlayer: MediaPlayer? = null
    private var currentListener: PlaybackListener? = null

    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onPlaybackCompleted()
        fun onPlaybackError(error: String)
    }

    fun playAudio(audioFile: File, listener: PlaybackListener) {
        if (!audioFile.exists()) {
            listener.onPlaybackError("Audio file does not exist: ${audioFile.path}")
            return
        }

        stopPlayback()

        currentListener = listener

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(audioFile.path)
                
                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared, starting playback")
                    start()
                    listener.onPlaybackStarted()
                }

                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    listener.onPlaybackCompleted()
                    release()
                    mediaPlayer = null
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    listener.onPlaybackError("MediaPlayer error: $what, $extra")
                    release()
                    mediaPlayer = null
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            listener.onPlaybackError("Error playing audio: ${e.message}")
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stopPlayback() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping playback", e)
            }
            mediaPlayer = null
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun release() {
        stopPlayback()
    }

    companion object {
        private const val TAG = "AudioPlayerDataSource"
    }
}
