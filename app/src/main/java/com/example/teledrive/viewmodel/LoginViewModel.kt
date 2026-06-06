package com.example.teledrive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teledrive.data.local.entity.UserSession
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class LoginViewModel(
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : ViewModel() {

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tdLibraryManager.authorizationState.collect { state ->
                when (state) {
                    is TdApi.AuthorizationStateWaitPhoneNumber -> {
                        _uiState.value = LoginUiState.WaitPhoneNumber
                    }
                    is TdApi.AuthorizationStateWaitCode -> {
                        _uiState.value = LoginUiState.WaitCode
                    }
                    is TdApi.AuthorizationStateReady -> {
                        handleLoginSuccess()
                    }
                    is TdApi.AuthorizationStateLoggingOut -> {
                        _uiState.value = LoginUiState.Loading
                    }
                    else -> {}
                }
            }
        }
    }

    fun submitPhoneNumber(phoneNumber: String) {
        _uiState.value = LoginUiState.Loading
        try {
            tdLibraryManager.setPhoneNumber(phoneNumber)
        } catch (e: Exception) {
            _uiState.value = LoginUiState.WaitPhoneNumber
            viewModelScope.launch { _errorFlow.emit("Failed to send code: ${e.message}") }
        }
    }

    fun submitCode(code: String) {
        _uiState.value = LoginUiState.Loading
        try {
            tdLibraryManager.checkCode(code)
        } catch (e: Exception) {
            _uiState.value = LoginUiState.WaitCode
            viewModelScope.launch { _errorFlow.emit("Invalid code: ${e.message}") }
        }
    }

    private suspend fun handleLoginSuccess() {
        val me = tdLibraryManager.execute(TdApi.GetMe())
        val localSession = repository.getUserSession().firstOrNull()

        if (localSession == null) {
            val chats = tdLibraryManager.execute(TdApi.GetChats(100))
            var existingChannelId: Long? = null

            for (id in chats.chatIds) {
                val chat = tdLibraryManager.execute(TdApi.GetChat(id))
                if (chat.title.startsWith("My Cloud Storage_")) {
                    existingChannelId = chat.id
                    break
                }
            }

            val channelId = if (existingChannelId != null) {
                existingChannelId
            } else {
                val randomDigits = (100000..999999).random()
                val channelTitle = "My Cloud Storage_$randomDigits"
                val chat = tdLibraryManager.execute(TdApi.CreateNewSupergroupChat(channelTitle, true, "Storage for TeleDrive"))
                tdLibraryManager.execute(TdApi.ToggleSupergroupIsForum(chat.id, true))
                chat.id
            }

            val newSession = UserSession(
                telegramUserId = me.id,
                phoneNumber = "",
                username = me.username,
                firstName = me.firstName,
                lastName = me.lastName,
                channelId = channelId,
                channelUsername = null,
                isPremium = me.isPremium,
                loginDate = System.currentTimeMillis()
            )
            repository.saveSession(newSession)
        }
        _uiState.value = LoginUiState.LoggedIn
    }
}

sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    object WaitPhoneNumber : LoginUiState()
    object WaitCode : LoginUiState()
    object LoggedIn : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
