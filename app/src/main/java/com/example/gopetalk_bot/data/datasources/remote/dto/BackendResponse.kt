package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.gson.annotations.SerializedName

data class BackendResponse(
    @SerializedName("status") val status: String = "",
    @SerializedName("intent") val intent: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: BackendResponseData? = null,
    @SerializedName("text") val text: String = "",
    @SerializedName("action") val action: String = "",
    @SerializedName("channels") val channels: List<String> = emptyList(),
    @SerializedName("users") val users: List<String> = emptyList(),
    @SerializedName("audio_file") val audioFile: String? = null,
    @SerializedName("is_audio_response") val isAudioResponse: Boolean = false,
    @SerializedName("channel") val channel: String? = null
)

data class BackendResponseData(
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("channel_label") val channelLabel: String? = null
)
