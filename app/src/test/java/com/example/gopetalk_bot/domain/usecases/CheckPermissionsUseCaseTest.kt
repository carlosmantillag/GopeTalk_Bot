package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.PermissionStatus
import com.example.gopetalk_bot.domain.repositories.PermissionRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class CheckPermissionsUseCaseTest {

    private lateinit var permissionRepository: PermissionRepository
    private lateinit var useCase: CheckPermissionsUseCase

    @Before
    fun setup() {
        permissionRepository = mockk(relaxed = true)
        useCase = CheckPermissionsUseCase(permissionRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should return permission status from repository`() {
        val expectedStatus = PermissionStatus(
            permissions = listOf("android.permission.RECORD_AUDIO"),
            allGranted = true
        )
        every { permissionRepository.getPermissionStatus() } returns expectedStatus

        val result = useCase.execute()

        assertThat(result).isEqualTo(expectedStatus)
        verify { permissionRepository.getPermissionStatus() }
    }

    @Test
    fun `execute should return not granted status when permissions missing`() {
        val expectedStatus = PermissionStatus(
            permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.INTERNET"),
            allGranted = false
        )
        every { permissionRepository.getPermissionStatus() } returns expectedStatus

        val result = useCase.execute()

        assertThat(result.allGranted).isFalse()
        assertThat(result.permissions).hasSize(2)
    }

    @Test
    fun `execute should handle empty permissions list`() {
        val expectedStatus = PermissionStatus(
            permissions = emptyList(),
            allGranted = true
        )
        every { permissionRepository.getPermissionStatus() } returns expectedStatus

        val result = useCase.execute()

        assertThat(result.permissions).isEmpty()
        assertThat(result.allGranted).isTrue()
    }

    @Test
    fun `execute should handle multiple consecutive calls`() {
        val status = PermissionStatus(
            permissions = listOf("android.permission.RECORD_AUDIO"),
            allGranted = true
        )
        every { permissionRepository.getPermissionStatus() } returns status

        useCase.execute()
        useCase.execute()
        useCase.execute()

        verify(exactly = 3) { permissionRepository.getPermissionStatus() }
    }

    @Test
    fun `execute should handle single permission granted`() {
        val status = PermissionStatus(
            permissions = listOf("android.permission.RECORD_AUDIO"),
            allGranted = true
        )
        every { permissionRepository.getPermissionStatus() } returns status

        val result = useCase.execute()

        assertThat(result.allGranted).isTrue()
        assertThat(result.permissions).hasSize(1)
    }

    @Test
    fun `execute should handle multiple permissions with mixed status`() {
        val status = PermissionStatus(
            permissions = listOf(
                "android.permission.RECORD_AUDIO",
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE"
            ),
            allGranted = false
        )
        every { permissionRepository.getPermissionStatus() } returns status

        val result = useCase.execute()

        assertThat(result.allGranted).isFalse()
        assertThat(result.permissions).hasSize(3)
    }
}
