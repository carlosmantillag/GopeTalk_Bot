package com.example.gopetalk_bot.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.gopetalk_bot.presentation.authentication.AuthenticationScreen
import org.junit.Rule
import org.junit.Test

class AuthenticationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun authenticationScreenShowsTitleAndStatus() {
        composeRule.setContent {
            AuthenticationScreen(particleCount = 0)
        }

        composeRule.onNodeWithText("Autenticación").assertIsDisplayed()
        composeRule.onNodeWithText("Escuchando...").assertIsDisplayed()
        composeRule.onNodeWithTag("authentication_sphere").assertIsDisplayed()
    }
}
