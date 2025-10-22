package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.PermissionDataSource
import com.example.gopetalk_bot.domain.entities.PermissionStatus
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class PermissionRepositoryImplTest {

    private lateinit var permissionDataSource: PermissionDataSource
    private lateinit var repository: PermissionRepositoryImpl

    @Before
    fun setup() {
        permissionDataSource = mockk(relaxed = true)
        repository = PermissionRepositoryImpl(permissionDataSource)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getRequiredPermissions should return permissions from data source`() {
        val expectedPermissions = listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
        )
        every { permissionDataSource.getRequiredPermissions() } returns expectedPermissions

        val result = repository.getRequiredPermissions()

        assertThat(result).isEqualTo(expectedPermissions)
        verify { permissionDataSource.getRequiredPermissions() }
    }

    @Test
    fun `areAllPermissionsGranted should return true when all granted`() {
        every { permissionDataSource.areAllPermissionsGranted() } returns true

        val result = repository.areAllPermissionsGranted()

        assertThat(result).isTrue()
        verify { permissionDataSource.areAllPermissionsGranted() }
    }

    @Test
    fun `areAllPermissionsGranted should return false when not all granted`() {
        every { permissionDataSource.areAllPermissionsGranted() } returns false

        val result = repository.areAllPermissionsGranted()

        assertThat(result).isFalse()
        verify { permissionDataSource.areAllPermissionsGranted() }
    }

    @Test
    fun `getPermissionStatus should return correct status when all granted`() {
        val permissions = listOf("android.permission.RECORD_AUDIO")
        every { permissionDataSource.getRequiredPermissions() } returns permissions
        every { permissionDataSource.areAllPermissionsGranted() } returns true

        val result = repository.getPermissionStatus()

        assertThat(result.permissions).isEqualTo(permissions)
        assertThat(result.allGranted).isTrue()
    }

    @Test
    fun `getPermissionStatus should return correct status when not all granted`() {
        val permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.INTERNET")
        every { permissionDataSource.getRequiredPermissions() } returns permissions
        every { permissionDataSource.areAllPermissionsGranted() } returns false

        val result = repository.getPermissionStatus()

        assertThat(result.permissions).isEqualTo(permissions)
        assertThat(result.allGranted).isFalse()
    }

    @Test
    fun `getPermissionStatus should handle empty permissions list`() {
        every { permissionDataSource.getRequiredPermissions() } returns emptyList()
        every { permissionDataSource.areAllPermissionsGranted() } returns true

        val result = repository.getPermissionStatus()

        assertThat(result.permissions).isEmpty()
        assertThat(result.allGranted).isTrue()
    }

    @Test
    fun `getRequiredPermissions should handle single permission`() {
        val permissions = listOf("android.permission.RECORD_AUDIO")
        every { permissionDataSource.getRequiredPermissions() } returns permissions

        val result = repository.getRequiredPermissions()

        assertThat(result).hasSize(1)
        assertThat(result).contains("android.permission.RECORD_AUDIO")
    }

    @Test
    fun `getRequiredPermissions should handle multiple permissions`() {
        val permissions = listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.POST_NOTIFICATIONS"
        )
        every { permissionDataSource.getRequiredPermissions() } returns permissions

        val result = repository.getRequiredPermissions()

        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyElementsIn(permissions)
    }

    @Test
    fun `getPermissionStatus should be called multiple times`() {
        val permissions = listOf("android.permission.RECORD_AUDIO")
        every { permissionDataSource.getRequiredPermissions() } returns permissions
        every { permissionDataSource.areAllPermissionsGranted() } returns true

        repository.getPermissionStatus()
        repository.getPermissionStatus()
        repository.getPermissionStatus()

        verify(exactly = 3) { permissionDataSource.getRequiredPermissions() }
        verify(exactly = 3) { permissionDataSource.areAllPermissionsGranted() }
    }

    @Test
    fun `areAllPermissionsGranted should be idempotent`() {
        every { permissionDataSource.areAllPermissionsGranted() } returns true

        val result1 = repository.areAllPermissionsGranted()
        val result2 = repository.areAllPermissionsGranted()

        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `getRequiredPermissions should return same list on multiple calls`() {
        val permissions = listOf("android.permission.RECORD_AUDIO")
        every { permissionDataSource.getRequiredPermissions() } returns permissions

        val result1 = repository.getRequiredPermissions()
        val result2 = repository.getRequiredPermissions()

        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `getPermissionStatus with large permission list should work`() {
        val permissions = (1..100).map { "android.permission.PERMISSION_$it" }
        every { permissionDataSource.getRequiredPermissions() } returns permissions
        every { permissionDataSource.areAllPermissionsGranted() } returns false

        val result = repository.getPermissionStatus()

        assertThat(result.permissions).hasSize(100)
        assertThat(result.allGranted).isFalse()
    }
}
