package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class StartAudioMonitoringUseCaseTest {

    private lateinit var audioRepository: AudioRepository
    private lateinit var useCase: StartAudioMonitoringUseCase

    @Before
    fun setup() {
        audioRepository = mockk(relaxed = true)
        useCase = StartAudioMonitoringUseCase(audioRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository startMonitoring`() {
        useCase.execute()

        verify { audioRepository.startMonitoring() }
    }

    @Test
    fun `execute should handle multiple calls`() {
        useCase.execute()
        useCase.execute()

        verify(exactly = 2) { audioRepository.startMonitoring() }
    }
}
