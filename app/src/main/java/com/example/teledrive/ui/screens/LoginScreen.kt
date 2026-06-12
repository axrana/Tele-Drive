package com.example.teledrive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teledrive.viewmodel.LoginViewModel
import com.example.teledrive.viewmodel.LoginUiState

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Tele Drive",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Your private Telegram-powered cloud",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (uiState) {
                        is LoginUiState.WaitPhoneNumber, is LoginUiState.Initial -> {
                            Text("Welcome Back", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Enter your phone number to start", style = MaterialTheme.typography.bodySmall)

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Button(
                                onClick = { viewModel.submitPhoneNumber(phoneNumber) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Send Code")
                            }
                        }

                        is LoginUiState.WaitCode -> {
                            Text("Verification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Enter the code sent via Telegram", style = MaterialTheme.typography.bodySmall)

                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("Code") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Button(
                                onClick = { viewModel.submitCode(code) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Verify")
                            }
                        }

                        is LoginUiState.WaitPassword -> {
                            Text("Two-Step Verification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Enter your cloud password", style = MaterialTheme.typography.bodySmall)

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Button(
                                onClick = { viewModel.submitPassword(password) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Login")
                            }
                        }

                        is LoginUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is LoginUiState.LoggedIn -> {
                             // Will be navigated away
                        }

                        is LoginUiState.Error -> {
                            Text("Error", color = MaterialTheme.colorScheme.error)
                            Text((uiState as LoginUiState.Error).message)
                            Button(onClick = { viewModel.submitPhoneNumber(phoneNumber) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
