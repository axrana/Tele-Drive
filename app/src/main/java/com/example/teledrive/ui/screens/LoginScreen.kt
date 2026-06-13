package com.example.teledrive.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teledrive.ui.theme.TeleBluePrimary
import com.example.teledrive.ui.theme.TeleBlueLight
import com.example.teledrive.viewmodel.LoginUiState
import com.example.teledrive.viewmodel.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TeleBluePrimary, TeleBlueLight, Color.White),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Logo area
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Tele Drive",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                "Your personal Telegram cloud storage",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = uiState,
                        label = "login_state",
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { state ->
                        when (state) {
                            is LoginUiState.WaitPhoneNumber -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Welcome Back",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Enter your phone to continue",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(32.dp))
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        label = { Text("Phone number") },
                                        placeholder = { Text("+1 234 567 8900") },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TeleBluePrimary) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.submitPhoneNumber(phone) },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is LoginUiState.WaitCode -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Verification",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Code sent to your Telegram app",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(32.dp))
                                    OutlinedTextField(
                                        value = code,
                                        onValueChange = { code = it },
                                        label = { Text("Enter OTP") },
                                        placeholder = { Text("12345") },
                                        leadingIcon = { Icon(Icons.Default.Dialpad, null, tint = TeleBluePrimary) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.submitCode(code) },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Verify", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    TextButton(onClick = { viewModel.submitPhoneNumber(phone) }) {
                                        Text("Resend code", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            is LoginUiState.WaitPassword -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Two-Step Verification",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Enter your 2FA password",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(32.dp))
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TeleBluePrimary) },
                                        modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.submitPassword(password) },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Confirm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is LoginUiState.Loading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    CircularProgressIndicator(color = TeleBluePrimary, strokeWidth = 3.dp)
                                    Spacer(Modifier.height(24.dp))
                                    Text(
                                        "Securing connection...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Secure • Private • Cloud",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
