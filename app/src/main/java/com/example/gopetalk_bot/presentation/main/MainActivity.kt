package com.example.gopetalk_bot.presentation.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gopetalk_bot.data.datasources.local.PermissionDataSource
import com.example.gopetalk_bot.data.repositories.PermissionRepositoryImpl
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.repositories.UserRepositoryImpl
import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitor
import com.example.gopetalk_bot.presentation.voiceinteraction.VoiceInteractionService
import com.example.gopetalk_bot.ui.theme.GopeTalk_BotTheme
import kotlin.random.Random

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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2E0C43),
                                        Color(0xFF0E4377)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        repeat(50) { index ->
                            val infiniteTransition =
                                rememberInfiniteTransition(label = "particle_animation_$index")

                            val initialX =
                                remember { (Random.nextFloat() - 0.5f) * 400 }
                            val duration = remember { Random.nextInt(4000, 10000) }

                            val animatedAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.1f,
                                targetValue = 0.7f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = duration / 2,
                                        easing = LinearEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "particle_alpha_$index"
                            )

                            val animatedY by infiniteTransition.animateFloat(
                                initialValue = 400f,
                                targetValue = -400f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(duration, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ), label = "particle_y_$index"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = initialX.dp, y = animatedY.dp)
                                    .size(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = animatedAlpha))
                            )
                        }

                        val rms by AudioRmsMonitor.rmsDbFlow.collectAsState(initial = 0f)
                        val sizeIncrease = (rms * 2f).coerceIn(80f, 100f)
                        val size by animateDpAsState(
                            targetValue = 150.dp + sizeIncrease.dp,
                            animationSpec = tween(durationMillis = 100), label = "sphere_size"
                        )

                        Box(
                            modifier = Modifier
                                .size(size)
                                .clip(CircleShape)
                                .background(color = Color.Black)
                        )
                    }
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
}
