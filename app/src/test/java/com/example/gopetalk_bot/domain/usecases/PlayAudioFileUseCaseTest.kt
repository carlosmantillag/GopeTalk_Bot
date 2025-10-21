package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class PlayAudioFileUseCaseTest {

    private lateinit var audioPlayerRepository: AudioPlayerRepository
    private lateinit var useCase: PlayAudioFileUseCase

    @Before
    fun setup() {
        audioPlayerRepository = mockk(relaxed = true)
        useCase = PlayAudioFileUseCase(audioPlayerRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call playAudio on repository with correct parameters`() {
        val mockFile = mockk<File>(relaxed = true)
        val mockListener = mockk<AudioPlayerRepository.PlaybackListener>(relaxed = true)

        useCase.execute(mockFile, mockListener)

        verify { audioPlayerRepository.playAudio(mockFile, mockListener) }
    }

    @Test
    fun `execute should handle multiple files`() {
        val mockFile1 = mockk<File>(relaxed = true)
        val mockFile2 = mockk<File>(relaxed = true)
        val mockListener = mockk<AudioPlayerRepository.PlaybackListener>(relaxed = true)

        useCase.execute(mockFile1, mockListener)
        useCase.execute(mockFile2, mockListener)

        verify { audioPlayerRepository.playAudio(mockFile1, mockListener) }
        verify { audioPlayerRepository.playAudio(mockFile2, mockListener) }
    }
}
