package com.example.teledrive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teledrive.data.local.entity.UserSession
import com.example.teledrive.data.repository.TeleDriveRepository
import com.example.teledrive.tdlib.TdLibraryManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    object WaitPhoneNumber : LoginUiState()
    object WaitCode : LoginUiState()
    object WaitPassword : LoginUiState()
    object LoggedIn : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

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
                    is TdApi.AuthorizationStateWaitPhoneNumber -> _uiState.value = LoginUiState.WaitPhoneNumber
                    is TdApi.AuthorizationStateWaitCode -> _uiState.value = LoginUiState.WaitCode
                    is TdApi.AuthorizationStateWaitPassword -> _uiState.value = LoginUiState.WaitPassword
                    is TdApi.AuthorizationStateReady -> handleLoginSuccess()
                    is TdApi.AuthorizationStateLoggingOut -> _uiState.value = LoginUiState.Loading
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

        val query = TdApi.SetAuthenticationPhoneNumber()
        query.phoneNumber = phone
        query.settings = settings

        tdLibraryManager.send(query) { result ->
            if (result is TdApi.Error) {
                _uiState.value = LoginUiState.WaitPhoneNumber
                viewModelScope.launch { _errorFlow.emit("Failed to send code: ${result.message}") }
            }
        }
    }

    fun submitCode(code: String) {
        _uiState.value = LoginUiState.Loading
        val query = TdApi.CheckAuthenticationCode()
        query.code = code
        tdLibraryManager.send(query) { result ->
            if (result is TdApi.Error) {
                _uiState.value = LoginUiState.WaitCode
                viewModelScope.launch { _errorFlow.emit("Invalid code: ${result.message}") }
            }
        }
    }

    fun submitPassword(password: String) {
        _uiState.value = LoginUiState.Loading
        val query = TdApi.CheckAuthenticationPassword()
        query.password = password
        tdLibraryManager.send(query) { result ->
            if (result is TdApi.Error) {
                _uiState.value = LoginUiState.WaitPassword
                viewModelScope.launch { _errorFlow.emit("Wrong password: ${result.message}") }
            }
        }
    }

    private suspend fun handleLoginSuccess() {
        try {
            val me = tdLibraryManager.execute(TdApi.GetMe())
            val localSession = repository.getUserSession().firstOrNull()

            if (localSession == null) {
                val getChatsQuery = TdApi.GetChats()
                getChatsQuery.chatList = TdApi.ChatListMain()
                getChatsQuery.limit = 100

                val chatsResponse = tdLibraryManager.execute(getChatsQuery)
                var existingChannelId: Long? = null
                val chatIds = chatsResponse.chatIds
                if (chatIds != null) {
                    for (id in chatIds) {
                        try {
                            val getChatQuery = TdApi.GetChat()
                            getChatQuery.chatId = id
                            val chat = tdLibraryManager.execute(getChatQuery)
                            if (chat.title != null && chat.title.startsWith("My Cloud Storage_")) {
                                existingChannelId = chat.id
                                break
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }

                val channelId = if (existingChannelId != null) {
                    existingChannelId
                } else {
                    val randomDigits = (100000..999999).random()
                    val createQuery = TdApi.CreateNewSupergroupChat()
                    createQuery.title = "My Cloud Storage_$randomDigits"
                    createQuery.isChannel = true
                    createQuery.description = "Storage for TeleDrive"

                    val chat = tdLibraryManager.execute(createQuery)
                    chat.id
                }

                repository.saveSession(
                    UserSession(
                        telegramUserId = me.id,
                        phoneNumber = phoneNumber,
                        username = me.usernames?.activeUsernames?.firstOrNull() ?: "",
                        firstName = me.firstName ?: "",
                        lastName = me.lastName ?: "",
                        channelId = channelId,
                        channelUsername = null,
                        isPremium = me.isPremium,
                        loginDate = System.currentTimeMillis()
                    )
                )
            }
            _uiState.value = LoginUiState.LoggedIn
        } catch (e: Exception) {
            _errorFlow.emit("Login failed: ${e.message}")
            _uiState.value = LoginUiState.WaitPhoneNumber
        }
    }
}
