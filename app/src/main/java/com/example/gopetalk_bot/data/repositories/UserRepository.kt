package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.UserPreferences

interface UserRepository {
    fun getUsername(): String
    fun setUsername(username: String)
}

class UserRepositoryImpl constructor(
    private val userPreferences: UserPreferences
) : UserRepository {
    
    override fun getUsername(): String = userPreferences.username
    
    override fun setUsername(username: String) {
        userPreferences.username = username
    }
}
