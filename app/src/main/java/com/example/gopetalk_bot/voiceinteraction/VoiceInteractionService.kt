package com.example.gopetalk_bot.voiceinteraction

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gopetalk_bot.R
import java.io.File

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VoiceInteractionService : Service(), VoiceInteractionContract.View, SpeechRecognizerManager.SpeechRecognitionListener {

    private lateinit var presenter: VoiceInteractionContract.Presenter
    private lateinit var speechRecognizerManager: SpeechRecognizerManager
    private lateinit var audioRecordingManager: AudioRecordingManager
    private lateinit var textToSpeechManager: TextToSpeechManager


    companion object {
        private const val TAG = "VoiceInteractionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceInteractionChannel"
        private const val HOTWORD = "gopebot"
    }

    override fun onCreate() {
        super.onCreate()
        presenter = VoiceInteractionPresenter(this)
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        speechRecognizerManager = SpeechRecognizerManager(this, this)
        speechRecognizerManager.create()

        audioRecordingManager = AudioRecordingManager(this, ::logInfo, ::logError)
        textToSpeechManager = TextToSpeechManager(this) { error -> logError(error, null) }

        presenter.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startListeningForHotword()
        return START_STICKY
    }

    override fun startListeningForHotword() {
        if (audioRecordingManager.isRecording()) return // Don't interrupt command recording
        speechRecognizerManager.startListening()
    }

    // Implementation of SpeechRecognizerManager.SpeechRecognitionListener
    override fun onHotwordDetected() {
        presenter.onHotwordDetected()
    }

    override fun onSpeechEnded() {
        presenter.onSpeechEnded()
    }

    override fun onRmsChanged(rmsdB: Float) {
        GlobalScope.launch {
            com.example.gopetalk_bot.main.AudioRmsMonitor.updateRmsDb(rmsdB)
        }
    }

    override fun onError(error: Int) {
        if (error != SpeechRecognizer.ERROR_CLIENT && error != SpeechRecognizer.ERROR_NO_MATCH) {
            logError("SpeechRecognizer error: $error", null)
        }
        startListeningForHotword()
    }


    override fun startCommandRecording() {
        audioRecordingManager.startCommandRecording()
    }

    override fun stopCommandRecording(): File? {
        return audioRecordingManager.stopCommandRecording()
    }

    override fun speak(text: String) {
        textToSpeechManager.speak(text)
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
        .setContentText("Escuchando por el hotword '$HOTWORD'...")
        .setSmallIcon(R.mipmap.ic_launcher)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        presenter.stop()
        speechRecognizerManager.destroy()
        audioRecordingManager.release()
        textToSpeechManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
