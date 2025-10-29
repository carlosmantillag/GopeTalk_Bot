package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.AudioDataSource
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.entities.AudioFormat
import com.example.gopetalk_bot.domain.entities.AudioLevel
import com.example.gopetalk_bot.domain.repositories.AudioRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AudioRepositoryImpl(
    private val audioDataSource: AudioDataSource
) : AudioRepository {

    private var audioLevelCallback: ((AudioDataSource.AudioLevelData) -> Unit)? = null
    private var recordingStoppedCallback: ((AudioDataSource.RecordedAudioData) -> Unit)? = null
    private var errorCallback: ((String, Throwable?) -> Unit)? = null

    override fun startMonitoring() {
        audioDataSource.startMonitoring(
            onAudioLevel = { audioLevelCallback?.invoke(it) },
            onRecordingStopped = { recordingStoppedCallback?.invoke(it) },
            onError = { msg, throwable -> errorCallback?.invoke(msg, throwable) }
        )
    }

    override fun stopMonitoring() {
        audioDataSource.stopMonitoring()
    }

    override fun pauseRecording() {
        audioDataSource.pauseRecording()
    }

    override fun resumeRecording() {
        audioDataSource.resumeRecording()
    }

    override fun getAudioLevelStream(): Flow<AudioLevel> = callbackFlow {
        audioLevelCallback = { audioLevelData ->
            trySend(AudioLevel(rmsDb = audioLevelData.rmsDb))
        }
        
        errorCallback = { msg, throwable ->
            close(Exception(msg, throwable))
        }

        awaitClose {
            audioLevelCallback = null
            errorCallback = null
        }
    }

    override fun getRecordedAudioStream(): Flow<AudioData> = callbackFlow {
        recordingStoppedCallback = { recordedAudioData ->
            trySend(
                AudioData(
                    file = recordedAudioData.file,
                    sampleRate = 16000,
                    channels = 1,
                    format = AudioFormat.PCM_16BIT
                )
            )
        }

        awaitClose {
            recordingStoppedCallback = null
        }
    }

    override fun release() {
        audioDataSource.release()
        audioLevelCallback = null
        recordingStoppedCallback = null
        errorCallback = null
    }
    
    override fun getAdaptiveStatus(): String {
        return audioDataSource.getAdaptiveStatus()
    }
    
    override fun resetAdaptiveSystem() {
        audioDataSource.resetAdaptiveSystem()
    }
    
    override fun forceRecalibration() {
        audioDataSource.forceRecalibration()
    }
}
