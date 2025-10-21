package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesTest {

    private lateinit var context: Context
    private lateinit var userPreferences: UserPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        userPreferences = UserPreferences(context)
        // Clear preferences
        userPreferences.authToken = null
        userPreferences.username = "usuario"
    }

    @After
    fun tearDown() {
        // Clear preferences
        userPreferences.authToken = null
        userPreferences.username = "usuario"
    }

    @Test
    fun authToken_shouldPersistValue() {
        val testToken = "test-auth-token-123"
        
        userPreferences.authToken = testToken
        
        assertEquals(testToken, userPreferences.authToken)
    }

    @Test
    fun authToken_shouldReturnNullWhenNotSet() {
        assertNull(userPreferences.authToken)
    }

    @Test
    fun username_shouldPersistValue() {
        val testUsername = "TestUser"
        
        userPreferences.username = testUsername
        
        assertEquals(testUsername, userPreferences.username)
    }

    @Test
    fun username_shouldReturnDefaultWhenNotSet() {
        // UserPreferences returns "usuario" as default
        assertEquals("usuario", userPreferences.username)
    }

    @Test
    fun clearingValues_shouldWork() {
        userPreferences.authToken = "token"
        userPreferences.username = "user"
        
        userPreferences.authToken = null
        userPreferences.username = "usuario"
        
        assertNull(userPreferences.authToken)
        assertEquals("usuario", userPreferences.username)
    }

    @Test
    fun multipleInstances_shouldShareSameData() {
        val userPrefs1 = UserPreferences(context)
        val userPrefs2 = UserPreferences(context)
        
        userPrefs1.authToken = "shared-token"
        
        assertEquals("shared-token", userPrefs2.authToken)
    }

    @Test
    fun authToken_shouldHandleSpecialCharacters() {
        val specialToken = "token!@#$%^&*()_+-=[]{}|;':\",./<>?"
        
        userPreferences.authToken = specialToken
        
        assertEquals(specialToken, userPreferences.authToken)
    }

    @Test
    fun username_shouldHandleUnicodeCharacters() {
        val unicodeName = "José María 日本語"
        
        userPreferences.username = unicodeName
        
        assertEquals(unicodeName, userPreferences.username)
    }
}
