package com.example.gopetalk_bot.presentation.main

import com.example.gopetalk_bot.domain.entities.PermissionStatus
import com.example.gopetalk_bot.domain.repositories.UserRepository
import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class MainPresenterTest {

    private lateinit var presenter: MainPresenter
    private lateinit var view: MainContract.View
    private lateinit var checkPermissionsUseCase: CheckPermissionsUseCase
    private lateinit var userRepository: UserRepository

    @Before
    fun setup() {
        view = mockk(relaxed = true)
        checkPermissionsUseCase = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)

        presenter = MainPresenter(
            view = view,
            checkPermissionsUseCase = checkPermissionsUseCase,
            userRepository = userRepository
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onViewCreated should start services when all permissions granted`() {
        val permissionStatus = PermissionStatus(
            allGranted = true,
            permissions = emptyList()
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus
        every { userRepository.getUsername() } returns "TestUser"

        presenter.onViewCreated()

        verify { view.startVoiceService() }
        verify { view.speakWelcomeMessage("TestUser") }
        verify(exactly = 0) { view.requestPermissions(any()) }
    }

    @Test
    fun `onViewCreated should request permissions when not all granted`() {
        val permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.INTERNET")
        val permissionStatus = PermissionStatus(
            allGranted = false,
            permissions = permissions
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus

        presenter.onViewCreated()

        verify { view.requestPermissions(permissions.toTypedArray()) }
        verify(exactly = 0) { view.startVoiceService() }
        verify(exactly = 0) { view.speakWelcomeMessage(any()) }
    }

    @Test
    fun `onPermissionsResult should start services when all granted`() {
        every { userRepository.getUsername() } returns "JohnDoe"

        presenter.onPermissionsResult(allGranted = true)

        verify { view.startVoiceService() }
        verify { view.speakWelcomeMessage("JohnDoe") }
    }

    @Test
    fun `onPermissionsResult should show error when not all granted`() {
        presenter.onPermissionsResult(allGranted = false)

        verify { view.showPermissionsRequiredError() }
        verify(exactly = 0) { view.startVoiceService() }
        verify(exactly = 0) { view.speakWelcomeMessage(any()) }
    }

    @Test
    fun `startServices should get username from repository`() {
        val permissionStatus = PermissionStatus(
            allGranted = true,
            permissions = emptyList()
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus
        every { userRepository.getUsername() } returns "Alice"

        presenter.onViewCreated()

        verify { userRepository.getUsername() }
        verify { view.speakWelcomeMessage("Alice") }
    }

    @Test
    fun `startServices should handle empty username`() {
        val permissionStatus = PermissionStatus(
            allGranted = true,
            permissions = emptyList()
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus
        every { userRepository.getUsername() } returns ""

        presenter.onViewCreated()

        verify { view.speakWelcomeMessage("") }
    }

    @Test
    fun `onViewCreated should check permissions first`() {
        val permissionStatus = PermissionStatus(
            allGranted = true,
            permissions = emptyList()
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus
        every { userRepository.getUsername() } returns "TestUser"

        presenter.onViewCreated()

        verifyOrder {
            checkPermissionsUseCase.execute()
            view.startVoiceService()
            userRepository.getUsername()
            view.speakWelcomeMessage("TestUser")
        }
    }

    @Test
    fun `onPermissionsResult with true should call startServices`() {
        every { userRepository.getUsername() } returns "Bob"

        presenter.onPermissionsResult(allGranted = true)

        verify { view.startVoiceService() }
        verify { userRepository.getUsername() }
    }

    @Test
    fun `onPermissionsResult with false should not call startServices`() {
        presenter.onPermissionsResult(allGranted = false)

        verify(exactly = 0) { view.startVoiceService() }
        verify(exactly = 0) { userRepository.getUsername() }
    }

    @Test
    fun `multiple onViewCreated calls should work correctly`() {
        val permissionStatus = PermissionStatus(
            allGranted = true,
            permissions = emptyList()
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus
        every { userRepository.getUsername() } returns "User1"

        presenter.onViewCreated()
        presenter.onViewCreated()

        verify(exactly = 2) { checkPermissionsUseCase.execute() }
        verify(exactly = 2) { view.startVoiceService() }
        verify(exactly = 2) { view.speakWelcomeMessage("User1") }
    }
}
