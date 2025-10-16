package com.example.gopetalk_bot.presentation.main

import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase

/**
 * Presenter for Main screen following MVP + Clean Architecture
 */
class MainPresenter(
    private val view: MainContract.View,
    private val checkPermissionsUseCase: CheckPermissionsUseCase,
    private val userRepository: com.example.gopetalk_bot.data.repositories.UserRepository
) : MainContract.Presenter {

    override fun onViewCreated() {
        // Speak welcome message with username
        val username = userRepository.getUsername()
        view.speakWelcomeMessage(username)
        
        // Check and request permissions
        val permissionStatus = checkPermissionsUseCase.execute()
        
        if (permissionStatus.allGranted) {
            view.startVoiceService()
        } else {
            view.requestPermissions(permissionStatus.permissions.toTypedArray())
        }
    }

    override fun onPermissionsResult(allGranted: Boolean) {
        if (allGranted) {
            view.startVoiceService()
        } else {
            view.showPermissionsRequiredError()
        }
    }
}
