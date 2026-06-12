package com.example.teledrive.ui.screens

import androidx.compose.foundation.background
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
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Profile card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = TeleBlueContainer)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(TeleBluePrimary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Tele Drive User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TeleBlueDark)
                        Spacer(Modifier.height(2.dp))
                        Text("Channel ID: ${channelId ?: "Not set"}", style = MaterialTheme.typography.bodySmall, color = TeleBlueDark.copy(alpha = 0.7f))
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = TeleBluePrimary.copy(alpha = 0.15f)) {
                            Text("Connected", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = TeleBluePrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Preferences section
            SettingsSectionHeader("Preferences")
            SettingsSwitchItem(
                icon = Icons.Default.Compress,
                iconTint = ColorDoc,
                title = "Compress Images",
                subtitle = "Resize to 1024px, 85% quality before upload",
                checked = settings.shouldCompress,
                onCheckedChange = { onSettingsChange(settings.copy(shouldCompress = it)) }
            )
            SettingsDivider()
            SettingsSwitchItem(
                icon = Icons.Default.DarkMode,
                iconTint = Color(0xFF5856D6),
                title = "Dark Mode",
                subtitle = "Switch to dark theme",
                checked = settings.isDarkMode,
                onCheckedChange = { onSettingsChange(settings.copy(isDarkMode = it)) }
            )
            SettingsDivider()
            SettingsSwitchItem(
                icon = Icons.Default.CloudSync,
                iconTint = TeleSuccess,
                title = "Auto Upload",
                subtitle = "Coming soon",
                checked = false,
                onCheckedChange = {},
                enabled = false
            )

            // About section
            SettingsSectionHeader("About")
            SettingsInfoItem(icon = Icons.Default.Info, iconTint = TeleBlueLight, title = "App Version", value = "1.1.0")
            SettingsDivider()
            SettingsInfoItem(icon = Icons.Default.Storage, iconTint = ColorArchive, title = "Storage Limit", value = "50 GB (Telegram)")
            SettingsDivider()
            SettingsInfoItem(icon = Icons.Default.Security, iconTint = TeleSuccess, title = "Encryption", value = "End-to-end (Telegram)")

            Spacer(Modifier.height(24.dp))

            // Logout button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Sign Out?") },
            text = { Text("You will need to log in again with your Telegram account.") },
            confirmButton = {
                Button(onClick = { showLogoutDialog = false; onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Sign Out")
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = TeleBluePrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
}

@Composable
fun SettingsSwitchItem(icon: ImageVector, iconTint: Color, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconTint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (enabled) iconTint else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TeleBluePrimary))
        }
    )
}

@Composable
fun SettingsInfoItem(icon: ImageVector, iconTint: Color, title: String, value: String) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconTint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
        },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}
