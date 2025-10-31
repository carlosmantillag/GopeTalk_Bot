package com.example.gopetalk_bot.data.datasources.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class PermissionDataSource(
    private val context: Context,
    private val permissionChecker: PermissionChecker = AndroidPermissionChecker()
) {
    fun getRequiredPermissions(): List<String> {
        return mutableListOf(
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (permissionChecker.getSdkVersion() >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun areAllPermissionsGranted(): Boolean {
        return getRequiredPermissions().all {
            permissionChecker.checkPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
