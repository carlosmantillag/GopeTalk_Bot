package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences constructor(
    context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var username: String
        get() = sharedPreferences.getString(KEY_USERNAME, "usuario") ?: "usuario"
        set(value) = sharedPreferences.edit().putString(KEY_USERNAME, value).apply()

    var authToken: String?
        get() = sharedPreferences.getString(KEY_AUTH_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_AUTH_TOKEN, value).apply()

    fun hasActiveSession(): Boolean {
        return !authToken.isNullOrEmpty() && username.isNotEmpty()
    }

    fun clearSession() {
        sharedPreferences.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USERNAME)
            apply()
        }
    }

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
