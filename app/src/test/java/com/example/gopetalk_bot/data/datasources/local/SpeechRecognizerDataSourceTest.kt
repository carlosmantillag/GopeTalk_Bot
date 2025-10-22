package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class SpeechRecognizerDataSourceTest {

    private lateinit var dataSource: SpeechRecognizerDataSource
    private lateinit var mockContext: Context
    private lateinit var mockRecognizer: SpeechRecognizerWrapper
    private lateinit var mockFactory: SpeechRecognizerFactory
    private lateinit var onResultCallback: (String) -> Unit
    private lateinit var onErrorCallback: (String) -> Unit
    private val listenerSlot = slot<RecognitionListener>()

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockRecognizer = mockk(relaxed = true)
        mockFactory = mockk(relaxed = true)
        onResultCallback = mockk(relaxed = true)
        onErrorCallback = mockk(relaxed = true)

        val mockIntent = mockk<Intent>(relaxed = true)
        every { mockFactory.isRecognitionAvailable(any()) } returns true
        every { mockFactory.createRecognizer(any()) } returns mockRecognizer
        every { mockFactory.createRecognitionIntent() } returns mockIntent
        every { mockRecognizer.setRecognitionListener(capture(listenerSlot)) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startListening should create recognizer and start listening when available`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        
        dataSource.startListening(onResultCallback, onErrorCallback)

        verify { mockFactory.isRecognitionAvailable(mockContext) }
        verify { mockFactory.createRecognizer(mockContext) }
        verify { mockRecognizer.setRecognitionListener(any()) }
        verify { mockRecognizer.startListening(any()) }
        verify(exactly = 0) { onErrorCallback(any()) }
    }

    @Test
    fun `startListening should call onError when recognition not available`() {
        every { mockFactory.isRecognitionAvailable(any()) } returns false
        
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)

        verify { onErrorCallback("Speech recognition not available") }
        verify(exactly = 0) { mockFactory.createRecognizer(any()) }
    }

    @Test
    fun `startListening should not start if already listening`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        
        // Primera llamada
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        // Simular que está listo
        listenerSlot.captured.onReadyForSpeech(null)
        
        // Segunda llamada mientras está escuchando
        dataSource.startListening(onResultCallback, onErrorCallback)

        // Solo debe crear el recognizer una vez
        verify(exactly = 1) { mockFactory.createRecognizer(mockContext) }
    }

    @Test
    fun `onReadyForSpeech should set isListening to true`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onReadyForSpeech(null)
        
        // Intentar iniciar de nuevo debería ser ignorado
        dataSource.startListening(onResultCallback, onErrorCallback)
        verify(exactly = 1) { mockFactory.createRecognizer(mockContext) }
    }

    @Test
    fun `onResults should call onResult with recognized text`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        val results = mockk<Bundle>(relaxed = true)
        every { results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) } returns 
            arrayListOf("Hello World", "Alternative")
        
        listenerSlot.captured.onResults(results)

        verify { onResultCallback("Hello World") }
        verify(exactly = 0) { onErrorCallback(any()) }
    }

    @Test
    fun `onResults should call onError when no matches`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        val results = mockk<Bundle>(relaxed = true)
        every { results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) } returns arrayListOf()
        
        listenerSlot.captured.onResults(results)

        verify { onErrorCallback("No results") }
        verify(exactly = 0) { onResultCallback(any()) }
    }

    @Test
    fun `onResults should call onError when results is null`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onResults(null)

        verify { onErrorCallback("No results") }
    }

    @Test
    fun `onError should call onError callback with error message for ERROR_AUDIO`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onError(SpeechRecognizer.ERROR_AUDIO)

        verify { onErrorCallback("Audio recording error") }
    }

    @Test
    fun `onError should call onError callback with error message for ERROR_NETWORK`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onError(SpeechRecognizer.ERROR_NETWORK)

        verify { onErrorCallback("Network error") }
    }

    @Test
    fun `onError should call onError callback with error message for ERROR_NO_MATCH`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onError(SpeechRecognizer.ERROR_NO_MATCH)

        verify { onErrorCallback("No match found") }
    }

    @Test
    fun `onError should call onError callback with unknown error for unrecognized code`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onError(999)

        verify { onErrorCallback("Unknown error") }
    }

    @Test
    fun `onEndOfSpeech should set isListening to false`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onReadyForSpeech(null)
        listenerSlot.captured.onEndOfSpeech()
        
        // Ahora debería poder iniciar de nuevo
        dataSource.startListening(onResultCallback, onErrorCallback)
        verify(exactly = 2) { mockFactory.createRecognizer(mockContext) }
    }

    @Test
    fun `stopListening should call stopListening on recognizer when listening`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onReadyForSpeech(null)
        dataSource.stopListening()

        verify { mockRecognizer.stopListening() }
    }

    @Test
    fun `stopListening should not call stopListening when not listening`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        
        dataSource.stopListening()

        verify(exactly = 0) { mockRecognizer.stopListening() }
    }

    @Test
    fun `release should destroy recognizer`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        dataSource.release()

        verify { mockRecognizer.destroy() }
    }

    @Test
    fun `release should set isListening to false`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        listenerSlot.captured.onReadyForSpeech(null)
        dataSource.release()
        
        // Debería poder iniciar de nuevo después de release
        dataSource.startListening(onResultCallback, onErrorCallback)
        verify(exactly = 2) { mockFactory.createRecognizer(mockContext) }
    }

    @Test
    fun `all error codes should have proper messages`() {
        dataSource = SpeechRecognizerDataSource(mockContext, mockFactory)
        dataSource.startListening(onResultCallback, onErrorCallback)
        
        val errorCodes = mapOf(
            SpeechRecognizer.ERROR_AUDIO to "Audio recording error",
            SpeechRecognizer.ERROR_CLIENT to "Client side error",
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to "Insufficient permissions",
            SpeechRecognizer.ERROR_NETWORK to "Network error",
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT to "Network timeout",
            SpeechRecognizer.ERROR_NO_MATCH to "No match found",
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY to "Recognition service busy",
            SpeechRecognizer.ERROR_SERVER to "Server error",
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT to "No speech input"
        )
        
        errorCodes.forEach { (code, expectedMessage) ->
            clearMocks(onErrorCallback, answers = false)
            listenerSlot.captured.onError(code)
            verify { onErrorCallback(expectedMessage) }
        }
    }
}
