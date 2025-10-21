package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.AudioLevel
import com.example.gopetalk_bot.domain.repositories.AudioRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class MonitorAudioLevelUseCaseTest {

    private lateinit var audioRepository: AudioRepository
    private lateinit var useCase: MonitorAudioLevelUseCase

    @Before
    fun setup() {
        audioRepository = mockk(relaxed = true)
        useCase = MonitorAudioLevelUseCase(audioRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should return audio level stream from repository`() {
        val audioLevel = AudioLevel(50.0f)
        val expectedFlow = flowOf(audioLevel)
        every { audioRepository.getAudioLevelStream() } returns expectedFlow

        val result = useCase.execute()

        assertThat(result).isEqualTo(expectedFlow)
        verify { audioRepository.getAudioLevelStream() }
    }
}
