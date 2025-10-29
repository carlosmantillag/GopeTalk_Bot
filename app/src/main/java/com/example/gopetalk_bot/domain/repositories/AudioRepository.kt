package com.example.gopetalk_bot.domain.repositories

import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.entities.AudioLevel
import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    fun startMonitoring()
    fun stopMonitoring()
    fun pauseRecording()
    fun resumeRecording()

    fun getAudioLevelStream(): Flow<AudioLevel>
    fun getRecordedAudioStream(): Flow<AudioData>
    fun release()
    
    fun getAdaptiveStatus(): String
    fun resetAdaptiveSystem()
    fun forceRecalibration()
}
