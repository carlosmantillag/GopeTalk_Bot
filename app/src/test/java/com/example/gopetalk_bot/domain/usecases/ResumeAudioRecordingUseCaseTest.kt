package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.AudioRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class ResumeAudioRecordingUseCaseTest {

    private lateinit var audioRepository: AudioRepository
    private lateinit var useCase: ResumeAudioRecordingUseCase

    @Before
    fun setup() {
        audioRepository = mockk(relaxed = true)
        useCase = ResumeAudioRecordingUseCase(audioRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call resumeRecording on repository`() {
        useCase.execute()

        verify { audioRepository.resumeRecording() }
    }

    @Test
    fun `execute should be callable multiple times`() {
        useCase.execute()
        useCase.execute()
        useCase.execute()

        verify(exactly = 3) { audioRepository.resumeRecording() }
    }
}
