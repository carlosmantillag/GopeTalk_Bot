package com.example.gopetalk_bot.data.datasources.local

import android.content.Context

interface PermissionChecker {
    fun checkPermission(context: Context, permission: String): Int
    fun getSdkVersion(): Int
}
