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
import com.example.gopetalk_bot.data.datasources.local.AudioDataSource
import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
import com.example.gopetalk_bot.data.repositories.ApiRepositoryImpl
import com.example.gopetalk_bot.data.repositories.AudioRepositoryImpl
import com.example.gopetalk_bot.domain.usecases.GetRecordedAudioUseCase
import com.example.gopetalk_bot.domain.usecases.MonitorAudioLevelUseCase
import com.example.gopetalk_bot.domain.usecases.SendAudioCommandUseCase
import com.example.gopetalk_bot.domain.usecases.StartAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.StopAudioMonitoringUseCase

/**
 * Service for Voice Interaction following MVP + Clean Architecture
 */
class VoiceInteractionService : Service(), VoiceInteractionContract.View {

    private lateinit var presenter: VoiceInteractionContract.Presenter

    override val context: Context
        get() = this

    companion object {
        private const val TAG = "VoiceInteractionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceInteractionChannel"
    }

    override fun onCreate() {
        super.onCreate()
        
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Manual dependency injection
        val audioDataSource = AudioDataSource(this)
        val audioRepository = AudioRepositoryImpl(audioDataSource)
        
        val remoteDataSource = RemoteDataSource()
        val apiRepository = ApiRepositoryImpl(remoteDataSource)
        
        val startAudioMonitoringUseCase = StartAudioMonitoringUseCase(audioRepository)
        val stopAudioMonitoringUseCase = StopAudioMonitoringUseCase(audioRepository)
        val monitorAudioLevelUseCase = MonitorAudioLevelUseCase(audioRepository)
        val getRecordedAudioUseCase = GetRecordedAudioUseCase(audioRepository)
        val sendAudioCommandUseCase = SendAudioCommandUseCase(apiRepository)
        
        presenter = VoiceInteractionPresenter(
            view = this,
            startAudioMonitoringUseCase = startAudioMonitoringUseCase,
            stopAudioMonitoringUseCase = stopAudioMonitoringUseCase,
            monitorAudioLevelUseCase = monitorAudioLevelUseCase,
            getRecordedAudioUseCase = getRecordedAudioUseCase,
            sendAudioCommandUseCase = sendAudioCommandUseCase
        )
        
        presenter.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Interaction Service",
                NotificationManager.IMPORTANCE_LOW
            )
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
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
