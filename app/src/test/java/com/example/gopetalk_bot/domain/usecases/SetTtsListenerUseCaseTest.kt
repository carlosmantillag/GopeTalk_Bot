package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class SetTtsListenerUseCaseTest {

    private lateinit var textToSpeechRepository: TextToSpeechRepository
    private lateinit var useCase: SetTtsListenerUseCase

    @Before
    fun setup() {
        textToSpeechRepository = mockk(relaxed = true)
        useCase = SetTtsListenerUseCase(textToSpeechRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository setUtteranceProgressListener`() {
        val onStart: (String?) -> Unit = mockk(relaxed = true)
        val onDone: (String?) -> Unit = mockk(relaxed = true)
        val onError: (String?) -> Unit = mockk(relaxed = true)

        useCase.execute(onStart, onDone, onError)

        verify { textToSpeechRepository.setUtteranceProgressListener(onStart, onDone, onError) }
    }

    @Test
    fun `execute should invoke onStart callback`() {
        val onStart: (String?) -> Unit = mockk(relaxed = true)
        val onDone: (String?) -> Unit = mockk(relaxed = true)
        val onError: (String?) -> Unit = mockk(relaxed = true)

        every { 
            textToSpeechRepository.setUtteranceProgressListener(any(), any(), any()) 
        } answers {
            val callback = firstArg<(String?) -> Unit>()
            callback("test-id")
        }

        useCase.execute(onStart, onDone, onError)

        verify { onStart("test-id") }
    }

    @Test
    fun `execute should invoke onDone callback`() {
        val onStart: (String?) -> Unit = mockk(relaxed = true)
        val onDone: (String?) -> Unit = mockk(relaxed = true)
        val onError: (String?) -> Unit = mockk(relaxed = true)

        every { 
            textToSpeechRepository.setUtteranceProgressListener(any(), any(), any()) 
        } answers {
            val callback = secondArg<(String?) -> Unit>()
            callback("test-id")
        }

        useCase.execute(onStart, onDone, onError)

        verify { onDone("test-id") }
    }

    @Test
    fun `execute should invoke onError callback`() {
        val onStart: (String?) -> Unit = mockk(relaxed = true)
        val onDone: (String?) -> Unit = mockk(relaxed = true)
        val onError: (String?) -> Unit = mockk(relaxed = true)

        every { 
            textToSpeechRepository.setUtteranceProgressListener(any(), any(), any()) 
        } answers {
            val callback = thirdArg<(String?) -> Unit>()
            callback("test-id")
        }

        useCase.execute(onStart, onDone, onError)

        verify { onError("test-id") }
    }
}
