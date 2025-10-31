package com.example.gopetalk_bot.data.repositories

import android.util.Log
import com.example.gopetalk_bot.data.datasources.remote.WebSocketDataSource
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class WebSocketRepositoryImplTest {

    private lateinit var webSocketDataSource: WebSocketDataSource
    private lateinit var repository: WebSocketRepositoryImpl

    @Before
    fun setup() {
        
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        
        webSocketDataSource = mockk(relaxed = true)
        repository = WebSocketRepositoryImpl(webSocketDataSource)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        clearAllMocks()
    }

    @Test
    fun `connect should call data source with correct parameters`() {
        val url = "ws://test.com"
        val authToken = "test-token"
        val channel = "general"
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)

        repository.connect(url, authToken, channel, listener)

        verify { 
            webSocketDataSource.connect(
                url = url,
                authToken = authToken,
                channel = channel,
                listener = any()
            )
        }
    }

    @Test
    fun `connect should propagate microphone start event`() {
        var capturedListener: WebSocketDataSource.MicrophoneControlListener? = null
        var micStartCalled = false
        
        every { webSocketDataSource.connect(any(), any(), any(), any()) } answers {
            capturedListener = arg(3)
        }

        val listener = object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() { micStartCalled = true }
            override fun onMicrophoneStop() {}
            override fun onConnectionEstablished() {}
            override fun onConnectionClosed() {}
            override fun onError(error: String) {}
        }

        repository.connect("ws://test.com", "token", null, listener)
        capturedListener?.onMicrophoneStart()

        assertThat(micStartCalled).isTrue()
    }

    @Test
    fun `connect should propagate microphone stop event`() {
        var capturedListener: WebSocketDataSource.MicrophoneControlListener? = null
        var micStopCalled = false
        
        every { webSocketDataSource.connect(any(), any(), any(), any()) } answers {
            capturedListener = arg(3)
        }

        val listener = object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() {}
            override fun onMicrophoneStop() { micStopCalled = true }
            override fun onConnectionEstablished() {}
            override fun onConnectionClosed() {}
            override fun onError(error: String) {}
        }

        repository.connect("ws://test.com", "token", null, listener)
        capturedListener?.onMicrophoneStop()

        assertThat(micStopCalled).isTrue()
    }

    @Test
    fun `connect should propagate connection established event`() {
        var capturedListener: WebSocketDataSource.MicrophoneControlListener? = null
        var connectionEstablishedCalled = false
        
        every { webSocketDataSource.connect(any(), any(), any(), any()) } answers {
            capturedListener = arg(3)
        }

        val listener = object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() {}
            override fun onMicrophoneStop() {}
            override fun onConnectionEstablished() { connectionEstablishedCalled = true }
            override fun onConnectionClosed() {}
            override fun onError(error: String) {}
        }

        repository.connect("ws://test.com", "token", null, listener)
        capturedListener?.onConnectionEstablished()

        assertThat(connectionEstablishedCalled).isTrue()
    }

    @Test
    fun `connect should propagate error event`() {
        var capturedListener: WebSocketDataSource.MicrophoneControlListener? = null
        var errorMessage: String? = null
        
        every { webSocketDataSource.connect(any(), any(), any(), any()) } answers {
            capturedListener = arg(3)
        }

        val listener = object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() {}
            override fun onMicrophoneStop() {}
            override fun onConnectionEstablished() {}
            override fun onConnectionClosed() {}
            override fun onError(error: String) { errorMessage = error }
        }

        repository.connect("ws://test.com", "token", null, listener)
        capturedListener?.onError("Connection failed")

        assertThat(errorMessage).isEqualTo("Connection failed")
    }

    @Test
    fun `disconnect should call data source disconnect`() {
        repository.disconnect()

        verify { webSocketDataSource.disconnect() }
    }

    @Test
    fun `updateChannel should call data source with correct parameters`() {
        repository.updateChannel("test-token", "general")

        verify { 
            webSocketDataSource.updateChannel(
                authToken = "test-token",
                channel = "general"
            )
        }
    }

    @Test
    fun `updateChannel should handle null channel`() {
        repository.updateChannel("test-token", null)

        verify { 
            webSocketDataSource.updateChannel(
                authToken = "test-token",
                channel = null
            )
        }
    }

    @Test
    fun `isConnected should return true when data source is connected`() {
        every { webSocketDataSource.isConnected() } returns true

        val result = repository.isConnected()

        assertThat(result).isTrue()
        verify { webSocketDataSource.isConnected() }
    }

    @Test
    fun `isConnected should return false when data source is not connected`() {
        every { webSocketDataSource.isConnected() } returns false

        val result = repository.isConnected()

        assertThat(result).isFalse()
        verify { webSocketDataSource.isConnected() }
    }
}
