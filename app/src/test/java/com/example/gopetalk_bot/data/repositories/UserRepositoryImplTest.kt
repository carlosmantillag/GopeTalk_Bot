package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class UserRepositoryImplTest {

    private lateinit var userPreferences: UserPreferences
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setup() {
        userPreferences = mockk(relaxed = true)
        repository = UserRepositoryImpl(userPreferences)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getUsername should return username from preferences`() {
        every { userPreferences.username } returns "TestUser"

        val result = repository.getUsername()

        assertThat(result).isEqualTo("TestUser")
        verify { userPreferences.username }
    }

    @Test
    fun `setUsername should save username to preferences`() {
        repository.setUsername("NewUser")

        verify { userPreferences.username = "NewUser" }
    }

    @Test
    fun `getUsername should return empty string when no username set`() {
        every { userPreferences.username } returns ""

        val result = repository.getUsername()

        assertThat(result).isEmpty()
    }
}
