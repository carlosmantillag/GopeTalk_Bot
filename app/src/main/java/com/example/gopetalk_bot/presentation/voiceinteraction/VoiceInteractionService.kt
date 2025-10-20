package com.example.gopetalk_bot.presentation.voiceinteraction

import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
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
import com.example.gopetalk_bot.data.datasources.local.TextToSpeechDataSource
import com.example.gopetalk_bot.data.datasources.local.AudioPlayerDataSource
import com.example.gopetalk_bot.data.datasources.remote.WebSocketDataSource
import com.example.gopetalk_bot.data.repositories.ApiRepositoryImpl
import com.example.gopetalk_bot.data.repositories.AudioRepositoryImpl
import com.example.gopetalk_bot.data.repositories.TextToSpeechRepositoryImpl
import com.example.gopetalk_bot.data.repositories.WebSocketRepositoryImpl
import com.example.gopetalk_bot.data.repositories.AudioPlayerRepositoryImpl
import com.example.gopetalk_bot.domain.usecases.GetRecordedAudioUseCase
import com.example.gopetalk_bot.domain.usecases.MonitorAudioLevelUseCase
import com.example.gopetalk_bot.domain.usecases.PauseAudioRecordingUseCase
import com.example.gopetalk_bot.domain.usecases.ResumeAudioRecordingUseCase
import com.example.gopetalk_bot.domain.usecases.SendAudioCommandUseCase
import com.example.gopetalk_bot.domain.usecases.SpeakTextUseCase
import com.example.gopetalk_bot.domain.usecases.SetTtsListenerUseCase
import com.example.gopetalk_bot.domain.usecases.ShutdownTtsUseCase
import com.example.gopetalk_bot.domain.usecases.StartAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.StopAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.ConnectWebSocketUseCase
import com.example.gopetalk_bot.domain.usecases.DisconnectWebSocketUseCase
import com.example.gopetalk_bot.domain.usecases.PlayAudioFileUseCase
import com.example.gopetalk_bot.domain.usecases.UpdateWebSocketChannelUseCase

class VoiceInteractionService : Service(), VoiceInteractionContract.View {

    private lateinit var presenter: VoiceInteractionContract.Presenter

    override val context: Context
        get() = this

    companion object {
        private const val TAG = "VoiceInteractionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceInteractionChannel"
        const val ACTION_SPEAK_WELCOME = "com.example.gopetalk_bot.ACTION_SPEAK_WELCOME"
        const val EXTRA_USERNAME = "username"
    }

    override fun onCreate() {
        super.onCreate()
        
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        val audioDataSource = AudioDataSource(this)
        val audioRepository = AudioRepositoryImpl(audioDataSource)
        
        val userPreferences = com.example.gopetalk_bot.data.datasources.local.UserPreferences(this)
        val remoteDataSource = RemoteDataSource()
        val apiRepository = ApiRepositoryImpl(remoteDataSource, userPreferences)

        val ttsDataSource = TextToSpeechDataSource(this) { error ->
            logError("TTS Error: $error")
        }
        val ttsRepository = TextToSpeechRepositoryImpl(ttsDataSource)

        val startAudioMonitoringUseCase = StartAudioMonitoringUseCase(audioRepository)
        val stopAudioMonitoringUseCase = StopAudioMonitoringUseCase(audioRepository)
        val pauseAudioRecordingUseCase = PauseAudioRecordingUseCase(audioRepository)
        val resumeAudioRecordingUseCase = ResumeAudioRecordingUseCase(audioRepository)
        val monitorAudioLevelUseCase = MonitorAudioLevelUseCase(audioRepository)
        val getRecordedAudioUseCase = GetRecordedAudioUseCase(audioRepository)
        val sendAudioCommandUseCase = SendAudioCommandUseCase(apiRepository)
        val speakTextUseCase = SpeakTextUseCase(ttsRepository)
        val setTtsListenerUseCase = SetTtsListenerUseCase(ttsRepository)
        val shutdownTtsUseCase = ShutdownTtsUseCase(ttsRepository)
        val webSocketDataSource = WebSocketDataSource()
        val webSocketRepository = WebSocketRepositoryImpl(webSocketDataSource)
        val connectWebSocketUseCase = ConnectWebSocketUseCase(webSocketRepository)
        val disconnectWebSocketUseCase = DisconnectWebSocketUseCase(webSocketRepository)
        val updateWebSocketChannelUseCase = UpdateWebSocketChannelUseCase(webSocketRepository)
        val audioPlayerDataSource = AudioPlayerDataSource()
        val audioPlayerRepository = AudioPlayerRepositoryImpl(audioPlayerDataSource)
        val playAudioFileUseCase = PlayAudioFileUseCase(audioPlayerRepository)

        presenter = VoiceInteractionPresenter(
            view = this,
            startAudioMonitoringUseCase = startAudioMonitoringUseCase,
            stopAudioMonitoringUseCase = stopAudioMonitoringUseCase,
            pauseAudioRecordingUseCase = pauseAudioRecordingUseCase,
            resumeAudioRecordingUseCase = resumeAudioRecordingUseCase,
            monitorAudioLevelUseCase = monitorAudioLevelUseCase,
            getRecordedAudioUseCase = getRecordedAudioUseCase,
            sendAudioCommandUseCase = sendAudioCommandUseCase,
            speakTextUseCase = speakTextUseCase,
            setTtsListenerUseCase = setTtsListenerUseCase,
            shutdownTtsUseCase = shutdownTtsUseCase,
            connectWebSocketUseCase = connectWebSocketUseCase,
            disconnectWebSocketUseCase = disconnectWebSocketUseCase,
            playAudioFileUseCase = playAudioFileUseCase,
            updateWebSocketChannelUseCase = updateWebSocketChannelUseCase
        )
        
        presenter.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_SPEAK_WELCOME -> {
                    val username = it.getStringExtra(EXTRA_USERNAME) ?: "usuario"
                    presenter.speakWelcome(username)
                }
            }
        }
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
