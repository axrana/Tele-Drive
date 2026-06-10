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

    init { initializeClient() }

    @Synchronized
    private fun initializeClient() {
        if (client != null) return
        val updateHandler = Client.ResultHandler { obj ->
            if (obj is TdApi.UpdateAuthorizationState) {
                _authorizationState.value = obj.authorizationState
                handleAuthorizationState(obj.authorizationState)
            } else if (obj is TdApi.UpdateFile) {
                scope.launch { fileUpdates.emit(obj.file) }
            }
        }
        client = Client.create(updateHandler, null, null)
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        if (state is TdApi.AuthorizationStateWaitTdlibParameters) {
            val params = TdApi.SetTdlibParameters()
            params.useTestDc = false
            params.databaseDirectory = File(context.filesDir, "tdlib").absolutePath
            params.filesDirectory = File(context.filesDir, "tdlib_files").absolutePath
            params.databaseEncryptionKey = "".toByteArray()
            params.useFileDatabase = true
            params.useChatInfoDatabase = true
            params.useMessageDatabase = true
            params.useSecretChats = false
            params.apiId = context.getString(R.string.telegram_api_id).let { if (it == "YOURAPIIDHERE") 0 else it.toInt() }
            params.apiHash = context.getString(R.string.telegram_api_hash)
            params.systemLanguageCode = "en"
            params.deviceModel = "Android"
            params.systemVersion = "1.0"
            params.applicationVersion = "1.0"

            send(params)
        }
    }

    fun send(query: TdApi.Function<*>, callback: (TdApi.Object) -> Unit = {}) {
        client?.send(query, callback)
    }

    suspend fun <T : TdApi.Object> execute(query: TdApi.Function<T>): T = suspendCancellableCoroutine { cont ->
        client?.send(query) { result ->
            if (result is TdApi.Error) {
                cont.resumeWithException(Exception(result.message))
            } else {
                @Suppress("UNCHECKED_CAST")
                cont.resume(result as T)
            }
        }
    }

    fun submitPhoneNumber(phone: String) {
        val settings = TdApi.PhoneNumberAuthenticationSettings(false, false, false, false, false, null, null)
        send(TdApi.SetAuthenticationPhoneNumber(phone, settings))
    }

    fun submitCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code))
    }

    fun logOut() {
        send(TdApi.LogOut())
    }
}
