package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.google.common.truth.Truth.assertThat
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class SpeechRecognizerDataSourceTest {

    private lateinit var context: Context
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var dataSource: SpeechRecognizerDataSource

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        speechRecognizer = mockk(relaxed = true)
        dataSource = SpeechRecognizerDataSource(context)

        mockkStatic(SpeechRecognizer::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(SpeechRecognizer::class)
    }

    @Test
    fun `startListening should return error when recognition not available`() {
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns false

        var errorMessage: String? = null
        dataSource.startListening(onResult = {}, onError = { errorMessage = it })

        assertThat(errorMessage).isEqualTo("Speech recognition not available")
        verify { SpeechRecognizer.createSpeechRecognizer(any()) wasNot Called }
    }

    @Test
    fun `startListening should setup recognizer and start listening`() {
        val listenerSlot = slot<RecognitionListener>()
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns true
        every { SpeechRecognizer.createSpeechRecognizer(context) } returns speechRecognizer
        every { speechRecognizer.setRecognitionListener(capture(listenerSlot)) } returns Unit

        dataSource.startListening(onResult = {}, onError = {})

        verify { speechRecognizer.startListening(match { intent: Intent ->
            intent.action == RecognizerIntent.ACTION_RECOGNIZE_SPEECH &&
                intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL) == RecognizerIntent.LANGUAGE_MODEL_FREE_FORM &&
                intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE) != null
        }) }
        assertThat(listenerSlot.captured).isNotNull()
    }

    @Test
    fun `recognizer should forward success results`() {
        val listenerSlot = slot<RecognitionListener>()
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns true
        every { SpeechRecognizer.createSpeechRecognizer(context) } returns speechRecognizer
        every { speechRecognizer.setRecognitionListener(capture(listenerSlot)) } returns Unit

        var receivedResult: String? = null
        dataSource.startListening(onResult = { receivedResult = it }, onError = {})

        val resultsBundle = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("hola", "mundo"))
        }
        listenerSlot.captured.onResults(resultsBundle)

        assertThat(receivedResult).isEqualTo("hola")
    }

    @Test
    fun `recognizer should forward error messages`() {
        val listenerSlot = slot<RecognitionListener>()
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns true
        every { SpeechRecognizer.createSpeechRecognizer(context) } returns speechRecognizer
        every { speechRecognizer.setRecognitionListener(capture(listenerSlot)) } returns Unit

        var receivedError: String? = null
        dataSource.startListening(onResult = {}, onError = { receivedError = it })

        listenerSlot.captured.onError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT)

        assertThat(receivedError).isEqualTo("Network timeout")
    }

    @Test
    fun `stopListening should stop recognizer when listening`() {
        val listenerSlot = slot<RecognitionListener>()
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns true
        every { SpeechRecognizer.createSpeechRecognizer(context) } returns speechRecognizer
        every { speechRecognizer.setRecognitionListener(capture(listenerSlot)) } returns Unit

        dataSource.startListening(onResult = {}, onError = {})
        listenerSlot.captured.onReadyForSpeech(null)

        dataSource.stopListening()

        verify { speechRecognizer.stopListening() }
    }

    @Test
    fun `release should destroy recognizer`() {
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns true
        every { SpeechRecognizer.createSpeechRecognizer(context) } returns speechRecognizer
        every { speechRecognizer.setRecognitionListener(any()) } returns Unit

        dataSource.startListening(onResult = {}, onError = {})
        dataSource.release()

        verify { speechRecognizer.destroy() }
    }

    @Test
    fun `startListening should not recreate recognizer when already listening`() {
        val listenerSlot = slot<RecognitionListener>()
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns true
        every { SpeechRecognizer.createSpeechRecognizer(context) } returns speechRecognizer
        every { speechRecognizer.setRecognitionListener(capture(listenerSlot)) } returns Unit

        dataSource.startListening(onResult = {}, onError = {})
        listenerSlot.captured.onReadyForSpeech(null)

        dataSource.startListening(onResult = {}, onError = {})

        verify(exactly = 1) { SpeechRecognizer.createSpeechRecognizer(context) }
    }
}
