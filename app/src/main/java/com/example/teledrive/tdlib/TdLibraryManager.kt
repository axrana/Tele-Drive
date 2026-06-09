package com.example.teledrive.tdlib

import android.content.Context
import com.example.teledrive.R
import com.example.teledrive.util.TeleDriveLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File

class TdLibraryManager(private val context: Context) {
    private var client: Client? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState = _authorizationState.asStateFlow()

    init { initializeClient() }

    @Synchronized
    private fun initializeClient() {
        if (client != null) return
        client = Client.create({ obj ->
            if (obj is TdApi.UpdateAuthorizationState) {
                _authorizationState.value = obj.authorizationState
                handleAuthorizationState(obj.authorizationState)
            }
        }, null, null)
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        if (state is TdApi.AuthorizationStateWaitTdlibParameters) {
            val params = TdApi.TdlibParameters().apply {
                apiId = context.getString(R.string.telegram_api_id).toInt()
                apiHash = context.getString(R.string.telegram_api_hash)
                useTestDc = false
                databaseDirectory = File(context.filesDir, "tdlib").absolutePath
                filesDirectory = File(context.filesDir, "tdlib_files").absolutePath
                useFileDatabase = true
                useChatInfoDatabase = true
                useMessageDatabase = true
                useSecretChats = false
                systemLanguageCode = "en"
                deviceModel = "Android"
                systemVersion = "1.0"
                applicationVersion = "1.0"
                enableStorageOptimizer = true
                ignoreFileNames = false
            }
            send(TdApi.SetTdlibParameters(params))
        }
    }

    fun send(query: TdApi.Function, callback: (TdApi.Object) -> Unit = {}) {
        client?.send(query, callback)
    }

    suspend fun <T : TdApi.Object> execute(query: TdApi.Function): T = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { }
        client?.send(query) { result ->
            if (result is TdApi.Error) {
                cont.resumeWith(Result.failure(Exception(result.message)))
            } else {
                @Suppress("UNCHECKED_CAST")
                cont.resumeWith(Result.success(result as T))
            }
        }
    }

    fun submitPhoneNumber(phone: String) {
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        send(TdApi.SetAuthenticationPhoneNumber(phone, settings))
    }

    fun submitCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code))
    }

    fun logOut() {
        send(TdApi.LogOut())
    }
}
