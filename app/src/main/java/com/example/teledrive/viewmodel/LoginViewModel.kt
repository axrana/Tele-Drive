package com.example.teledrive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teledrive.data.local.entity.UserSession
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.td.libcore.telegram.TdApi

class LoginViewModel(
    private val tdLibraryManager: TdLibraryManager,
    private val repository: TeleDriveRepository
) : ViewModel() {

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var phoneNumber: String = ""

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

    fun submitPhoneNumber(phone: String) {
        _uiState.value = LoginUiState.Loading
        this.phoneNumber = phone
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        settings.allowFlashCall = false
        settings.allowSmsRetrieverApi = false
        tdLibraryManager.send(TdApi.SetAuthenticationPhoneNumber(phone, settings)) { result ->
            if (result is TdApi.Error) {
                _uiState.value = LoginUiState.WaitPhoneNumber
                viewModelScope.launch { _errorFlow.emit("Failed to send code: ${result.message}") }
            }
        }
    }

    fun submitCode(code: String) {
        _uiState.value = LoginUiState.Loading
        tdLibraryManager.send(TdApi.CheckAuthenticationCode(code)) { result ->
            if (result is TdApi.Error) {
                _uiState.value = LoginUiState.WaitCode
                viewModelScope.launch { _errorFlow.emit("Invalid code: ${result.message}") }
            }
        }
    }

    private suspend fun handleLoginSuccess() {
        val me = tdLibraryManager.execute(TdApi.GetMe())
        val localSession = repository.getUserSession().firstOrNull()

        if (localSession == null) {
            val chatsResponse = tdLibraryManager.execute(TdApi.GetChats(TdApi.ChatListMain(), 100))
            var existingChannelId: Long? = null

            val chatIds = chatsResponse.chatIds
            if (chatIds != null) {
                for (id in chatIds) {
                    try {
                        val chat = tdLibraryManager.execute(TdApi.GetChat(id))
                        if (chat.title != null && chat.title.startsWith("My Cloud Storage_")) {
                            existingChannelId = chat.id
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val channelId = if (existingChannelId != null) {
                existingChannelId
            } else {
                val randomDigits = (100000..999999).random()
                val channelTitle = "My Cloud Storage_$randomDigits"
                val chat = tdLibraryManager.execute(TdApi.CreateNewSupergroupChat(channelTitle, true, "Storage for TeleDrive"))
                // Try to toggle forum, but ignore errors as it might not be supported on all accounts immediately
                try {
                    tdLibraryManager.execute(TdApi.ToggleSupergroupIsForum(chat.id, true))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                chat.id
            }

            val newSession = UserSession(
                telegramUserId = me.id,
                phoneNumber = phoneNumber,
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
