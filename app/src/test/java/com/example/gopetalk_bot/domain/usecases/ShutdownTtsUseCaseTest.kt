package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class ShutdownTtsUseCaseTest {

    private lateinit var textToSpeechRepository: TextToSpeechRepository
    private lateinit var useCase: ShutdownTtsUseCase

    @Before
    fun setup() {
        textToSpeechRepository = mockk(relaxed = true)
        useCase = ShutdownTtsUseCase(textToSpeechRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository shutdown`() {
        useCase.execute()

        verify { textToSpeechRepository.shutdown() }
    }

    @Test
    fun `execute should handle multiple shutdown calls`() {
        useCase.execute()
        useCase.execute()

        verify(exactly = 2) { textToSpeechRepository.shutdown() }
    }
}
