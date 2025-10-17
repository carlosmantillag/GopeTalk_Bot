package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.PermissionStatus
import com.example.gopetalk_bot.domain.repositories.PermissionRepository

class CheckPermissionsUseCase(
    private val permissionRepository: PermissionRepository
) {
    fun execute(): PermissionStatus {
        return permissionRepository.getPermissionStatus()
    }
}
