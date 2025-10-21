package com.example.gopetalk_bot.data.repositories

import android.speech.tts.UtteranceProgressListener
import com.example.gopetalk_bot.data.datasources.local.TextToSpeechDataSource
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class TextToSpeechRepositoryImplTest {

    private lateinit var ttsDataSource: TextToSpeechDataSource
    private lateinit var repository: TextToSpeechRepositoryImpl

    @Before
    fun setup() {
        ttsDataSource = mockk(relaxed = true)
        repository = TextToSpeechRepositoryImpl(ttsDataSource)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `speak should call data source speak method`() {
        repository.speak("Hello", "utterance-1")

        verify { ttsDataSource.speak("Hello", "utterance-1") }
    }

    @Test
    fun `shutdown should call data source shutdown`() {
        repository.shutdown()

        verify { ttsDataSource.shutdown() }
    }

    @Test
    fun `isInitialized should return data source initialization status`() {
        every { ttsDataSource.isInitialized() } returns true

        val result = repository.isInitialized()

        assertThat(result).isTrue()
        verify { ttsDataSource.isInitialized() }
    }

    @Test
    fun `isInitialized should return false when not initialized`() {
        every { ttsDataSource.isInitialized() } returns false

        val result = repository.isInitialized()

        assertThat(result).isFalse()
    }

    @Test
    fun `setUtteranceProgressListener should set listener on data source`() {
        var capturedListener: UtteranceProgressListener? = null
        
        every { ttsDataSource.setUtteranceProgressListener(any()) } answers {
            capturedListener = firstArg()
        }

        val onStart: (String?) -> Unit = mockk(relaxed = true)
        val onDone: (String?) -> Unit = mockk(relaxed = true)
        val onError: (String?) -> Unit = mockk(relaxed = true)

        repository.setUtteranceProgressListener(onStart, onDone, onError)

        verify { ttsDataSource.setUtteranceProgressListener(any()) }
        assertThat(capturedListener).isNotNull()
    }

    @Test
    fun `utterance listener onStart should trigger callback`() {
        var capturedListener: UtteranceProgressListener? = null
        var startCalled = false
        
        every { ttsDataSource.setUtteranceProgressListener(any()) } answers {
            capturedListener = firstArg()
        }

        repository.setUtteranceProgressListener(
            onStart = { startCalled = true },
            onDone = {},
            onError = {}
        )

        capturedListener?.onStart("test-utterance")

        assertThat(startCalled).isTrue()
    }

    @Test
    fun `utterance listener onDone should trigger callback`() {
        var capturedListener: UtteranceProgressListener? = null
        var doneCalled = false
        
        every { ttsDataSource.setUtteranceProgressListener(any()) } answers {
            capturedListener = firstArg()
        }

        repository.setUtteranceProgressListener(
            onStart = {},
            onDone = { doneCalled = true },
            onError = {}
        )

        capturedListener?.onDone("test-utterance")

        assertThat(doneCalled).isTrue()
    }

    @Test
    fun `utterance listener onError should trigger callback`() {
        var capturedListener: UtteranceProgressListener? = null
        var errorCalled = false
        
        every { ttsDataSource.setUtteranceProgressListener(any()) } answers {
            capturedListener = firstArg()
        }

        repository.setUtteranceProgressListener(
            onStart = {},
            onDone = {},
            onError = { errorCalled = true }
        )

        capturedListener?.onError("test-utterance")

        assertThat(errorCalled).isTrue()
    }

    @Test
    fun `speak should handle multiple consecutive calls`() {
        repository.speak("Text 1", "id-1")
        repository.speak("Text 2", "id-2")
        repository.speak("Text 3", "id-3")

        verify { ttsDataSource.speak("Text 1", "id-1") }
        verify { ttsDataSource.speak("Text 2", "id-2") }
        verify { ttsDataSource.speak("Text 3", "id-3") }
    }

    @Test
    fun `speak should handle empty text`() {
        repository.speak("", "utterance-1")

        verify { ttsDataSource.speak("", "utterance-1") }
    }

    @Test
    fun `speak should handle long text`() {
        val longText = "a".repeat(5000)
        repository.speak(longText, "utterance-1")

        verify { ttsDataSource.speak(longText, "utterance-1") }
    }

    @Test
    fun `shutdown should be idempotent`() {
        repository.shutdown()
        repository.shutdown()

        verify(exactly = 2) { ttsDataSource.shutdown() }
    }

    @Test
    fun `utterance listener should handle null utterance IDs`() {
        var capturedListener: UtteranceProgressListener? = null
        var startUtteranceId: String? = "not-null"
        
        every { ttsDataSource.setUtteranceProgressListener(any()) } answers {
            capturedListener = firstArg()
        }

        repository.setUtteranceProgressListener(
            onStart = { startUtteranceId = it },
            onDone = {},
            onError = {}
        )

        capturedListener?.onStart(null)

        assertThat(startUtteranceId).isNull()
    }

    @Test
    fun `utterance listener should pass correct utterance ID`() {
        var capturedListener: UtteranceProgressListener? = null
        var receivedId: String? = null
        
        every { ttsDataSource.setUtteranceProgressListener(any()) } answers {
            capturedListener = firstArg()
        }

        repository.setUtteranceProgressListener(
            onStart = { receivedId = it },
            onDone = {},
            onError = {}
        )

        capturedListener?.onStart("specific-id-123")

        assertThat(receivedId).isEqualTo("specific-id-123")
    }

    @Test
    fun `setUtteranceProgressListener should be callable multiple times`() {
        repository.setUtteranceProgressListener({}, {}, {})
        repository.setUtteranceProgressListener({}, {}, {})

        verify(exactly = 2) { ttsDataSource.setUtteranceProgressListener(any()) }
    }
}
