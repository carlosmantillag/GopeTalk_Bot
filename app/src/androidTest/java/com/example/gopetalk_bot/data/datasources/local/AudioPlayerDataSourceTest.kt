package com.example.gopetalk_bot.data.datasources.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AudioPlayerDataSourceTest {

    private lateinit var audioPlayerDataSource: AudioPlayerDataSource
    private lateinit var mockFile: File
    private lateinit var mockListener: AudioPlayerDataSource.PlaybackListener

    @Before
    fun setup() {
        audioPlayerDataSource = AudioPlayerDataSource()
        mockListener = mockk(relaxed = true)
        mockFile = mockk {
            every { exists() } returns false
            every { path } returns "/sdcard/test.wav"
        }
    }

    @After
    fun tearDown() {
        audioPlayerDataSource.stopPlayback()
        clearAllMocks()
    }

    @Test
    fun playAudio_withNonExistentFile_shouldCallOnError() {
        every { mockFile.exists() } returns false

        audioPlayerDataSource.playAudio(mockFile, mockListener)

        verify { mockListener.onPlaybackError(any()) }
    }

    @Test
    fun stopPlayback_shouldNotThrowException() {
        audioPlayerDataSource.stopPlayback()
    }

    @Test
    fun multipleStopPlaybackCalls_shouldNotThrowException() {
        audioPlayerDataSource.stopPlayback()
        audioPlayerDataSource.stopPlayback()
    }
}
