package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserPreferencesTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var userPreferences: UserPreferences

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } just Runs

        userPreferences = UserPreferences(context)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `username should return default value when not set`() {
        every { sharedPreferences.getString("username", "usuario") } returns "usuario"

        val username = userPreferences.username

        assertThat(username).isEqualTo("usuario")
    }

    @Test
    fun `username should return stored value`() {
        every { sharedPreferences.getString("username", "usuario") } returns "testuser"

        val username = userPreferences.username

        assertThat(username).isEqualTo("testuser")
    }

    @Test
    fun `username setter should store value in preferences`() {
        userPreferences.username = "newuser"

        verify { editor.putString("username", "newuser") }
        verify { editor.apply() }
    }

    @Test
    fun `authToken should return null when not set`() {
        every { sharedPreferences.getString("auth_token", null) } returns null

        val token = userPreferences.authToken

        assertThat(token).isNull()
    }

    @Test
    fun `authToken should return stored value`() {
        every { sharedPreferences.getString("auth_token", null) } returns "test-token-123"

        val token = userPreferences.authToken

        assertThat(token).isEqualTo("test-token-123")
    }

    @Test
    fun `authToken setter should store value in preferences`() {
        userPreferences.authToken = "new-token"

        verify { editor.putString("auth_token", "new-token") }
        verify { editor.apply() }
    }

    @Test
    fun `authToken setter should store null value`() {
        userPreferences.authToken = null

        verify { editor.putString("auth_token", null) }
        verify { editor.apply() }
    }

    @Test
    fun `hasActiveSession should return true when token and username exist`() {
        every { sharedPreferences.getString("auth_token", null) } returns "token"
        every { sharedPreferences.getString("username", "usuario") } returns "user"

        val hasSession = userPreferences.hasActiveSession()

        assertThat(hasSession).isTrue()
    }

    @Test
    fun `hasActiveSession should return false when token is null`() {
        every { sharedPreferences.getString("auth_token", null) } returns null
        every { sharedPreferences.getString("username", "usuario") } returns "user"

        val hasSession = userPreferences.hasActiveSession()

        assertThat(hasSession).isFalse()
    }

    @Test
    fun `hasActiveSession should return false when token is empty`() {
        every { sharedPreferences.getString("auth_token", null) } returns ""
        every { sharedPreferences.getString("username", "usuario") } returns "user"

        val hasSession = userPreferences.hasActiveSession()

        assertThat(hasSession).isFalse()
    }

    @Test
    fun `hasActiveSession should return false when username is empty`() {
        every { sharedPreferences.getString("auth_token", null) } returns "token"
        every { sharedPreferences.getString("username", "usuario") } returns ""

        val hasSession = userPreferences.hasActiveSession()

        assertThat(hasSession).isFalse()
    }

    @Test
    fun `clearSession should remove token and username`() {
        userPreferences.clearSession()

        verify { editor.remove("auth_token") }
        verify { editor.remove("username") }
        verify { editor.apply() }
    }

    @Test
    fun `clearSession should be idempotent`() {
        userPreferences.clearSession()
        userPreferences.clearSession()

        verify(exactly = 2) { editor.remove("auth_token") }
        verify(exactly = 2) { editor.remove("username") }
    }

    @Test
    fun `username getter should handle null from SharedPreferences`() {
        every { sharedPreferences.getString("username", "usuario") } returns null

        val username = userPreferences.username

        assertThat(username).isEqualTo("usuario")
    }

    @Test
    fun `setting username multiple times should work`() {
        userPreferences.username = "user1"
        userPreferences.username = "user2"
        userPreferences.username = "user3"

        verify { editor.putString("username", "user1") }
        verify { editor.putString("username", "user2") }
        verify { editor.putString("username", "user3") }
        verify(exactly = 3) { editor.apply() }
    }

    @Test
    fun `setting authToken multiple times should work`() {
        userPreferences.authToken = "token1"
        userPreferences.authToken = "token2"

        verify { editor.putString("auth_token", "token1") }
        verify { editor.putString("auth_token", "token2") }
        verify(exactly = 2) { editor.apply() }
    }
}
