package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class PauseAudioRecordingUseCaseTest {

    private lateinit var audioRepository: AudioRepository
    private lateinit var useCase: PauseAudioRecordingUseCase

    @Before
    fun setup() {
        audioRepository = mockk(relaxed = true)
        useCase = PauseAudioRecordingUseCase(audioRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call pauseRecording on repository`() {
        useCase.execute()

        verify { audioRepository.pauseRecording() }
    }

    @Test
    fun `execute should be callable multiple times`() {
        useCase.execute()
        useCase.execute()
        useCase.execute()

        verify(exactly = 3) { audioRepository.pauseRecording() }
    }
}
