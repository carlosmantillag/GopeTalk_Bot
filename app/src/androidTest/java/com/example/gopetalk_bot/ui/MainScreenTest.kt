package com.example.gopetalk_bot.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.gopetalk_bot.presentation.main.MainScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun sphereRespondsToAudioLevels() {
        var updateRms: (Float) -> Unit = {}

        composeRule.setContent {
            var rmsState by remember { mutableStateOf(0f) }
            updateRms = { value -> rmsState = value }
            MainScreen(
                rms = rmsState,
                particleCount = 0
            )
        }

        composeRule.mainClock.advanceTimeBy(200L)
        composeRule.runOnIdle { updateRms(60f) }
        composeRule.mainClock.advanceTimeBy(300L)

        composeRule.onNodeWithTag("listening_sphere")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(250.dp)
            .assertWidthIsEqualTo(250.dp)
    }
}
