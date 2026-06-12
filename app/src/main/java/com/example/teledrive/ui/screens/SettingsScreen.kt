package com.example.teledrive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teledrive.data.local.entity.Settings
import com.example.teledrive.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    channelId: Long?,
    onSettingsChange: (Settings) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Profile section
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Tele Drive User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Channel ID: ${channelId ?: "Not set"}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        SuggestionChip(onClick = {}, label = { Text("Connected") }, border = null, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), labelColor = MaterialTheme.colorScheme.primary))
                    }
                }
            }

            SettingsSectionHeader("Appearance")
            SettingsSwitchItem(
                icon = Icons.Default.Palette,
                title = "Dark Mode",
                subtitle = "Use dark theme across the app",
                checked = settings.isDarkMode,
                onCheckedChange = { onSettingsChange(settings.copy(isDarkMode = it)) }
            )

            SettingsSectionHeader("Upload & Download")
            SettingsSwitchItem(
                icon = Icons.Default.Compress,
                title = "Compress Images",
                subtitle = "Save storage by compressing images",
                checked = settings.shouldCompress,
                onCheckedChange = { onSettingsChange(settings.copy(shouldCompress = it)) }
            )

            SettingsSectionHeader("Account")
            ListItem(
                headlineContent = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Log out from your Telegram account") },
                leadingContent = {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(12.dp)).clickable { showLogoutDialog = true }
            )

            SettingsSectionHeader("About")
            SettingsInfoItem(icon = Icons.Default.Info, title = "App Version", value = "1.2.0")
            SettingsInfoItem(icon = Icons.Default.Storage, title = "Storage Provider", value = "Telegram Cloud")

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? You will need to log in again.") },
            confirmButton = {
                Button(onClick = { showLogoutDialog = false; onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Sign Out")
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
fun SettingsSwitchItem(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun SettingsInfoItem(icon: ImageVector, title: String, value: String) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}
