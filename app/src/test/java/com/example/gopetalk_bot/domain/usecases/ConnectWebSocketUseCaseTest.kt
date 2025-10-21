package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class ConnectWebSocketUseCaseTest {

    private lateinit var webSocketRepository: WebSocketRepository
    private lateinit var useCase: ConnectWebSocketUseCase
    private lateinit var mockListener: WebSocketRepository.MicrophoneControlListener

    @Before
    fun setup() {
        webSocketRepository = mockk(relaxed = true)
        useCase = ConnectWebSocketUseCase(webSocketRepository)
        mockListener = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository connect with correct parameters`() {
        val url = "ws://test.com"
        val authToken = "test-token"
        val channel = "test-channel"

        useCase.execute(url, authToken, channel, mockListener)

        verify { webSocketRepository.connect(url, authToken, channel, mockListener) }
    }

    @Test
    fun `execute should handle null authToken`() {
        val url = "ws://test.com"
        val channel = "test-channel"

        useCase.execute(url, null, channel, mockListener)

        verify { webSocketRepository.connect(url, null, channel, mockListener) }
    }

    @Test
    fun `execute should handle null channel`() {
        val url = "ws://test.com"
        val authToken = "test-token"

        useCase.execute(url, authToken, null, mockListener)

        verify { webSocketRepository.connect(url, authToken, null, mockListener) }
    }

    @Test
    fun `execute should invoke listener callbacks`() {
        val url = "ws://test.com"
        val authToken = "test-token"
        val channel = "test-channel"

        every { 
            webSocketRepository.connect(any(), any(), any(), any()) 
        } answers {
            val listener = lastArg<WebSocketRepository.MicrophoneControlListener>()
            listener.onMicrophoneStart()
        }

        useCase.execute(url, authToken, channel, mockListener)

        verify { mockListener.onMicrophoneStart() }
    }
}
