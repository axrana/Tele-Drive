package com.example.teledrive.tdlib

import android.content.Context
import com.example.teledrive.R
import com.example.teledrive.util.TeleDriveLogger
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

    @Synchronized
    private fun initializeClient() {
        if (client != null) return

        TeleDriveLogger.i("Initializing TDLib Client...")
        try {
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
                TeleDriveLogger.e("TDLib update error: ${error.message}")
                scope.launch { _errorFlow.emit("TDLib update error: ${error.message}") }
            }, { error ->
                TeleDriveLogger.e("TDLib general error: ${error.message}")
                scope.launch { _errorFlow.emit("TDLib general error: ${error.message}") }
            })
        } catch (e: Throwable) {
            TeleDriveLogger.e("Failed to create TDLib client", e)
            scope.launch { _errorFlow.emit("Critical: TDLib init failed") }
        }
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
    val apiIdString = context.getString(R.string.telegram_api_id)
    val apiId = try { apiIdString.toInt() } catch (e: Exception) { 0 }
    send(TdApi.SetTdlibParameters(
        false,
        File(context.filesDir, "tdlib").absolutePath,
        File(context.filesDir, "tdlib_files").absolutePath,
        null,
        true,
        true,
        true,
        false,
        apiId,
        context.getString(R.string.telegram_api_hash),
        "en",
        "Android",
        "TeleDrive",
        "1.0"
    ))
}
            }
            else -> {
                TeleDriveLogger.d("Auth state: ${state::class.java.simpleName}")
            }
        }
    }

    fun send(query: TdApi.Function<*>, callback: (TdApi.Object) -> Unit = {}) {
        val currentClient = client
        if (currentClient == null) {
            TeleDriveLogger.e("Attempted to send query while client is null: ${query.javaClass.simpleName}. Re-initializing...")
            initializeClient()
            client?.send(query) { result -> callback(result) }
            return
        }
        currentClient.send(query) { result ->
            callback(result)
        }
    }

    suspend fun <T : TdApi.Object> execute(query: TdApi.Function<T>): T = suspendCancellableCoroutine { continuation ->
        try {
            val currentClient = client
            if (currentClient == null) {
                initializeClient()
                val retryClient = client
                if (retryClient == null) {
                    continuation.resumeWith(Result.failure(IllegalStateException("Client is null and could not be initialized")))
                    return@suspendCancellableCoroutine
                }
                retryClient.send(query) { result ->
                    if (result is TdApi.Error) {
                        continuation.resumeWith(Result.failure(Exception("${result.code}: ${result.message}")))
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        continuation.resume(result as T)
                    }
                }
                return@suspendCancellableCoroutine
            }
            currentClient.send(query) { result ->
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
