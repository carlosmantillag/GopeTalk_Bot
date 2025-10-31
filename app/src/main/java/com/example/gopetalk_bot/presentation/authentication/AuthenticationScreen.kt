package com.example.gopetalk_bot.presentation.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gopetalk_bot.presentation.common.ParticleBackground
import kotlin.random.Random

@Composable
fun AuthenticationScreen(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    random: Random = Random.Default
) {
    ParticleBackground(
        modifier = modifier.testTag("authentication_screen"),
        particleCount = particleCount,
        random = random
    ) {
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
