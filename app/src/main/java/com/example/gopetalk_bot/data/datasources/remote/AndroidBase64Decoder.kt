package com.example.gopetalk_bot.data.datasources.remote

import android.util.Base64

class AndroidBase64Decoder : Base64Decoder {
    override fun decode(str: String, flags: Int): ByteArray {
        return Base64.decode(str, flags)
    }
}
