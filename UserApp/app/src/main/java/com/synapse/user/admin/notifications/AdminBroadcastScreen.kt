package com.example.admin.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.data.AdminRepository
import com.example.common.models.UserProfile
import kotlinx.coroutines.launch

@Composable
fun AdminBroadcastScreen(
    currentAdmin: UserProfile,
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetUid by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val templates = listOf(
        "Scheduled Maintenance" to "Synapse will undergo scheduled system upgrades tonight between 02:00 - 03:00 UTC.",
        "Community Guidelines" to "A friendly reminder to all members to keep discourse respectful and supportive.",
        "New Features Live!" to "Explore updated stories and real-time direct messaging on Synapse."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("System Announcements", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Broadcast notifications to platform users or send targeted administrative notices.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Templates
        Text("Quick Templates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            templates.forEach { (tTitle, tBody) ->
                SuggestionChip(
                    onClick = {
                        title = tTitle
                        message = tBody
                    },
                    label = { Text(tTitle, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Announcement Title") },
            modifier = Modifier.fillMaxWidth().testTag("announcement_title_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Announcement Message Body") },
            modifier = Modifier.fillMaxWidth().height(120.dp).testTag("announcement_body_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = targetUid,
            onValueChange = { targetUid = it },
            label = { Text("Target User UID (Leave empty to broadcast to ALL users)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && message.isNotBlank()) {
                    coroutineScope.launch {
                        isSending = true
                        val res = adminRepository.sendAnnouncement(
                            title = title,
                            message = message,
                            targetUid = targetUid.ifBlank { null },
                            adminUid = currentAdmin.uid,
                            adminName = currentAdmin.displayName
                        )
                        isSending = false
                        res.fold(
                            onSuccess = {
                                statusMessage = "Announcement dispatched successfully!"
                                title = ""
                                message = ""
                                targetUid = ""
                            },
                            onFailure = {
                                statusMessage = "Failed: ${it.localizedMessage}"
                            }
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("send_announcement_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            enabled = !isSending && title.isNotBlank() && message.isNotBlank(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (targetUid.isBlank()) "Broadcast to All Users" else "Send Targeted Notice")
        }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(it, modifier = Modifier.padding(14.dp), fontSize = 13.sp)
            }
        }
    }
}
