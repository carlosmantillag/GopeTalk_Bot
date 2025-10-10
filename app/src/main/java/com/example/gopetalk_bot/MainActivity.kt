package com.example.gopetalk_bot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import com.example.gopetalk_bot.ui.theme.GopeTalk_BotTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val permissionsToRequest by lazy {
        mutableListOf(
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.entries.all { it.value }

        if (allPermissionsGranted) {
            startVoiceService()
        } else {
            Toast.makeText(this, "Todos los permisos son necesarios para que la app funcione.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (areAllPermissionsGranted()) {
            startVoiceService()
        } else {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            GopeTalk_BotTheme { // Wrap with your app's theme
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF6A1B9A), Color(0xFF1976D2)) // Purple to Blue
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Floating particles from bottom to top
                        repeat(50) { index ->
                            val infiniteTransition = rememberInfiniteTransition(label = "particle_animation_" + index)

                            val initialX = remember { (Random.nextFloat() - 0.5f) * 400 } // horizontal range
                            val duration = remember { Random.nextInt(4000, 10000) }

                            val animatedAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.1f,
                                targetValue = 0.7f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = duration / 2, easing = LinearEasing),
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

                        val infiniteTransition = rememberInfiniteTransition(label = "sphere_rotation")
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
                                .size(200.dp) // Placeholder size for the sphere
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

    private fun areAllPermissionsGranted(): Boolean {
        return permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startVoiceService() {
        val intent = Intent(this, VoiceInteractionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}