package com.example.gopetalk_bot.network

data class BackendResponse(
    val text: String = "",
    val action: String = "",
    val channels: List<String> = emptyList(),
    val users: List<String> = emptyList()
)
