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

    private val _errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val errorFlow: SharedFlow<String> = _errorFlow.asSharedFlow()

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
            scope.launch { _errorFlow.emit("TDLib update error: ${error.message}") }
        }, { error ->
            scope.launch { _errorFlow.emit("TDLib general error: ${error.message}") }
        })
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val parameters = TdApi.TdlibParameters()
                val apiIdString = context.getString(R.string.telegram_api_id)
                parameters.apiId = try {
                    apiIdString.toInt()
                } catch (e: NumberFormatException) {
                    // Fallback or handle placeholder state
                    0
                }
                parameters.apiHash = context.getString(R.string.telegram_api_hash)
                parameters.useTestDc = false
                parameters.databaseDirectory = File(context.filesDir, "tdlib").absolutePath
                parameters.filesDirectory = File(context.filesDir, "tdlib_files").absolutePath
                parameters.useFileDatabase = true
                parameters.useChatInfoDatabase = true
                parameters.useMessageDatabase = true
                parameters.systemLanguageCode = "en"
                parameters.deviceModel = "Android"
                parameters.systemVersion = "TeleDrive"
                parameters.applicationVersion = "1.0"

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
        try {
            client?.send(query) { result ->
                if (result is TdApi.Error) {
                    continuation.resumeWith(Result.failure(Exception("${result.code}: ${result.message}")))
                } else {
                    @Suppress("UNCHECKED_CAST")
                    continuation.resume(result as T)
                }
            }
        } catch (e: Exception) {
            continuation.resumeWith(Result.failure(e))
        }
    }

    fun setPhoneNumber(phoneNumber: String) {
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        settings.allowFlashCall = false
        settings.allowSmsRetrieverApi = false
        send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings))
    }

    fun checkCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code))
    }

    fun logOut() {
        send(TdApi.LogOut())
    }
}
