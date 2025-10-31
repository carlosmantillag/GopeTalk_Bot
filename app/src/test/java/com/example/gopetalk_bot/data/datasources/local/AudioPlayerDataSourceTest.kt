package com.example.gopetalk_bot.data.datasources.local

import android.media.MediaPlayer
import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class AudioPlayerDataSourceTest {

    private lateinit var dataSource: AudioPlayerDataSource
    private lateinit var listener: AudioPlayerDataSource.PlaybackListener

    @Before
    fun setup() {
        
        mockkStatic("android.util.Log")
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.d(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.i(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.v(any(), any()) } returns 0
        every { Log.v(any(), any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
        every { Log.wtf(any(), any<String>()) } returns 0
        every { Log.wtf(any(), any<Throwable>()) } returns 0
        every { Log.wtf(any(), any(), any()) } returns 0
        
        mockkConstructor(MediaPlayer::class)
        listener = mockk(relaxed = true)
        dataSource = AudioPlayerDataSource()
    }

    @After
    fun tearDown() {
        unmockkConstructor(MediaPlayer::class)
        unmockkStatic("android.util.Log")
    }

    @Test
    fun `playAudio should notify error when file does not exist`() {
        val nonexistentFile = File("nonexistent-file.wav")

        dataSource.playAudio(nonexistentFile, listener)

        verify { listener.onPlaybackError(match { it.contains("does not exist") }) }
    }


    @Test
    fun `playAudio should handle media player exception`() {
        val tempFile = File.createTempFile("audio_test", ".wav")

        every { anyConstructed<MediaPlayer>().setAudioAttributes(any()) } answers { Unit }
        every { anyConstructed<MediaPlayer>().setDataSource(any<String>()) } throws RuntimeException("boom")
        every { anyConstructed<MediaPlayer>().release() } just Runs

        dataSource.playAudio(tempFile, listener)

        verify { listener.onPlaybackError(match { it.contains("Error playing audio") }) }

        tempFile.delete()
    }

    @Test
    fun `isPlaying should return false when no media player`() {
        val result = dataSource.isPlaying()
        assertThat(result).isFalse()
    }
    
    @Test
    fun `stopPlayback should handle null media player`() {
        
        dataSource.stopPlayback()
    }
    
    @Test
    fun `release should call stopPlayback`() {
        
        dataSource.release()
    }

    

    @Test
    fun `isPlaying should return false after release`() {
        dataSource.release()
        
        val result = dataSource.isPlaying()
        
        assertThat(result).isFalse()
    }

    @Test
    fun `multiple release calls should not crash`() {
        dataSource.release()
        dataSource.release()
        dataSource.release()
        
        
        assertThat(dataSource.isPlaying()).isFalse()
    }
}
