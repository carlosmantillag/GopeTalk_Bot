package com.example.gopetalk_bot.domain.repositories

import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.entities.AudioLevel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for audio operations
 */
interface AudioRepository {
    /**
     * Start monitoring audio levels
     */
    fun startMonitoring()
    
    /**
     * Stop monitoring audio levels
     */
    fun stopMonitoring()
    
    /**
     * Get audio level stream
     */
    fun getAudioLevelStream(): Flow<AudioLevel>
    
    /**
     * Get recorded audio when available
     */
    fun getRecordedAudioStream(): Flow<AudioData>
    
    /**
     * Release all audio resources
     */
    fun release()
}
