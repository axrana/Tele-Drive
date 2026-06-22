package com.example.teledrive.tdlib

import android.content.Context
import com.example.teledrive.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TdLibraryManager(private val context: Context) {
    private var client: Client? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState = _authorizationState.asStateFlow()

    val errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val fileUpdates = MutableSharedFlow<TdApi.File>(extraBufferCapacity = 64)
    val updateFlow = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 100)

    init { initializeClient() }

    @Synchronized
private fun initializeClient() {
    if (client != null) return
    client = Client.create({ obj ->
        scope.launch { updateFlow.emit(obj) }
        if (obj is TdApi.UpdateAuthorizationState) {
            _authorizationState.value = obj.authorizationState
            handleAuthorizationState(obj.authorizationState)
            if (obj.authorizationState is TdApi.AuthorizationStateReady) {
                setOnline()
            }
        } else if (obj is TdApi.UpdateFile) {
            scope.launch { fileUpdates.emit(obj.file) }
        } else if (obj is TdApi.UpdateConnectionState) {
            // Connection state changed (e.g. reconnecting) - re-assert online status
            scope.launch { setOnline() }
        }
    }, null, null)
}

private fun setOnline() {
    val option = TdApi.SetOption()
    option.name = "online"
    val value = TdApi.OptionValueBoolean()
    value.value = true
    option.value = value
    send(option)
}

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        if (state is TdApi.AuthorizationStateWaitTdlibParameters) {
            val parameters = TdApi.SetTdlibParameters()
            parameters.useTestDc = false
            parameters.databaseDirectory = File(context.filesDir, "tdlib").absolutePath
            parameters.filesDirectory = File(context.filesDir, "tdlib_files").absolutePath
            parameters.useFileDatabase = true
            parameters.useChatInfoDatabase = true
            parameters.useMessageDatabase = true
            parameters.useSecretChats = false
            val rawApiId = context.getString(R.string.telegram_api_id)
            parameters.apiId = if (rawApiId == "YOURAPIIDHERE" || rawApiId == "0") 0 else rawApiId.toIntOrNull() ?: 0
            parameters.apiHash = context.getString(R.string.telegram_api_hash)
            parameters.systemLanguageCode = "en"
            parameters.deviceModel = "Android"
            parameters.systemVersion = "1.0"
            parameters.applicationVersion = "1.1"
            send(parameters)
        }
    }

    fun send(query: TdApi.Function<*>, callback: (TdApi.Object) -> Unit = {}) {
        client?.send(query, callback)
    }

    suspend fun <T : TdApi.Object> execute(query: TdApi.Function<T>): T = suspendCancellableCoroutine { cont ->
        client?.send(query) { result ->
            if (result is TdApi.Error) {
                scope.launch { errorFlow.emit(result.message) }
                cont.resumeWithException(Exception("${result.code}: ${result.message}"))
            } else {
                @Suppress("UNCHECKED_CAST")
                cont.resume(result as T)
            }
        } ?: cont.resumeWithException(Exception("TDLib client not initialized"))
    }

    fun submitPhoneNumber(phone: String) {
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        settings.allowFlashCall = false
        settings.allowSmsRetrieverApi = false

        val query = TdApi.SetAuthenticationPhoneNumber()
        query.phoneNumber = phone
        query.settings = settings
        send(query)
    }

    fun submitCode(code: String) {
        val query = TdApi.CheckAuthenticationCode()
        query.code = code
        send(query)
    }

    fun submitPassword(password: String) {
        val query = TdApi.CheckAuthenticationPassword()
        query.password = password
        send(query)
    }

    fun logOut() {
        send(TdApi.LogOut())
    }
}
