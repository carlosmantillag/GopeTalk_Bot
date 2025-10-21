package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class DisconnectWebSocketUseCaseTest {

    private lateinit var webSocketRepository: WebSocketRepository
    private lateinit var useCase: DisconnectWebSocketUseCase

    @Before
    fun setup() {
        webSocketRepository = mockk(relaxed = true)
        useCase = DisconnectWebSocketUseCase(webSocketRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository disconnect`() {
        useCase.execute()

        verify { webSocketRepository.disconnect() }
    }

    @Test
    fun `execute should handle multiple disconnect calls`() {
        useCase.execute()
        useCase.execute()
        useCase.execute()

        verify(exactly = 3) { webSocketRepository.disconnect() }
    }
}
