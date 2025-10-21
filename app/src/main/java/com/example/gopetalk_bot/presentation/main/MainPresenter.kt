package com.example.gopetalk_bot.presentation.main

import com.example.gopetalk_bot.domain.repositories.UserRepository
import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase

class MainPresenter(
    private val view: MainContract.View,
    private val checkPermissionsUseCase: CheckPermissionsUseCase,
    private val userRepository: UserRepository
) : MainContract.Presenter {

    override fun onViewCreated() {
        val permissionStatus = checkPermissionsUseCase.execute()
        
        if (permissionStatus.allGranted) {
            startServices()
        } else {
            view.requestPermissions(permissionStatus.permissions.toTypedArray())
        }
    }

    override fun onPermissionsResult(allGranted: Boolean) {
        if (allGranted) {
            startServices()
        } else {
            view.showPermissionsRequiredError()
        }
    }

    private fun startServices() {
        view.startVoiceService()
        
        // Obtener el nombre de usuario guardado y dar la bienvenida
        val username = userRepository.getUsername()
        view.speakWelcomeMessage(username)
    }
}
