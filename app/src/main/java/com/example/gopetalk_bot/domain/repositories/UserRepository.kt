package com.example.gopetalk_bot.domain.repositories

interface UserRepository {
    fun getUsername(): String
    fun setUsername(username: String)
}