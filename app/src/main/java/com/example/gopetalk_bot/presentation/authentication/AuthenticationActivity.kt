package com.example.gopetalk_bot.presentation.authentication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gopetalk_bot.data.datasources.local.PermissionDataSource
import com.example.gopetalk_bot.data.datasources.local.TextToSpeechDataSource
import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
import com.example.gopetalk_bot.data.repositories.ApiRepositoryImpl
import com.example.gopetalk_bot.data.repositories.PermissionRepositoryImpl
import com.example.gopetalk_bot.data.repositories.TextToSpeechRepositoryImpl
import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase
import com.example.gopetalk_bot.domain.usecases.SendAuthenticationUseCase
import com.example.gopetalk_bot.domain.usecases.SetTtsListenerUseCase
import com.example.gopetalk_bot.domain.usecases.ShutdownTtsUseCase
import com.example.gopetalk_bot.domain.usecases.SpeakTextUseCase
import com.example.gopetalk_bot.presentation.main.MainActivity
import com.example.gopetalk_bot.ui.theme.GopeTalk_BotTheme

class AuthenticationActivity : ComponentActivity(), AuthenticationContract.View {

    private var presenter: AuthenticationContract.Presenter? = null

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        presenter?.onPermissionsResult(permissions.entries.all { it.value })
    }

    override val context: Context
        get() = this

    companion object {
        private const val TAG = "AuthenticationActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPreferences = com.example.gopetalk_bot.data.datasources.local.UserPreferences(this)
        
        // Verificar si ya hay una sesión activa
        if (userPreferences.hasActiveSession()) {
            logInfo("Active session found, navigating to MainActivity")
            navigateToMainActivity()
            finish()
            return
        }
        
        val speechRecognizerDataSource = com.example.gopetalk_bot.data.datasources.local.SpeechRecognizerDataSource(this)
        val permissionDataSource = PermissionDataSource(this)
        val permissionRepository = PermissionRepositoryImpl(permissionDataSource)
        val checkPermissionsUseCase = CheckPermissionsUseCase(permissionRepository)
        
        val remoteDataSource = RemoteDataSource()
        val apiRepository = ApiRepositoryImpl(remoteDataSource, userPreferences)

        val ttsDataSource = TextToSpeechDataSource(
            context = this,
            onInitError = { error -> logError("TTS Error: $error") }
        )
        val ttsRepository = TextToSpeechRepositoryImpl(ttsDataSource)

        val sendAuthenticationUseCase = SendAuthenticationUseCase(apiRepository)
        val speakTextUseCase = SpeakTextUseCase(ttsRepository)
        val setTtsListenerUseCase = SetTtsListenerUseCase(ttsRepository)
        val shutdownTtsUseCase = ShutdownTtsUseCase(ttsRepository)

        presenter = AuthenticationPresenter(
            view = this,
            speechRecognizerDataSource = speechRecognizerDataSource,
            sendAuthenticationUseCase = sendAuthenticationUseCase,
            speakTextUseCase = speakTextUseCase,
            setTtsListenerUseCase = setTtsListenerUseCase,
            shutdownTtsUseCase = shutdownTtsUseCase,
            userPreferences = userPreferences,
            checkPermissionsUseCase = checkPermissionsUseCase
        )

        presenter?.onViewCreated()

        setContent {
            GopeTalk_BotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthenticationScreen()
                }
            }
        }
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

    override fun navigateToMainActivity() {
        logInfo("Authentication successful, navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun showAuthenticationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun requestPermissions(permissions: Array<String>) {
        requestMultiplePermissionsLauncher.launch(permissions)
    }

    override fun showPermissionsRequiredError() {
        Toast.makeText(
            this,
            "Se requieren permisos de micrófono para continuar",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.stop()
    }
}