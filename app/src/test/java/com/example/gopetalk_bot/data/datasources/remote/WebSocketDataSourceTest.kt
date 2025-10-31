package com.example.gopetalk_bot.data.datasources.remote

import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import okhttp3.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test

class WebSocketDataSourceTest {

    private lateinit var dataSource: WebSocketDataSource
    private lateinit var listener: WebSocketDataSource.MicrophoneControlListener
    private lateinit var mockWebSocket: WebSocket
    private lateinit var mockOkHttpClient: OkHttpClient
    private val webSocketListenerSlot = slot<WebSocketListener>()
    
    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        mockWebSocket = mockk(relaxed = true)
        mockOkHttpClient = mockk(relaxed = true)
        listener = mockk(relaxed = true)

        every { 
            mockOkHttpClient.newWebSocket(any<Request>(), capture(webSocketListenerSlot))
        } returns mockWebSocket

        
        dataSource = WebSocketDataSource(mockOkHttpClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `connect should create WebSocket with correct URL`() {
        val url = "ws://test.com"
        
        dataSource.connect(url, "token", "channel", listener)

        verify { mockOkHttpClient.newWebSocket(any(), any()) }
    }

    @Test
    fun `isConnected should return false initially`() {
        val result = dataSource.isConnected()

        assertThat(result).isFalse()
    }

    @Test
    fun `isConnected should return true after connection`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)

        val result = dataSource.isConnected()

        assertThat(result).isTrue()
    }

    @Test
    fun `isConnected should return false after disconnect`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        dataSource.disconnect()

        val result = dataSource.isConnected()

        assertThat(result).isFalse()
    }

    @Test
    fun `onOpen should send handshake and notify listener`() {
        dataSource.connect("ws://test.com", "token123", "channel456", listener)
        
        val mockResponse = mockk<Response>(relaxed = true)
        webSocketListenerSlot.captured.onOpen(mockWebSocket, mockResponse)

        verify { mockWebSocket.send(match<String> { 
            val json = JSONObject(it)
            json.optString("authToken") == "token123" && 
            json.optString("channel") == "channel456"
        }) }
        verify { listener.onConnectionEstablished() }
    }

    @Test
    fun `onOpen should send handshake without authToken if null`() {
        dataSource.connect("ws://test.com", null, "channel456", listener)
        
        val mockResponse = mockk<Response>(relaxed = true)
        webSocketListenerSlot.captured.onOpen(mockWebSocket, mockResponse)

        verify { mockWebSocket.send(match<String> { 
            val json = JSONObject(it)
            !json.has("authToken") && json.optString("channel") == "channel456"
        }) }
    }

    @Test
    fun `onOpen should send handshake without channel if null`() {
        dataSource.connect("ws://test.com", "token123", null, listener)
        
        val mockResponse = mockk<Response>(relaxed = true)
        webSocketListenerSlot.captured.onOpen(mockWebSocket, mockResponse)

        verify { mockWebSocket.send(match<String> { 
            val json = JSONObject(it)
            json.optString("authToken") == "token123" && !json.has("channel")
        }) }
    }

    @Test
    fun `onMessage with JSON START action should trigger onMicrophoneStart`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        val message = """{"action":"START"}"""
        webSocketListenerSlot.captured.onMessage(mockWebSocket, message)

        verify { listener.onMicrophoneStart() }
    }

    @Test
    fun `onMessage with JSON STOP action should trigger onMicrophoneStop`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        val message = """{"action":"STOP"}"""
        webSocketListenerSlot.captured.onMessage(mockWebSocket, message)

        verify { listener.onMicrophoneStop() }
    }

    @Test
    fun `onMessage with JSON type START should trigger onMicrophoneStart`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        val message = """{"type":"START"}"""
        webSocketListenerSlot.captured.onMessage(mockWebSocket, message)

        verify { listener.onMicrophoneStart() }
    }

    @Test
    fun `onMessage with JSON type STOP should trigger onMicrophoneStop`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        val message = """{"type":"STOP"}"""
        webSocketListenerSlot.captured.onMessage(mockWebSocket, message)

        verify { listener.onMicrophoneStop() }
    }

    @Test
    fun `onMessage with plain text START should trigger onMicrophoneStart`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        webSocketListenerSlot.captured.onMessage(mockWebSocket, "START")

        verify { listener.onMicrophoneStart() }
    }

    @Test
    fun `onMessage with plain text STOP should trigger onMicrophoneStop`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        webSocketListenerSlot.captured.onMessage(mockWebSocket, "STOP")

        verify { listener.onMicrophoneStop() }
    }

    @Test
    fun `onMessage with lowercase start should trigger onMicrophoneStart`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        webSocketListenerSlot.captured.onMessage(mockWebSocket, "start")

        verify { listener.onMicrophoneStart() }
    }

    @Test
    fun `onMessage with unknown command should log warning`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        webSocketListenerSlot.captured.onMessage(mockWebSocket, "UNKNOWN")

        verify { Log.w(any<String>(), match<String> { it.contains("Unknown command") }) }
        verify(exactly = 0) { listener.onMicrophoneStart() }
        verify(exactly = 0) { listener.onMicrophoneStop() }
    }

    @Test
    fun `onMessage with invalid JSON should fallback to plain text parsing`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        webSocketListenerSlot.captured.onMessage(mockWebSocket, "{invalid json START")

        verify { Log.e(any(), any(), any()) }
    }

    @Test
    fun `onClosing should close WebSocket`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        webSocketListenerSlot.captured.onClosing(mockWebSocket, 1000, "Normal closure")

        verify { mockWebSocket.close(1000, null) }
    }

    @Test
    fun `onClosed should notify listener`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        webSocketListenerSlot.captured.onClosed(mockWebSocket, 1000, "Normal closure")

        verify { listener.onConnectionClosed() }
    }

    @Test
    fun `onFailure should log error and notify listener`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        val exception = Exception("Connection failed")
        webSocketListenerSlot.captured.onFailure(mockWebSocket, exception, null)

        verify { Log.e(any(), any(), exception) }
        verify { listener.onError("Connection failed") }
    }

    @Test
    fun `onFailure with null message should use default error message`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        val exception = Exception()
        webSocketListenerSlot.captured.onFailure(mockWebSocket, exception, null)

        verify { listener.onError("Unknown WebSocket error") }
    }

    @Test
    fun `disconnect should close WebSocket with normal code`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        dataSource.disconnect()

        verify { mockWebSocket.close(1000, "Client disconnecting") }
    }

    @Test
    fun `disconnect should set webSocket to null`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        dataSource.disconnect()

        val result = dataSource.isConnected()

        assertThat(result).isFalse()
    }

    @Test
    fun `updateChannel should send update when connected`() {
        dataSource.connect("ws://test.com", "token123", "oldChannel", listener)
        
        
        val mockResponse = mockk<Response>(relaxed = true)
        webSocketListenerSlot.captured.onOpen(mockWebSocket, mockResponse)
        
        dataSource.updateChannel("newToken", "newChannel")

        
        verify(atLeast = 2) { mockWebSocket.send(any<String>()) }
    }

    @Test
    fun `updateChannel should log warning when not connected`() {
        dataSource.updateChannel("token", "channel")

        verify { Log.w(any<String>(), match<String> { it.contains("Cannot update channel") }) }
    }

    @Test
    fun `updateChannel should handle null authToken`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        dataSource.updateChannel(null, "newChannel")

        verify { mockWebSocket.send(match<String> { 
            val json = JSONObject(it)
            !json.has("authToken") && json.optString("channel") == "newChannel"
        }) }
    }

    @Test
    fun `updateChannel should handle null channel`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        dataSource.updateChannel("newToken", null)

        verify { mockWebSocket.send(match<String> { 
            val json = JSONObject(it)
            json.optString("authToken") == "newToken" && !json.has("channel")
        }) }
    }

    @Test
    fun `updateChannel should handle blank authToken`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        dataSource.updateChannel("  ", "newChannel")

        verify { mockWebSocket.send(match<String> { 
            val json = JSONObject(it)
            !json.has("authToken") && json.optString("channel") == "newChannel"
        }) }
    }

    @Test
    fun `updateChannel should handle blank channel`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        dataSource.updateChannel("newToken", "  ")

        verify { mockWebSocket.send(match<String> { 
            val json = JSONObject(it)
            json.optString("authToken") == "newToken" && !json.has("channel")
        }) }
    }

    @Test
    fun `onMessage with whitespace around plain text should work`() {
        dataSource.connect("ws://test.com", "token", "channel", listener)
        
        webSocketListenerSlot.captured.onMessage(mockWebSocket, "  START  ")

        verify { listener.onMicrophoneStart() }
    }

    @Test
    fun `multiple connections should replace previous WebSocket`() {
        dataSource.connect("ws://test1.com", "token1", "channel1", listener)
        val firstWebSocket = mockWebSocket
        
        dataSource.connect("ws://test2.com", "token2", "channel2", listener)

        verify(exactly = 2) { mockOkHttpClient.newWebSocket(any(), any()) }
    }
}
