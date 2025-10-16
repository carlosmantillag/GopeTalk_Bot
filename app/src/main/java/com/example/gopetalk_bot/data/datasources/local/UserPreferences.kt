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

    companion object {
        private const val KEY_USERNAME = "username"
    }
}
