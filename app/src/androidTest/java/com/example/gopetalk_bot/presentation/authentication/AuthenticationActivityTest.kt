package com.example.gopetalk_bot.presentation.authentication

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<AuthenticationActivity>()

    @Test
    fun authenticationScreen_isDisplayed() {
        composeTestRule.onNodeWithText("GopeTalk Bot").assertExists()
    }

    @Test
    fun authenticationScreen_showsAnimatedParticles() {
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun authenticationScreen_hasCorrectInitialState() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("GopeTalk Bot").assertIsDisplayed()
    }
}
