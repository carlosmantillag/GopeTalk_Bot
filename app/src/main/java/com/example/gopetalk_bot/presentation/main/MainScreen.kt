package com.example.gopetalk_bot.presentation.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.gopetalk_bot.presentation.common.ParticleBackground
import kotlin.random.Random

@Composable
fun MainScreen(
    rms: Float,
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    random: Random = Random.Default
) {
    ParticleBackground(
        modifier = modifier.testTag("main_screen"),
        particleCount = particleCount,
        random = random
    ) {
        val sizeIncrease = (rms * 2f).coerceIn(80f, 100f)
        val size by animateDpAsState(
            targetValue = 150.dp + sizeIncrease.dp,
            animationSpec = tween(durationMillis = 100),
            label = "sphere_size"
        )

        Box(
            modifier = Modifier
                .size(size)
                .testTag("listening_sphere")
                .clip(CircleShape)
                .background(color = Color.Black)
        )
    }
}
