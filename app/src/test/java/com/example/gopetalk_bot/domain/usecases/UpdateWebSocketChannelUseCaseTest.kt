package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class UpdateWebSocketChannelUseCaseTest {

    private lateinit var webSocketRepository: WebSocketRepository
    private lateinit var useCase: UpdateWebSocketChannelUseCase

    @Before
    fun setup() {
        webSocketRepository = mockk(relaxed = true)
        useCase = UpdateWebSocketChannelUseCase(webSocketRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository updateChannel with correct parameters`() {
        val authToken = "test-token"
        val channel = "test-channel"

        useCase.execute(authToken, channel)

        verify { webSocketRepository.updateChannel(authToken, channel) }
    }

    @Test
    fun `execute should handle null authToken`() {
        val channel = "test-channel"

        useCase.execute(null, channel)

        verify { webSocketRepository.updateChannel(null, channel) }
    }

    @Test
    fun `execute should handle null channel`() {
        val authToken = "test-token"

        useCase.execute(authToken, null)

        verify { webSocketRepository.updateChannel(authToken, null) }
    }

    @Test
    fun `execute should handle both null parameters`() {
        useCase.execute(null, null)

        verify { webSocketRepository.updateChannel(null, null) }
    }

    @Test
    fun `execute should handle multiple channel updates`() {
        useCase.execute("token1", "channel1")
        useCase.execute("token2", "channel2")
        useCase.execute("token3", "channel3")

        verify { webSocketRepository.updateChannel("token1", "channel1") }
        verify { webSocketRepository.updateChannel("token2", "channel2") }
        verify { webSocketRepository.updateChannel("token3", "channel3") }
    }
}
