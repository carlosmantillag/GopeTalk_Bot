package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.WebSocketRepository

class DisconnectWebSocketUseCase(
    private val webSocketRepository: WebSocketRepository
) {
    fun execute() {
        webSocketRepository.disconnect()
    }
}
