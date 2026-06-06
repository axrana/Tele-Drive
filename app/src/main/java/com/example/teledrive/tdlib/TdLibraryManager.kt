package com.example.teledrive.tdlib

import android.content.Context
import com.example.teledrive.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

class TdLibraryManager(private val context: Context) {
    private var client: Client? = null

    private val _authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState: StateFlow<TdApi.AuthorizationState?> = _authorizationState.asStateFlow()

    private val _fileUpdates = MutableSharedFlow<TdApi.File>(extraBufferCapacity = 64)
    val fileUpdates: SharedFlow<TdApi.File> = _fileUpdates.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        initializeClient()
    }

    private fun initializeClient() {
        client = Client.create({ objectResponse ->
            when (objectResponse) {
                is TdApi.UpdateAuthorizationState -> {
                    _authorizationState.value = objectResponse.authorizationState
                    handleAuthorizationState(objectResponse.authorizationState)
                }
                is TdApi.UpdateFile -> {
                    scope.launch { _fileUpdates.emit(objectResponse.file) }
                }
            }
        }, { error ->
            // Update exception handler
        }, { error ->
            // Default exception handler
        })
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val parameters = TdApi.TdlibParameters().apply {
                    apiId = context.getString(R.string.telegram_api_id).toInt()
                    apiHash = context.getString(R.string.telegram_api_hash)
                    useTestDc = false
                    databaseDirectory = File(context.filesDir, "tdlib").absolutePath
                    filesDirectory = File(context.filesDir, "tdlib_files").absolutePath
                    useFileDatabase = true
                    useChatInfoDatabase = true
                    useMessageDatabase = true
                    systemLanguageCode = "en"
                    deviceModel = "Android"
                    systemVersion = "TeleDrive"
                    applicationVersion = "1.0"
                }
                send(TdApi.SetTdlibParameters(parameters))
            }
            is TdApi.AuthorizationStateWaitEncryptionKey -> {
                send(TdApi.CheckDatabaseEncryptionKey(byteArrayOf()))
            }
            else -> {}
        }
    }

    fun send(query: TdApi.Function<*>, callback: (TdApi.Object) -> Unit = {}) {
        client?.send(query) { result ->
            callback(result)
        }
    }

    suspend fun <T : TdApi.Object> execute(query: TdApi.Function<T>): T = suspendCancellableCoroutine { continuation ->
        client?.send(query) { result ->
            if (result is TdApi.Error) {
                continuation.resumeWith(Result.failure(Exception("${result.code}: ${result.message}")))
            } else {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(result as T)
            }
        }
    }

    fun setPhoneNumber(phoneNumber: String) {
        send(TdApi.SetAuthenticationPhoneNumber(phoneNumber))
    }

    fun checkCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code))
    }

    fun logOut() {
        send(TdApi.LogOut())
    }
}
