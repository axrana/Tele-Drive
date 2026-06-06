package com.example.teledrive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.teledrive.viewmodel.LoginUiState
import com.example.teledrive.viewmodel.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to Tele Drive", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        when (uiState) {
            is LoginUiState.Loading -> CircularProgressIndicator()
            is LoginUiState.WaitPhoneNumber -> {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Phone Number (+91 XXXXX-XXXXX)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.submitPhoneNumber(input) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Send Code")
                }
            }
            is LoginUiState.WaitCode -> {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("OTP Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.submitCode(input) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Login")
                }
            }
            is LoginUiState.LoggedIn -> {
                Text("Login Successful!")
            }
            else -> {}
        }
    }
}
