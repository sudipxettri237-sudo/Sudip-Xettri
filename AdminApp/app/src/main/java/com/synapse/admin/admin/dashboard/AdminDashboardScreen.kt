package com.example.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.data.AdminRepository
import com.example.common.models.AdminLog
import com.example.common.models.DashboardStats
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier
) {
    var stats by remember { mutableStateOf(DashboardStats()) }
    var logs by remember { mutableStateOf<List<AdminLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        stats = adminRepository.getDashboardStats()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        adminRepository.observeAdminLogs().collect {
            logs = it
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "System Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Real-time metrics from the Synapse database cluster",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Metrics Grid Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Users",
                    value = "${stats.totalUsers}",
                    icon = Icons.Default.People,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Online Now",
                    value = "${stats.onlineUsers}",
                    icon = Icons.Default.Sensors,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Metrics Grid Row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "New Today",
                    value = "${stats.newUsersToday}",
                    icon = Icons.Default.PersonAdd,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Posts",
                    value = "${stats.totalPosts}",
                    icon = Icons.Default.DynamicFeed,
                    tint = Color(0xFFA78BFA),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Metrics Grid Row 3 (Moderation focus)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Pending Reports",
                    value = "${stats.pendingReports}",
                    icon = Icons.Default.WarningAmber,
                    tint = if (stats.pendingReports > 0) Color(0xFFEF4444) else Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Suspended / Banned",
                    value = "${stats.suspendedUsers + stats.bannedUsers}",
                    icon = Icons.Default.Block,
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Activity Bar Chart Visual
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Weekly Platform Activity", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    val days = listOf("Mon" to 45, "Tue" to 68, "Wed" to 82, "Thu" to 74, "Fri" to 95, "Sat" to 110, "Sun" to 88)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEach { (day, count) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.height(120.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((count * 0.9).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(day, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Recent Admin Actions Feed
        item {
            Text(
                text = "Recent Moderation Logs",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (logs.isEmpty()) {
            item {
                Text(
                    "No audit logs yet. Actions taken on users or posts will be logged here.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logs.take(5), key = { it.logId }) { log ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("admin_log_card_${log.logId}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            log.timestamp?.let { ts ->
                                val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts.toDate())
                                Text(
                                    "by ${log.adminName} • $dateStr",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
