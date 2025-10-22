package com.example.gopetalk_bot.data.datasources.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class PermissionDataSourceTest {

    private lateinit var dataSource: PermissionDataSource
    private lateinit var mockContext: Context
    private lateinit var mockPermissionChecker: PermissionChecker

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPermissionChecker = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getRequiredPermissions should return RECORD_AUDIO on API less than 33`() {
        every { mockPermissionChecker.getSdkVersion() } returns 28
        
        dataSource = PermissionDataSource(mockContext, mockPermissionChecker)
        val permissions = dataSource.getRequiredPermissions()

        assertThat(permissions).contains(Manifest.permission.RECORD_AUDIO)
        assertThat(permissions).doesNotContain(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(permissions).hasSize(1)
    }

    @Test
    fun `getRequiredPermissions should include POST_NOTIFICATIONS on API 33+`() {
        every { mockPermissionChecker.getSdkVersion() } returns Build.VERSION_CODES.TIRAMISU
        
        dataSource = PermissionDataSource(mockContext, mockPermissionChecker)
        val permissions = dataSource.getRequiredPermissions()

        assertThat(permissions).contains(Manifest.permission.RECORD_AUDIO)
        assertThat(permissions).contains(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(permissions).hasSize(2)
    }

    @Test
    fun `areAllPermissionsGranted should return true when all permissions granted on API 28`() {
        every { mockPermissionChecker.getSdkVersion() } returns 28
        every { 
            mockPermissionChecker.checkPermission(mockContext, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED
        
        dataSource = PermissionDataSource(mockContext, mockPermissionChecker)
        val result = dataSource.areAllPermissionsGranted()

        assertThat(result).isTrue()
        verify { mockPermissionChecker.checkPermission(mockContext, Manifest.permission.RECORD_AUDIO) }
    }

    @Test
    fun `areAllPermissionsGranted should return false when RECORD_AUDIO denied`() {
        every { mockPermissionChecker.getSdkVersion() } returns 28
        every { 
            mockPermissionChecker.checkPermission(mockContext, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_DENIED
        
        dataSource = PermissionDataSource(mockContext, mockPermissionChecker)
        val result = dataSource.areAllPermissionsGranted()

        assertThat(result).isFalse()
    }

    @Test
    fun `areAllPermissionsGranted should return false when POST_NOTIFICATIONS denied on API 33+`() {
        every { mockPermissionChecker.getSdkVersion() } returns Build.VERSION_CODES.TIRAMISU
        every { 
            mockPermissionChecker.checkPermission(mockContext, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED
        every { 
            mockPermissionChecker.checkPermission(mockContext, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_DENIED
        
        dataSource = PermissionDataSource(mockContext, mockPermissionChecker)
        val result = dataSource.areAllPermissionsGranted()

        assertThat(result).isFalse()
    }

    @Test
    fun `areAllPermissionsGranted should return true when all permissions granted on API 33+`() {
        every { mockPermissionChecker.getSdkVersion() } returns Build.VERSION_CODES.TIRAMISU
        every { 
            mockPermissionChecker.checkPermission(mockContext, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED
        every { 
            mockPermissionChecker.checkPermission(mockContext, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED
        
        dataSource = PermissionDataSource(mockContext, mockPermissionChecker)
        val result = dataSource.areAllPermissionsGranted()

        assertThat(result).isTrue()
        verify { mockPermissionChecker.checkPermission(mockContext, Manifest.permission.RECORD_AUDIO) }
        verify { mockPermissionChecker.checkPermission(mockContext, Manifest.permission.POST_NOTIFICATIONS) }
    }

    @Test
    fun `getRequiredPermissions should return same list on multiple calls with same SDK`() {
        every { mockPermissionChecker.getSdkVersion() } returns 28
        
        dataSource = PermissionDataSource(mockContext, mockPermissionChecker)
        val permissions1 = dataSource.getRequiredPermissions()
        val permissions2 = dataSource.getRequiredPermissions()

        assertThat(permissions1).isEqualTo(permissions2)
    }
}
