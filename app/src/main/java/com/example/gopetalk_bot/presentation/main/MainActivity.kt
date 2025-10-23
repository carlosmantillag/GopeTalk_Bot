package com.example.gopetalk_bot.presentation.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.gopetalk_bot.data.datasources.local.PermissionDataSource
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.repositories.PermissionRepositoryImpl
import com.example.gopetalk_bot.data.repositories.UserRepositoryImpl
import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitor
import com.example.gopetalk_bot.presentation.voiceinteraction.VoiceInteractionService
import com.example.gopetalk_bot.ui.theme.GopeTalk_BotTheme

class MainActivity : ComponentActivity(), MainContract.View {

    private lateinit var presenter: MainContract.Presenter

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        presenter.onPermissionsResult(permissions.entries.all { it.value })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionDataSource = PermissionDataSource(this)
        val permissionRepository = PermissionRepositoryImpl(permissionDataSource)
        val checkPermissionsUseCase = CheckPermissionsUseCase(permissionRepository)
        val userPreferences = UserPreferences(this)
        val userRepository = UserRepositoryImpl(userPreferences)
        
        presenter = MainPresenter(this, checkPermissionsUseCase, userRepository)
        
        // Check permissions and start voice service
        presenter.onViewCreated()

        setContent {
            GopeTalk_BotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val rms by AudioRmsMonitor.rmsDbFlow.collectAsState(initial = 0f)
                    MainScreen(rms = rms)
                }
            }
        }
    }

    override fun requestPermissions(permissions: Array<String>) {
        requestMultiplePermissionsLauncher.launch(permissions)
    }

    override fun showPermissionsRequiredError() {
        Toast.makeText(
            this,
            "Se requieren permisos para continuar",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun startVoiceService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, VoiceInteractionService::class.java))
        } else {
            startService(Intent(this, VoiceInteractionService::class.java))
        }
    }
    
    override fun speakWelcomeMessage(username: String) {
        val intent = Intent(this, VoiceInteractionService::class.java).apply {
            action = VoiceInteractionService.ACTION_SPEAK_WELCOME
            putExtra(VoiceInteractionService.EXTRA_USERNAME, username)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the voice interaction service when the app is closed
        stopService(Intent(this, VoiceInteractionService::class.java))
    }
}