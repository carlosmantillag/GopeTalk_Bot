package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

class TextToSpeechDataSourceTest {

    private lateinit var dataSource: TextToSpeechDataSource
    private lateinit var mockContext: Context
    private lateinit var mockTtsEngine: TtsEngine
    private lateinit var mockTtsFactory: TtsEngineFactory
    private lateinit var onInitErrorCallback: (String) -> Unit
    private val initCallbackSlot = slot<(Int) -> Unit>()

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockTtsEngine = mockk(relaxed = true)
        mockTtsFactory = mockk(relaxed = true)
        onInitErrorCallback = mockk(relaxed = true)

        every { 
            mockTtsFactory.create(any(), capture(initCallbackSlot))
        } returns mockTtsEngine
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init should initialize TTS successfully and set language`() {
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        
        
        initCallbackSlot.captured.invoke(TextToSpeech.SUCCESS)

        assertThat(dataSource.isInitialized()).isTrue()
        verify { mockTtsEngine.setLanguage(Locale.forLanguageTag("es-MX")) }
        verify(exactly = 0) { onInitErrorCallback(any()) }
    }

    @Test
    fun `init should call onInitError when TTS initialization fails`() {
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        
        
        initCallbackSlot.captured.invoke(TextToSpeech.ERROR)

        assertThat(dataSource.isInitialized()).isFalse()
        verify { onInitErrorCallback("Failed to initialize TextToSpeech.") }
        verify(exactly = 0) { mockTtsEngine.setLanguage(any()) }
    }

    @Test
    fun `speak should call TTS engine when initialized`() {
        every { mockTtsEngine.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS
        
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        initCallbackSlot.captured.invoke(TextToSpeech.SUCCESS)
        
        dataSource.speak("Hello", "utterance-1")

        verify { 
            mockTtsEngine.speak(
                "Hello", 
                TextToSpeech.QUEUE_ADD, 
                any(), 
                "utterance-1"
            ) 
        }
    }

    @Test
    fun `speak should queue text when not initialized`() {
        every { mockTtsEngine.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS
        
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        
        
        dataSource.speak("Pending text", "utterance-1")
        
        
        verify(exactly = 0) { mockTtsEngine.speak(any(), any(), any(), any()) }
        
        
        initCallbackSlot.captured.invoke(TextToSpeech.SUCCESS)
        
        
        verify { mockTtsEngine.speak("Pending text", TextToSpeech.QUEUE_ADD, any(), "utterance-1") }
    }

    @Test
    fun `speak should queue multiple texts and speak all when initialized`() {
        every { mockTtsEngine.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS
        
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        
        
        dataSource.speak("Text 1", "utterance-1")
        dataSource.speak("Text 2", "utterance-2")
        dataSource.speak("Text 3", "utterance-3")
        
        
        initCallbackSlot.captured.invoke(TextToSpeech.SUCCESS)
        
        
        verify { mockTtsEngine.speak("Text 1", TextToSpeech.QUEUE_ADD, any(), "utterance-1") }
        verify { mockTtsEngine.speak("Text 2", TextToSpeech.QUEUE_ADD, any(), "utterance-2") }
        verify { mockTtsEngine.speak("Text 3", TextToSpeech.QUEUE_ADD, any(), "utterance-3") }
    }

    @Test
    fun `setUtteranceProgressListener should set listener on TTS engine`() {
        val mockListener = mockk<UtteranceProgressListener>(relaxed = true)
        
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        dataSource.setUtteranceProgressListener(mockListener)

        verify { mockTtsEngine.setOnUtteranceProgressListener(mockListener) }
    }

    @Test
    fun `shutdown should stop and shutdown TTS engine`() {
        every { mockTtsEngine.stop() } returns TextToSpeech.SUCCESS
        
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        dataSource.shutdown()

        verify { mockTtsEngine.stop() }
        verify { mockTtsEngine.shutdown() }
    }

    @Test
    fun `isInitialized should return false before initialization`() {
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        
        assertThat(dataSource.isInitialized()).isFalse()
    }

    @Test
    fun `isInitialized should return true after successful initialization`() {
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        initCallbackSlot.captured.invoke(TextToSpeech.SUCCESS)
        
        assertThat(dataSource.isInitialized()).isTrue()
    }

    @Test
    fun `speak should pass utteranceId to TTS engine`() {
        val utteranceIdSlot = slot<String>()
        every { mockTtsEngine.speak(any(), any(), any(), capture(utteranceIdSlot)) } returns TextToSpeech.SUCCESS
        
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        initCallbackSlot.captured.invoke(TextToSpeech.SUCCESS)
        
        dataSource.speak("Test", "my-utterance-id")

        assertThat(utteranceIdSlot.captured).isEqualTo("my-utterance-id")
    }

    @Test
    fun `pending texts should be cleared after initialization`() {
        every { mockTtsEngine.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS
        
        dataSource = TextToSpeechDataSource(mockContext, onInitErrorCallback, mockTtsFactory)
        
        
        dataSource.speak("Pending", "utterance-1")
        
        
        initCallbackSlot.captured.invoke(TextToSpeech.SUCCESS)
        
        
        dataSource.speak("New text", "utterance-2")
        
        
        verify(exactly = 2) { mockTtsEngine.speak(any(), any(), any(), any()) }
    }
}
