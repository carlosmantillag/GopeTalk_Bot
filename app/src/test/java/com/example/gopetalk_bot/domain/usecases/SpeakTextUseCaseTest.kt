package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class SpeakTextUseCaseTest {

    private lateinit var textToSpeechRepository: TextToSpeechRepository
    private lateinit var useCase: SpeakTextUseCase

    @Before
    fun setup() {
        textToSpeechRepository = mockk(relaxed = true)
        useCase = SpeakTextUseCase(textToSpeechRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository speak with correct text`() {
        val text = "Hello World"
        val utteranceId = "test-id"

        useCase.execute(text, utteranceId)

        verify { textToSpeechRepository.speak(text, utteranceId) }
    }

    @Test
    fun `execute should handle empty text`() {
        val text = ""
        val utteranceId = "test-id"

        useCase.execute(text, utteranceId)

        verify { textToSpeechRepository.speak(text, utteranceId) }
    }

    @Test
    fun `execute should handle special characters in text`() {
        val text = "¡Hola! ¿Cómo estás?"
        val utteranceId = "test-id"

        useCase.execute(text, utteranceId)

        verify { textToSpeechRepository.speak(text, utteranceId) }
    }

    @Test
    fun `execute should handle long text`() {
        val text = "a".repeat(1000)
        val utteranceId = "test-id"

        useCase.execute(text, utteranceId)

        verify { textToSpeechRepository.speak(text, utteranceId) }
    }
}
