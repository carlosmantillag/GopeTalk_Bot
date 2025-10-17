package com.example.gopetalk_bot.domain.entities

import java.io.File

data class AudioData(
    val file: File,
    val sampleRate: Int,
    val channels: Int,
    val format: AudioFormat
)


