package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.repositories.ApiRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class SendAuthenticationUseCaseTest {

    private lateinit var apiRepository: ApiRepository
    private lateinit var useCase: SendAuthenticationUseCase

    @Before
    fun setup() {
        apiRepository = mockk(relaxed = true)
        useCase = SendAuthenticationUseCase(apiRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository sendAuthentication with correct parameters`() {
        val nombre = "TestUser"
        val pin = 1234
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        useCase.execute(nombre, pin, callback)

        verify { apiRepository.sendAuthentication(nombre, pin, callback) }
    }

    @Test
    fun `execute should invoke callback with success response`() {
        val nombre = "TestUser"
        val pin = 1234
        val successResponse = ApiResponse.Success(200, "Authentication successful", null)
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        every { 
            apiRepository.sendAuthentication(any(), any(), any()) 
        } answers {
            val cb = thirdArg<(ApiResponse) -> Unit>()
            cb(successResponse)
        }

        useCase.execute(nombre, pin, callback)

        verify { callback(successResponse) }
    }

    @Test
    fun `execute should invoke callback with error response`() {
        val nombre = "TestUser"
        val pin = 1234
        val errorResponse = ApiResponse.Error("Invalid credentials", null)
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        every { 
            apiRepository.sendAuthentication(any(), any(), any()) 
        } answers {
            val cb = thirdArg<(ApiResponse) -> Unit>()
            cb(errorResponse)
        }

        useCase.execute(nombre, pin, callback)

        verify { callback(errorResponse) }
    }

    @Test
    fun `execute should handle special characters in nombre`() {
        val nombre = "José María"
        val pin = 1234
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        useCase.execute(nombre, pin, callback)

        verify { apiRepository.sendAuthentication(nombre, pin, callback) }
    }

    @Test
    fun `execute should handle 4-digit PIN`() {
        val nombre = "TestUser"
        val pin = 9999
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        useCase.execute(nombre, pin, callback)

        verify { apiRepository.sendAuthentication(nombre, pin, callback) }
    }
}
