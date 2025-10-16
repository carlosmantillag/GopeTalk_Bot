package com.example.gopetalk_bot.presentation.main

import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase

/**
 * Presenter for Main screen following MVP + Clean Architecture
 */
class MainPresenter(
    private val view: MainContract.View,
    private val checkPermissionsUseCase: CheckPermissionsUseCase
) : MainContract.Presenter {

    override fun onViewCreated() {
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
