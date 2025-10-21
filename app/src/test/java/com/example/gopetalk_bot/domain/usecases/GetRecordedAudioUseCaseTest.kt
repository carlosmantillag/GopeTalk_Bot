package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.repositories.AudioRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class GetRecordedAudioUseCaseTest {

    private lateinit var audioRepository: AudioRepository
    private lateinit var useCase: GetRecordedAudioUseCase

    @Before
    fun setup() {
        audioRepository = mockk(relaxed = true)
        useCase = GetRecordedAudioUseCase(audioRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should return audio stream from repository`() {
        val mockFile = mockk<java.io.File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        val expectedFlow = flowOf(audioData)
        every { audioRepository.getRecordedAudioStream() } returns expectedFlow

        val result = useCase.execute()

        assertThat(result).isEqualTo(expectedFlow)
        verify { audioRepository.getRecordedAudioStream() }
    }
}
