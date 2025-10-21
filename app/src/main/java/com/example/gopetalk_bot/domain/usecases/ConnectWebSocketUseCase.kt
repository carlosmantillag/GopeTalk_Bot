package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.WebSocketRepository

class ConnectWebSocketUseCase(
    private val webSocketRepository: WebSocketRepository
) {
    fun execute(url: String, authToken: String?, channel: String?, listener: WebSocketRepository.MicrophoneControlListener) {
        webSocketRepository.connect(url, authToken, channel, listener)
    }
}
