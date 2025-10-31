package com.example.gopetalk_bot.data.datasources.local

import android.content.Context

interface TtsEngineFactory {
    fun create(context: Context, onInit: (Int) -> Unit): TtsEngine
}
