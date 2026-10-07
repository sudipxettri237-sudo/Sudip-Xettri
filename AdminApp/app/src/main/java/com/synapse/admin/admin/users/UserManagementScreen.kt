package com.example.admin.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.data.AdminRepository
import com.example.common.models.UserProfile
import com.example.common.models.UserRole
import com.example.common.ui.components.RoleBadge
import com.example.common.ui.components.UserAvatar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    currentAdmin: UserProfile,
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var roleFilter by remember { mutableStateOf("all") }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshUsers() {
        coroutineScope.launch {
            isLoading = true
            users = adminRepository.getAllUsers(searchQuery, statusFilter, roleFilter)
            isLoading = false
        }
    }

    LaunchedEffect(searchQuery, statusFilter, roleFilter) {
        refreshUsers()
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("User Management", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by username, email, UID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_search_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "active" to "Active", "suspended" to "Suspended", "banned" to "Banned").forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else if (users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users matched filters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users, key = { it.uid }) { user ->
                        val statusColor = when (user.status) {
                            "active" -> Color(0xFF10B981)
                            "suspended" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedUser = user }
                                .testTag("admin_user_item_${user.uid}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(photoUrl = user.photoURL, displayName = user.displayName, size = 44)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        RoleBadge(user.role)
                                    }
                                    Text("@${user.username} • ${user.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = user.status.uppercase(),
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // User Details & Moderation Sheet
    selectedUser?.let { user ->
        UserModerationSheet(
            user = user,
            currentAdmin = currentAdmin,
            onDismiss = { selectedUser = null },
            onUpdateStatus = { newStatus ->
                coroutineScope.launch {
                    adminRepository.updateUserStatus(
                        targetUid = user.uid,
                        newStatus = newStatus,
                        adminUid = currentAdmin.uid,
                        adminName = currentAdmin.displayName
                    )
                    selectedUser = null
                    refreshUsers()
                }
            },
            onUpdateRole = { newRole ->
                coroutineScope.launch {
                    adminRepository.updateUserRole(
                        targetUid = user.uid,
                        newRole = newRole,
                        adminUid = currentAdmin.uid,
                        adminName = currentAdmin.displayName
                    )
                    selectedUser = null
                    refreshUsers()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserModerationSheet(
    user: UserProfile,
    currentAdmin: UserProfile,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onUpdateRole: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(photoUrl = user.photoURL, displayName = user.displayName, size = 56)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(user.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("@${user.username} (${user.email})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("UID: ${user.uid}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timestamps
            user.createdAt?.let { ts ->
                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
                Text("Registered: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text("Moderation Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onUpdateStatus("active") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = user.status != "active"
                ) {
                    Text("Activate")
                }
                Button(
                    onClick = { onUpdateStatus("suspended") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    enabled = user.status != "suspended"
                ) {
                    Text("Suspend")
                }
                Button(
                    onClick = { onUpdateStatus("banned") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    enabled = user.status != "banned"
                ) {
                    Text("Ban")
                }
            }

            // Role assignment (Only Super Admin can change roles)
            if (currentAdmin.userRole == UserRole.ADMIN) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Role Assignment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onUpdateRole("user") },
                        enabled = user.role != "user"
                    ) {
                        Text("User")
                    }
                    OutlinedButton(
                        onClick = { onUpdateRole("moderator") },
                        enabled = user.role != "moderator"
                    ) {
                        Text("Moderator")
                    }
                    OutlinedButton(
                        onClick = { onUpdateRole("admin") },
                        enabled = user.role != "admin"
                    ) {
                        Text("Admin")
                    }
                }
            }
        }
    }
}
