package com.example.gopetalk_bot.presentation.voiceinteraction

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gopetalk_bot.R
import com.example.gopetalk_bot.data.datasources.local.*
import com.example.gopetalk_bot.data.datasources.remote.*
import com.example.gopetalk_bot.data.repositories.*
import com.example.gopetalk_bot.domain.usecases.*

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
        val userPreferences = UserPreferences(this)
        val audioRepository = createAudioRepository()
        val apiRepository = createApiRepository(userPreferences)
        val ttsRepository = createTtsRepository()
        val webSocketRepository = createWebSocketRepository()
        val audioPlayerRepository = createAudioPlayerRepository()

        presenter = VoiceInteractionPresenter(
            view = this,
            startAudioMonitoringUseCase = StartAudioMonitoringUseCase(audioRepository),
            stopAudioMonitoringUseCase = StopAudioMonitoringUseCase(audioRepository),
            pauseAudioRecordingUseCase = PauseAudioRecordingUseCase(audioRepository),
            resumeAudioRecordingUseCase = ResumeAudioRecordingUseCase(audioRepository),
            monitorAudioLevelUseCase = MonitorAudioLevelUseCase(audioRepository),
            getRecordedAudioUseCase = GetRecordedAudioUseCase(audioRepository),
            sendAudioCommandUseCase = SendAudioCommandUseCase(apiRepository),
            speakTextUseCase = SpeakTextUseCase(ttsRepository),
            setTtsListenerUseCase = SetTtsListenerUseCase(ttsRepository),
            shutdownTtsUseCase = ShutdownTtsUseCase(ttsRepository),
            connectWebSocketUseCase = ConnectWebSocketUseCase(webSocketRepository),
            disconnectWebSocketUseCase = DisconnectWebSocketUseCase(webSocketRepository),
            playAudioFileUseCase = PlayAudioFileUseCase(audioPlayerRepository),
            updateWebSocketChannelUseCase = UpdateWebSocketChannelUseCase(webSocketRepository),
            pollAudioUseCase = PollAudioUseCase(apiRepository),
            userPreferences = userPreferences
        )
    }

    private fun createAudioRepository() = AudioRepositoryImpl(AudioDataSource(this))

    private fun createApiRepository(userPreferences: UserPreferences) = 
        ApiRepositoryImpl(RemoteDataSource(), userPreferences)

    private fun createTtsRepository() = TextToSpeechRepositoryImpl(
        TextToSpeechDataSource(this) { error -> logError("TTS Error: $error") }
    )

    private fun createWebSocketRepository() = WebSocketRepositoryImpl(WebSocketDataSource())

    private fun createAudioPlayerRepository() = AudioPlayerRepositoryImpl(AudioPlayerDataSource())

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
        
        // Navegar de vuelta a la pantalla de autenticación
        val intent = Intent(this, com.example.gopetalk_bot.presentation.authentication.AuthenticationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        // Detener el servicio
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
