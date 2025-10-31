package com.example.gopetalk_bot.data.repositories

import android.speech.tts.UtteranceProgressListener
import com.example.gopetalk_bot.data.datasources.local.TextToSpeechDataSource
import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository

class TextToSpeechRepositoryImpl(
    private val ttsDataSource: TextToSpeechDataSource
) : TextToSpeechRepository {

    override fun initialize(onInitError: (String) -> Unit) {
        
    }

    override fun speak(text: String, utteranceId: String) {
        ttsDataSource.speak(text, utteranceId)
    }

    override fun setUtteranceProgressListener(
        onStart: (String?) -> Unit,
        onDone: (String?) -> Unit,
        onError: (String?) -> Unit
    ) {
        ttsDataSource.setUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStart(utteranceId)
            }

            override fun onDone(utteranceId: String?) {
                onDone(utteranceId)
            }

            override fun onError(utteranceId: String?) {
                onError(utteranceId)
            }
        })
    }

    override fun shutdown() {
        ttsDataSource.shutdown()
    }

    override fun isInitialized(): Boolean {
        return ttsDataSource.isInitialized()
    }
}
