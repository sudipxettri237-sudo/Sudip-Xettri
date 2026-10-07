package com.example.admin.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.data.AdminRepository
import com.example.common.models.AppSetting
import com.example.common.models.UserProfile
import kotlinx.coroutines.launch

@Composable
fun AdminSettingsScreen(
    currentAdmin: UserProfile,
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var appSetting by remember { mutableStateOf(AppSetting()) }
    var isSaving by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        appSetting = adminRepository.getAppSettings()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Global Platform Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Configure real-time service gates and operational modes for all connected apps.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingToggleCard(
            title = "Maintenance Mode",
            description = "Temporarily close the user platform for urgent maintenance.",
            icon = Icons.Default.Build,
            checked = appSetting.maintenanceMode,
            onCheckedChange = { appSetting = appSetting.copy(maintenanceMode = it) }
        )

        SettingToggleCard(
            title = "Allow User Registrations",
            description = "Permit new accounts to sign up via Google Identity.",
            icon = Icons.Default.PersonAdd,
            checked = appSetting.registrationEnabled,
            onCheckedChange = { appSetting = appSetting.copy(registrationEnabled = it) }
        )

        SettingToggleCard(
            title = "Allow New Posts",
            description = "Users can submit feed publications and media.",
            icon = Icons.Default.Edit,
            checked = appSetting.postingEnabled,
            onCheckedChange = { appSetting = appSetting.copy(postingEnabled = it) }
        )

        SettingToggleCard(
            title = "Allow Direct Messaging",
            description = "Real-time chat functionality across user accounts.",
            icon = Icons.Default.Chat,
            checked = appSetting.messagingEnabled,
            onCheckedChange = { appSetting = appSetting.copy(messagingEnabled = it) }
        )

        SettingToggleCard(
            title = "Active Content Moderation",
            description = "Automatic flagging and community report routing.",
            icon = Icons.Default.Shield,
            checked = appSetting.moderationEnabled,
            onCheckedChange = { appSetting = appSetting.copy(moderationEnabled = it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isSaving = true
                    val res = adminRepository.updateAppSettings(
                        setting = appSetting,
                        adminUid = currentAdmin.uid,
                        adminName = currentAdmin.displayName
                    )
                    isSaving = false
                    res.fold(
                        onSuccess = { saveStatus = "Settings updated in Firestore!" },
                        onFailure = { saveStatus = "Error: ${it.localizedMessage}" }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_admin_settings_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            enabled = !isSaving,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Save Global Settings", fontWeight = FontWeight.Bold)
        }

        saveStatus?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(it, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SettingToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
