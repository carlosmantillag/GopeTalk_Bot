package com.example.gopetalk_bot.presentation.voiceinteraction

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
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.di.ServiceLocator
import com.example.gopetalk_bot.presentation.authentication.AuthenticationActivity

class VoiceInteractionService : Service(), VoiceInteractionContract.View {

    companion object {
        private const val TAG = "VoiceInteractionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceInteractionChannel"
        private const val CHANNEL_NAME = "Voice Interaction Service"
        private const val NOTIFICATION_TITLE = "GopeTalk Bot Activo"
        private const val NOTIFICATION_TEXT = "Escuchando..."
        const val ACTION_SPEAK_WELCOME = "com.example.gopetalk_bot.ACTION_SPEAK_WELCOME"
        const val EXTRA_USERNAME = "username"
        private const val DEFAULT_USERNAME = "usuario"
    }

    private lateinit var presenter: VoiceInteractionContract.Presenter

    override val context: Context get() = this

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initializePresenter()
        presenter.start()
    }

    private fun initializePresenter() {
        presenter = ServiceLocator.provideVoiceInteractionPresenter(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntent(intent)
        return START_NOT_STICKY
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SPEAK_WELCOME) {
            val username = intent.getStringExtra(EXTRA_USERNAME) ?: DEFAULT_USERNAME
            presenter.speakWelcome(username)
        }
    }

    override fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    override fun logError(message: String, t: Throwable?) {
        if (t != null) Log.e(TAG, message, t) else Log.e(TAG, message)
    }

    override fun logout() {
        logInfo("Logging out user")
        val userPreferences = UserPreferences(this)
        userPreferences.clearSession()
        
        
        val intent = Intent(this, AuthenticationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        
        stopSelf()
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(NOTIFICATION_TITLE)
        .setContentText(NOTIFICATION_TEXT)
        .setSmallIcon(R.mipmap.ic_launcher)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        logInfo("Service is being destroyed")
        presenter.stop()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
