package com.example.gopetalk_bot.presentation.authentication

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun AuthenticationScreen(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    random: Random = Random.Default
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("authentication_screen")
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
        repeat(particleCount) { index ->
            val infiniteTransition = rememberInfiniteTransition(
                label = "auth_particle_animation_$index"
            )

            val initialX = remember(index) {
                (random.nextFloat() - 0.5f) * 400
            }
            val duration = remember(index) { random.nextInt(4000, 10000) }

            val animatedAlpha by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = duration / 2,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "auth_particle_alpha_$index"
            )

            val animatedY by infiniteTransition.animateFloat(
                initialValue = 400f,
                targetValue = -400f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "auth_particle_y_$index"
            )

            Box(
                modifier = Modifier
                    .offset(x = initialX.dp, y = animatedY.dp)
                    .size(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = animatedAlpha))
            )
        }

        Column(
            modifier = Modifier.testTag("authentication_content"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("authentication_sphere")
                    .clip(CircleShape)
                    .background(color = Color.Black)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Autenticación",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("authentication_title")
            )

            Text(
                text = "Escuchando...",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.testTag("authentication_status")
            )
        }
    }
}
