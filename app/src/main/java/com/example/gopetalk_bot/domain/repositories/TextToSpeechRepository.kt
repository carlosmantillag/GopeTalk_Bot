package com.example.gopetalk_bot.domain.repositories

/**
 * Repository interface for Text-to-Speech operations
 */
interface TextToSpeechRepository {
    /**
     * Initialize TTS engine
     * @param onInitError Callback for initialization errors
     */
    fun initialize(onInitError: (String) -> Unit)
    
    /**
     * Speak text
     * @param text The text to speak
     * @param utteranceId Unique identifier for this utterance
     */
    fun speak(text: String, utteranceId: String)
    
    /**
     * Set listener for utterance progress
     */
    fun setUtteranceProgressListener(
        onStart: (String?) -> Unit,
        onDone: (String?) -> Unit,
        onError: (String?) -> Unit
    )
    
    /**
     * Shutdown TTS engine
     */
    fun shutdown()
    
    /**
     * Check if TTS is initialized
     */
    fun isInitialized(): Boolean
}
