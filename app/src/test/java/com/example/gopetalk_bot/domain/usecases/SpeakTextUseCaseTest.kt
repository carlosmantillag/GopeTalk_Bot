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

    @Test
    fun `execute should handle multiple consecutive calls`() {
        useCase.execute("Text 1", "id-1")
        useCase.execute("Text 2", "id-2")
        useCase.execute("Text 3", "id-3")

        verify { textToSpeechRepository.speak("Text 1", "id-1") }
        verify { textToSpeechRepository.speak("Text 2", "id-2") }
        verify { textToSpeechRepository.speak("Text 3", "id-3") }
    }

    @Test
    fun `execute should handle text with numbers`() {
        val text = "The year is 2025"
        val utteranceId = "test-id"

        useCase.execute(text, utteranceId)

        verify { textToSpeechRepository.speak(text, utteranceId) }
    }

    @Test
    fun `execute should handle text with punctuation`() {
        val text = "Hello, world! How are you?"
        val utteranceId = "test-id"

        useCase.execute(text, utteranceId)

        verify { textToSpeechRepository.speak(text, utteranceId) }
    }

    @Test
    fun `execute should handle multiline text`() {
        val text = "Line 1\nLine 2\nLine 3"
        val utteranceId = "test-id"

        useCase.execute(text, utteranceId)

        verify { textToSpeechRepository.speak(text, utteranceId) }
    }

    @Test
    fun `execute should handle different utterance IDs`() {
        val text = "Same text"

        useCase.execute(text, "id-1")
        useCase.execute(text, "id-2")

        verify { textToSpeechRepository.speak(text, "id-1") }
        verify { textToSpeechRepository.speak(text, "id-2") }
    }

    @Test
    fun `execute should handle whitespace text`() {
        val text = "   "
        val utteranceId = "test-id"

        useCase.execute(text, utteranceId)

        verify { textToSpeechRepository.speak(text, utteranceId) }
    }
}
