package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class StopAudioMonitoringUseCaseTest {

    private lateinit var audioRepository: AudioRepository
    private lateinit var useCase: StopAudioMonitoringUseCase

    @Before
    fun setup() {
        audioRepository = mockk(relaxed = true)
        useCase = StopAudioMonitoringUseCase(audioRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository stopMonitoring and release`() {
        useCase.execute()

        verify { audioRepository.stopMonitoring() }
        verify { audioRepository.release() }
    }

    @Test
    fun `execute should call methods in correct order`() {
        useCase.execute()

        verifyOrder {
            audioRepository.stopMonitoring()
            audioRepository.release()
        }
    }

    @Test
    fun `execute should handle multiple calls`() {
        useCase.execute()
        useCase.execute()

        verify(exactly = 2) { audioRepository.stopMonitoring() }
        verify(exactly = 2) { audioRepository.release() }
    }
}
