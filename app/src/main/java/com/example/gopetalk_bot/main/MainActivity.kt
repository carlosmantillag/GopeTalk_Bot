package com.example.gopetalk_bot.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.gopetalk_bot.voiceinteraction.VoiceInteractionService
import com.example.gopetalk_bot.ui.theme.GopeTalk_BotTheme
import kotlin.math.max
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

        presenter = MainPresenter(this, this)
        presenter.onViewCreated()

        setContent {
            GopeTalk_BotTheme { // Wrap with your app's theme
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF6A1B9A),
                                        Color(0xFF1976D2)
                                    ) // Purple to Blue
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Floating particles from bottom to top
                        repeat(50) { index ->
                            val infiniteTransition =
                                rememberInfiniteTransition(label = "particle_animation_" + index)

                            val initialX =
                                remember { (Random.nextFloat() - 0.5f) * 400 } // horizontal range
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
                                ), label = "particle_alpha_" + index
                            )

                            val animatedY by infiniteTransition.animateFloat(
                                initialValue = 400f, // Start from bottom
                                targetValue = -400f, // Move to top
                                animationSpec = infiniteRepeatable(
                                    animation = tween(duration, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ), label = "particle_y_" + index
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = initialX.dp, y = animatedY.dp)
                                    .size(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = animatedAlpha))
                            )
                        }

                        val rmsDb by AudioRmsMonitor.rmsDbFlow.collectAsState(initial = 0f)
                        val sizeIncrease = (max(0f, rmsDb) * 20f).coerceIn(0f, 100f)
                        val size by animateDpAsState(
                            targetValue = 200.dp + sizeIncrease.dp,
                            animationSpec = tween(durationMillis = 100), label = "sphere_size"
                        )

                        val infiniteTransition =
                            rememberInfiniteTransition(label = "sphere_rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 5000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "sphere_rotation_animation"
                        )

                        Box(
                            modifier = Modifier
                                .size(size) // Placeholder size for the sphere
                                .clip(CircleShape)
                                .background(Color.Black)
                                .graphicsLayer {
                                    rotationZ = rotation
                                }
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
        Toast.makeText(this, "Todos los permisos son necesarios para que la app funcione.", Toast.LENGTH_LONG).show()
    }

    override fun startVoiceService() {
        val intent = Intent(this, VoiceInteractionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}