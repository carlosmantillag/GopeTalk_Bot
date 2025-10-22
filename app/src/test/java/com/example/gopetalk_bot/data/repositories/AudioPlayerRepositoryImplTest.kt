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

    @Test
    fun `playAudio should handle multiple consecutive calls`() {
        val mockFile1 = mockk<File>(relaxed = true)
        val mockFile2 = mockk<File>(relaxed = true)
        val listener = mockk<AudioPlayerRepository.PlaybackListener>(relaxed = true)

        repository.playAudio(mockFile1, listener)
        repository.playAudio(mockFile2, listener)

        verify { audioPlayerDataSource.playAudio(mockFile1, any()) }
        verify { audioPlayerDataSource.playAudio(mockFile2, any()) }
    }

    @Test
    fun `stopPlayback should be callable multiple times`() {
        repository.stopPlayback()
        repository.stopPlayback()
        repository.stopPlayback()

        verify(exactly = 3) { audioPlayerDataSource.stopPlayback() }
    }

    @Test
    fun `release should be callable multiple times`() {
        repository.release()
        repository.release()

        verify(exactly = 2) { audioPlayerDataSource.release() }
    }

    @Test
    fun `isPlaying should be idempotent`() {
        every { audioPlayerDataSource.isPlaying() } returns true

        val result1 = repository.isPlaying()
        val result2 = repository.isPlaying()

        assertThat(result1).isEqualTo(result2)
        verify(exactly = 2) { audioPlayerDataSource.isPlaying() }
    }

    @Test
    fun `playAudio with different listeners should work`() {
        val mockFile = mockk<File>(relaxed = true)
        val listener1 = mockk<AudioPlayerRepository.PlaybackListener>(relaxed = true)
        val listener2 = mockk<AudioPlayerRepository.PlaybackListener>(relaxed = true)

        repository.playAudio(mockFile, listener1)
        repository.playAudio(mockFile, listener2)

        verify(exactly = 2) { audioPlayerDataSource.playAudio(mockFile, any()) }
    }

    @Test
    fun `onPlaybackError should propagate different error messages`() {
        val mockFile = mockk<File>(relaxed = true)
        var capturedListener: AudioPlayerDataSource.PlaybackListener? = null
        val errorMessages = mutableListOf<String>()

        every { audioPlayerDataSource.playAudio(any(), any()) } answers {
            capturedListener = secondArg()
        }

        val listener = object : AudioPlayerRepository.PlaybackListener {
            override fun onPlaybackStarted() {}
            override fun onPlaybackCompleted() {}
            override fun onPlaybackError(error: String) { errorMessages.add(error) }
        }

        repository.playAudio(mockFile, listener)
        capturedListener?.onPlaybackError("Error 1")
        capturedListener?.onPlaybackError("Error 2")

        assertThat(errorMessages).containsExactly("Error 1", "Error 2")
    }

    @Test
    fun `playback lifecycle should work correctly`() {
        val mockFile = mockk<File>(relaxed = true)
        var capturedListener: AudioPlayerDataSource.PlaybackListener? = null
        val events = mutableListOf<String>()

        every { audioPlayerDataSource.playAudio(any(), any()) } answers {
            capturedListener = secondArg()
        }

        val listener = object : AudioPlayerRepository.PlaybackListener {
            override fun onPlaybackStarted() { events.add("started") }
            override fun onPlaybackCompleted() { events.add("completed") }
            override fun onPlaybackError(error: String) { events.add("error") }
        }

        repository.playAudio(mockFile, listener)
        capturedListener?.onPlaybackStarted()
        capturedListener?.onPlaybackCompleted()

        assertThat(events).containsExactly("started", "completed").inOrder()
    }

    @Test
    fun `stopPlayback after release should work`() {
        repository.release()
        repository.stopPlayback()

        verify { audioPlayerDataSource.release() }
        verify { audioPlayerDataSource.stopPlayback() }
    }
}
