package com.example.admin.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.data.AdminRepository
import com.example.common.models.Report
import com.example.common.models.UserProfile
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReportManagementScreen(
    currentAdmin: UserProfile,
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var selectedStatus by remember { mutableStateOf("pending") }
    var activeActionReport by remember { mutableStateOf<Report?>(null) }
    var moderatorNotes by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("resolved") }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshReports() {
        coroutineScope.launch {
            isLoading = true
            reports = adminRepository.getReports(selectedStatus)
            isLoading = false
        }
    }

    LaunchedEffect(selectedStatus) {
        refreshReports()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Moderation Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        // Status Tabs
        TabRow(
            selectedTabIndex = when (selectedStatus) {
                "pending" -> 0
                "reviewed" -> 1
                "resolved" -> 2
                else -> 3
            }
        ) {
            Tab(selected = selectedStatus == "pending", onClick = { selectedStatus = "pending" }, text = { Text("Pending") })
            Tab(selected = selectedStatus == "reviewed", onClick = { selectedStatus = "reviewed" }, text = { Text("Reviewed") })
            Tab(selected = selectedStatus == "resolved", onClick = { selectedStatus = "resolved" }, text = { Text("Resolved") })
            Tab(selected = selectedStatus == "rejected", onClick = { selectedStatus = "rejected" }, text = { Text("Rejected") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF10B981))
            }
        } else if (reports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No $selectedStatus reports.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(reports, key = { it.reportId }) { report ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("report_item_${report.reportId}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reported ${report.targetType.uppercase()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = report.reason,
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            if (report.targetSummary.isNotBlank()) {
                                Text("Content: \"${report.targetSummary}\"", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("Reporter: ${report.reporterName} (ID: ${report.reporterId.take(8)})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            report.createdAt?.let { ts ->
                                val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(ts.toDate())
                                Text("Submitted: $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }

                            if (report.moderatorNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Mod Notes: ${report.moderatorNotes}", fontSize = 12.sp, color = Color(0xFF10B981))
                            }

                            if (report.status == "pending" || report.status == "reviewed") {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            activeActionReport = report
                                            actionType = "rejected"
                                            moderatorNotes = ""
                                        }
                                    ) {
                                        Text("Reject")
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = {
                                            activeActionReport = report
                                            actionType = "resolved"
                                            moderatorNotes = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Resolve")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Action Confirmation Dialog with Notes
    activeActionReport?.let { report ->
        AlertDialog(
            onDismissRequest = { activeActionReport = null },
            title = { Text("Complete Moderation ($actionType)") },
            text = {
                Column {
                    Text("Add internal notes regarding resolution of this report:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = moderatorNotes,
                        onValueChange = { moderatorNotes = it },
                        placeholder = { Text("e.g. Warning issued to user, content verified compliant...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            adminRepository.updateReport(
                                reportId = report.reportId,
                                newStatus = actionType,
                                notes = moderatorNotes,
                                adminUid = currentAdmin.uid,
                                adminName = currentAdmin.displayName
                            )
                            activeActionReport = null
                            refreshReports()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "resolved") Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeActionReport = null }) { Text("Cancel") }
            }
        )
    }
}
