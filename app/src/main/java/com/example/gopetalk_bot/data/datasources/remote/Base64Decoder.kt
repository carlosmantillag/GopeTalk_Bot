package com.example.gopetalk_bot.data.datasources.remote

interface Base64Decoder {
    fun decode(str: String, flags: Int): ByteArray
}
