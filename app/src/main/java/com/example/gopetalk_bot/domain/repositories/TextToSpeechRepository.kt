package com.example.gopetalk_bot.domain.repositories

interface TextToSpeechRepository {
    fun initialize(onInitError: (String) -> Unit)
    fun speak(text: String, utteranceId: String)
    fun setUtteranceProgressListener(
        onStart: (String?) -> Unit,
        onDone: (String?) -> Unit,
        onError: (String?) -> Unit
    )
    fun shutdown()
    fun isInitialized(): Boolean
}
