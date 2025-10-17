package com.example.gopetalk_bot.domain.repositories

import com.example.gopetalk_bot.domain.entities.PermissionStatus

interface PermissionRepository {
    fun getRequiredPermissions(): List<String>
    fun areAllPermissionsGranted(): Boolean
    fun getPermissionStatus(): PermissionStatus
}
