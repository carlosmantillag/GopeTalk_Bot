package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.AudioPlayerDataSource
import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import java.io.File

class AudioPlayerRepositoryImplTest {

    private lateinit var audioPlayerDataSource: AudioPlayerDataSource
    private lateinit var repository: AudioPlayerRepositoryImpl

    @Before
    fun setup() {
        audioPlayerDataSource = mockk(relaxed = true)
        repository = AudioPlayerRepositoryImpl(audioPlayerDataSource)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `playAudio should call data source with correct parameters`() {
        val mockFile = mockk<File>(relaxed = true)
        val listener = mockk<AudioPlayerRepository.PlaybackListener>(relaxed = true)

        repository.playAudio(mockFile, listener)

        verify { audioPlayerDataSource.playAudio(mockFile, any()) }
    }

    @Test
    fun `playAudio should propagate onPlaybackStarted event`() {
        val mockFile = mockk<File>(relaxed = true)
        var capturedListener: AudioPlayerDataSource.PlaybackListener? = null
        var startedCalled = false

        every { audioPlayerDataSource.playAudio(any(), any()) } answers {
            capturedListener = secondArg()
        }

        val listener = object : AudioPlayerRepository.PlaybackListener {
            override fun onPlaybackStarted() { startedCalled = true }
            override fun onPlaybackCompleted() {}
            override fun onPlaybackError(error: String) {}
        }

        repository.playAudio(mockFile, listener)
        capturedListener?.onPlaybackStarted()

        assertThat(startedCalled).isTrue()
    }

    @Test
    fun `playAudio should propagate onPlaybackCompleted event`() {
        val mockFile = mockk<File>(relaxed = true)
        var capturedListener: AudioPlayerDataSource.PlaybackListener? = null
        var completedCalled = false

        every { audioPlayerDataSource.playAudio(any(), any()) } answers {
            capturedListener = secondArg()
        }

        val listener = object : AudioPlayerRepository.PlaybackListener {
            override fun onPlaybackStarted() {}
            override fun onPlaybackCompleted() { completedCalled = true }
            override fun onPlaybackError(error: String) {}
        }

        repository.playAudio(mockFile, listener)
        capturedListener?.onPlaybackCompleted()

        assertThat(completedCalled).isTrue()
    }

    @Test
    fun `playAudio should propagate onPlaybackError event`() {
        val mockFile = mockk<File>(relaxed = true)
        var capturedListener: AudioPlayerDataSource.PlaybackListener? = null
        var errorMessage: String? = null

        every { audioPlayerDataSource.playAudio(any(), any()) } answers {
            capturedListener = secondArg()
        }

        val listener = object : AudioPlayerRepository.PlaybackListener {
            override fun onPlaybackStarted() {}
            override fun onPlaybackCompleted() {}
            override fun onPlaybackError(error: String) { errorMessage = error }
        }

        repository.playAudio(mockFile, listener)
        capturedListener?.onPlaybackError("Playback failed")

        assertThat(errorMessage).isEqualTo("Playback failed")
    }

    @Test
    fun `stopPlayback should call data source stopPlayback`() {
        repository.stopPlayback()

        verify { audioPlayerDataSource.stopPlayback() }
    }

    @Test
    fun `isPlaying should return true when data source is playing`() {
        every { audioPlayerDataSource.isPlaying() } returns true

        val result = repository.isPlaying()

        assertThat(result).isTrue()
        verify { audioPlayerDataSource.isPlaying() }
    }

    @Test
    fun `isPlaying should return false when data source is not playing`() {
        every { audioPlayerDataSource.isPlaying() } returns false

        val result = repository.isPlaying()

        assertThat(result).isFalse()
    }

    @Test
    fun `release should call data source release`() {
        repository.release()

        verify { audioPlayerDataSource.release() }
    }
}
