package com.example.gopetalk_bot.presentation.main

import com.example.gopetalk_bot.domain.entities.PermissionStatus
import com.example.gopetalk_bot.domain.repositories.UserRepository
import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class MainPresenterTest {

    private lateinit var view: MainContract.View
    private lateinit var checkPermissionsUseCase: CheckPermissionsUseCase
    private lateinit var userRepository: UserRepository
    private lateinit var presenter: MainPresenter

    @Before
    fun setup() {
        view = mockk(relaxed = true)
        checkPermissionsUseCase = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)

        presenter = MainPresenter(
            view,
            checkPermissionsUseCase,
            userRepository
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `onViewCreated should start voice service when all permissions granted`() {
        val permissionStatus = PermissionStatus(
            allGranted = true,
            permissions = emptyList()
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus

        presenter.onViewCreated()

        verify { view.startVoiceService() }
        verify(exactly = 0) { view.requestPermissions(any()) }
    }

    @Test
    fun `onViewCreated should request permissions when not all granted`() {
        val permissions = listOf("android.permission.RECORD_AUDIO")
        val permissionStatus = PermissionStatus(
            allGranted = false,
            permissions = permissions
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus

        presenter.onViewCreated()

        verify { view.requestPermissions(permissions.toTypedArray()) }
        verify(exactly = 0) { view.startVoiceService() }
    }

    @Test
    fun `onPermissionsResult should start voice service when all granted`() {
        presenter.onPermissionsResult(allGranted = true)

        verify { view.startVoiceService() }
        verify(exactly = 0) { view.showPermissionsRequiredError() }
    }

    @Test
    fun `onPermissionsResult should show error when not all granted`() {
        presenter.onPermissionsResult(allGranted = false)

        verify { view.showPermissionsRequiredError() }
        verify(exactly = 0) { view.startVoiceService() }
    }
}
