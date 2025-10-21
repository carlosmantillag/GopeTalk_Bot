package com.example.gopetalk_bot.domain.entities

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionStatusTest {

    @Test
    fun `PermissionStatus should contain correct properties`() {
        val permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.INTERNET")
        val status = PermissionStatus(
            permissions = permissions,
            allGranted = true
        )

        assertThat(status.permissions).isEqualTo(permissions)
        assertThat(status.allGranted).isTrue()
    }

    @Test
    fun `PermissionStatus with all granted true should work`() {
        val status = PermissionStatus(
            permissions = listOf("android.permission.RECORD_AUDIO"),
            allGranted = true
        )

        assertThat(status.allGranted).isTrue()
    }

    @Test
    fun `PermissionStatus with all granted false should work`() {
        val status = PermissionStatus(
            permissions = listOf("android.permission.RECORD_AUDIO"),
            allGranted = false
        )

        assertThat(status.allGranted).isFalse()
    }

    @Test
    fun `PermissionStatus with empty permissions list should work`() {
        val status = PermissionStatus(
            permissions = emptyList(),
            allGranted = true
        )

        assertThat(status.permissions).isEmpty()
        assertThat(status.allGranted).isTrue()
    }

    @Test
    fun `PermissionStatus with multiple permissions should work`() {
        val permissions = listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        )
        val status = PermissionStatus(permissions, false)

        assertThat(status.permissions).hasSize(3)
        assertThat(status.permissions).containsExactlyElementsIn(permissions)
    }

    @Test
    fun `PermissionStatus with same data should be equal`() {
        val permissions = listOf("android.permission.RECORD_AUDIO")
        val status1 = PermissionStatus(permissions, true)
        val status2 = PermissionStatus(permissions, true)

        assertThat(status1).isEqualTo(status2)
    }

    @Test
    fun `PermissionStatus with different allGranted should not be equal`() {
        val permissions = listOf("android.permission.RECORD_AUDIO")
        val status1 = PermissionStatus(permissions, true)
        val status2 = PermissionStatus(permissions, false)

        assertThat(status1).isNotEqualTo(status2)
    }

    @Test
    fun `PermissionStatus with different permissions should not be equal`() {
        val status1 = PermissionStatus(listOf("android.permission.RECORD_AUDIO"), true)
        val status2 = PermissionStatus(listOf("android.permission.INTERNET"), true)

        assertThat(status1).isNotEqualTo(status2)
    }

    @Test
    fun `PermissionStatus copy should create new instance`() {
        val original = PermissionStatus(listOf("android.permission.RECORD_AUDIO"), false)
        val copied = original.copy(allGranted = true)

        assertThat(copied.allGranted).isTrue()
        assertThat(original.allGranted).isFalse()
        assertThat(copied.permissions).isEqualTo(original.permissions)
    }

    @Test
    fun `PermissionStatus toString should contain data`() {
        val status = PermissionStatus(listOf("android.permission.RECORD_AUDIO"), true)
        val string = status.toString()

        assertThat(string).contains("RECORD_AUDIO")
        assertThat(string).contains("true")
    }
}
