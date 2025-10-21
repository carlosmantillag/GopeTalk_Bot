package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class PermissionDataSourceTest {

    private lateinit var context: Context
    private lateinit var dataSource: PermissionDataSource
    private var originalSdkInt: Int = Build.VERSION.SDK_INT

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        dataSource = PermissionDataSource(context)
        originalSdkInt = Build.VERSION.SDK_INT
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        setStaticSdkInt(originalSdkInt)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `getRequiredPermissions should include only record audio on pre T devices`() {
        setStaticSdkInt(Build.VERSION_CODES.S)

        val permissions = dataSource.getRequiredPermissions()

        assertThat(permissions).containsExactly(android.Manifest.permission.RECORD_AUDIO)
    }

    @Test
    fun `getRequiredPermissions should include post notifications on T and above`() {
        setStaticSdkInt(Build.VERSION_CODES.TIRAMISU)

        val permissions = dataSource.getRequiredPermissions()

        assertThat(permissions).containsExactly(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    }

    @Test
    fun `areAllPermissionsGranted should return true when all permissions granted`() {
        setStaticSdkInt(Build.VERSION_CODES.TIRAMISU)

        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED

        val result = dataSource.areAllPermissionsGranted()

        assertThat(result).isTrue()
    }

    @Test
    fun `areAllPermissionsGranted should return false when at least one permission denied`() {
        setStaticSdkInt(Build.VERSION_CODES.TIRAMISU)

        every { ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) } returns PackageManager.PERMISSION_GRANTED
        every { ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_DENIED

        val result = dataSource.areAllPermissionsGranted()

        assertThat(result).isFalse()
    }

    private fun setStaticSdkInt(value: Int) {
        try {
            val sdkIntField: Field = Build.VERSION::class.java.getDeclaredField("SDK_INT")
            sdkIntField.isAccessible = true

            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(sdkIntField, sdkIntField.modifiers and Modifier.FINAL.inv())

            sdkIntField.setInt(null, value)
        } catch (e: Exception) {
            throw RuntimeException("Failed to set SDK_INT via reflection", e)
        }
    }
}
