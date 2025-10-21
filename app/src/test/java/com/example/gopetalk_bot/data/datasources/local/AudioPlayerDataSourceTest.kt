package com.example.gopetalk_bot.data.datasources.local

import android.media.MediaPlayer
import com.google.common.truth.Truth.assertThat
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
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
        dataSource = AudioPlayerDataSource()
        listener = mockk(relaxed = true)
        mockkConstructor(MediaPlayer::class)
    }

    @After
    fun tearDown() {
        unmockkConstructor(MediaPlayer::class)
    }

    @Test
    fun `playAudio should notify error when file does not exist`() {
        val nonexistentFile = File("nonexistent-file.wav")

        dataSource.playAudio(nonexistentFile, listener)

        verify { listener.onPlaybackError(match { it.contains("does not exist") }) }
        verify { anyConstructed<MediaPlayer>() wasNot Called }
    }


    @Test
    fun `playAudio should handle media player exception`() {
        val tempFile = File.createTempFile("audio_test", ".wav")

        every { anyConstructed<MediaPlayer>().setAudioAttributes(any()) } answers { Unit }
        every { anyConstructed<MediaPlayer>().setDataSource(any<String>()) } throws RuntimeException("boom")
        every { anyConstructed<MediaPlayer>().release() } just Runs

        dataSource.playAudio(tempFile, listener)

        verify { listener.onPlaybackError(match { it.contains("Error playing audio") }) }
        verify { anyConstructed<MediaPlayer>().release() }

        tempFile.delete()
    }

    @Test
    fun `isPlaying should reflect media player state`() {
        every { anyConstructed<MediaPlayer>().isPlaying } returns true
        every { anyConstructed<MediaPlayer>().release() } just Runs
        every { anyConstructed<MediaPlayer>().setAudioAttributes(any()) } just Runs
        every { anyConstructed<MediaPlayer>().setDataSource(any<String>()) } just Runs
        every { anyConstructed<MediaPlayer>().setOnPreparedListener(any()) } just Runs
        every { anyConstructed<MediaPlayer>().setOnCompletionListener(any()) } just Runs
        every { anyConstructed<MediaPlayer>().setOnErrorListener(any()) } answers { true }
        every { anyConstructed<MediaPlayer>().prepareAsync() } just Runs
        every { anyConstructed<MediaPlayer>().start() } just Runs

        val tempFile = File.createTempFile("audio_test", ".wav")
        dataSource.playAudio(tempFile, listener)

        val result = dataSource.isPlaying()

        assertThat(result).isTrue()

        tempFile.delete()
    }
}
