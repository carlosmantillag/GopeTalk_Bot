package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.gson.annotations.SerializedName

data class BackendResponse(
    @SerializedName("text") val text: String = "",
    @SerializedName("action") val action: String = "",
    @SerializedName("channels") val channels: List<String> = emptyList(),
    @SerializedName("users") val users: List<String> = emptyList()
)
