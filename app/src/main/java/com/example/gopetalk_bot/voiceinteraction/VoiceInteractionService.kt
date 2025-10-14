package com.example.gopetalk_bot.voiceinteraction

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gopetalk_bot.R
import java.io.File

class VoiceInteractionService : Service(), VoiceInteractionContract.View {

    private lateinit var presenter: VoiceInteractionContract.Presenter
    private lateinit var textToSpeechManager: TextToSpeechManager

    override val context: Context
        get() = this

    companion object {
        private const val TAG = "VoiceInteractionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceInteractionChannel"
    }

    override fun onCreate() {
        super.onCreate()
        presenter = VoiceInteractionPresenter(this)
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        textToSpeechManager = TextToSpeechManager(this) { error -> logError(error, null) }
        presenter.start(textToSpeechManager)
        presenter.onHotwordDetected()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun speak(text: String, utteranceId: String) {
        textToSpeechManager.speak(text, utteranceId)
    }

    override fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    override fun logError(message: String, t: Throwable?) {
        if (t != null) {
            Log.e(TAG, message, t)
        } else {
            Log.e(TAG, message)
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Voice Interaction Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("GopeTalk Bot Activo")
        .setContentText("Escuchando...")
        .setSmallIcon(R.mipmap.ic_launcher)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        presenter.stop()
        textToSpeechManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
