package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.gson.annotations.SerializedName

data class AudioRelayResponse(
    @SerializedName("status") val status: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("recipients") val recipients: List<Int>,
    @SerializedName("audioBase64") val audioBase64: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("sampleRate") val sampleRate: Int,
    @SerializedName("format") val format: String
)
