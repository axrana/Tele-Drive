package com.example.teledrive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.teledrive.viewmodel.LoginViewModel
import com.example.teledrive.viewmodel.LoginUiState

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tele Drive", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        when (uiState) {
            is LoginUiState.WaitPhoneNumber -> {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (+91...)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Button(onClick = { viewModel.submitPhoneNumber(phone) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Send OTP")
                }
            }
            is LoginUiState.WaitCode -> {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(onClick = { viewModel.submitCode(code) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Verify")
                }
            }
            is LoginUiState.Loading -> {
                CircularProgressIndicator()
            }
            else -> {}
        }
    }
}
