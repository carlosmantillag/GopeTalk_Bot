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

    @Test
    fun `setUsername with empty string should work`() {
        repository.setUsername("")

        verify { userPreferences.username = "" }
    }

    @Test
    fun `setUsername with long username should work`() {
        val longUsername = "a".repeat(1000)
        
        repository.setUsername(longUsername)

        verify { userPreferences.username = longUsername }
    }

    @Test
    fun `setUsername with special characters should work`() {
        val specialUsername = "user@123!#$%"
        
        repository.setUsername(specialUsername)

        verify { userPreferences.username = specialUsername }
    }

    @Test
    fun `getUsername with special characters should work`() {
        val specialUsername = "user@123!#$%"
        every { userPreferences.username } returns specialUsername

        val result = repository.getUsername()

        assertThat(result).isEqualTo(specialUsername)
    }

    @Test
    fun `multiple setUsername calls should work`() {
        repository.setUsername("User1")
        repository.setUsername("User2")
        repository.setUsername("User3")

        verify(exactly = 1) { userPreferences.username = "User1" }
        verify(exactly = 1) { userPreferences.username = "User2" }
        verify(exactly = 1) { userPreferences.username = "User3" }
    }

    @Test
    fun `getUsername should be called multiple times`() {
        every { userPreferences.username } returns "TestUser"

        repository.getUsername()
        repository.getUsername()
        repository.getUsername()

        verify(exactly = 3) { userPreferences.username }
    }

    @Test
    fun `setUsername then getUsername should return same value`() {
        val username = "TestUser"
        every { userPreferences.username } returns username

        repository.setUsername(username)
        val result = repository.getUsername()

        assertThat(result).isEqualTo(username)
        verify { userPreferences.username = username }
        verify { userPreferences.username }
    }

    @Test
    fun `getUsername with null-like string should work`() {
        every { userPreferences.username } returns "null"

        val result = repository.getUsername()

        assertThat(result).isEqualTo("null")
    }

    @Test
    fun `setUsername with whitespace should work`() {
        repository.setUsername("   ")

        verify { userPreferences.username = "   " }
    }

    @Test
    fun `getUsername with unicode characters should work`() {
        val unicodeUsername = "用户名123"
        every { userPreferences.username } returns unicodeUsername

        val result = repository.getUsername()

        assertThat(result).isEqualTo(unicodeUsername)
    }
}
