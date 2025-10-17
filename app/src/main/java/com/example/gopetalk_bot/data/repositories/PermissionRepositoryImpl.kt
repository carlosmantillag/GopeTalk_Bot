package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.PermissionDataSource
import com.example.gopetalk_bot.domain.entities.PermissionStatus
import com.example.gopetalk_bot.domain.repositories.PermissionRepository

class PermissionRepositoryImpl(
    private val permissionDataSource: PermissionDataSource
) : PermissionRepository {

    override fun getRequiredPermissions(): List<String> {
        return permissionDataSource.getRequiredPermissions()
    }

    override fun areAllPermissionsGranted(): Boolean {
        return permissionDataSource.areAllPermissionsGranted()
    }

    override fun getPermissionStatus(): PermissionStatus {
        val permissions = getRequiredPermissions()
        val allGranted = areAllPermissionsGranted()
        return PermissionStatus(permissions, allGranted)
    }
}
