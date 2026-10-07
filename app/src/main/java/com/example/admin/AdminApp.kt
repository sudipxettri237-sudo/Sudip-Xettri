package com.example.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.auth.AdminAuthGate
import com.example.admin.dashboard.AdminDashboardScreen
import com.example.admin.logs.AdminLogsScreen
import com.example.admin.notifications.AdminBroadcastScreen
import com.example.admin.posts.PostManagementScreen
import com.example.admin.reports.ReportManagementScreen
import com.example.admin.settings.AdminSettingsScreen
import com.example.admin.users.UserManagementScreen
import com.example.common.data.AdminRepository
import com.example.common.data.AuthRepository
import com.example.common.data.FirestoreProvider
import com.example.common.models.UserProfile
import com.example.common.ui.components.RoleBadge
import com.example.common.ui.components.UserAvatar
import kotlinx.coroutines.launch

enum class AdminSection {
    DASHBOARD,
    USERS,
    POSTS,
    REPORTS,
    ANNOUNCEMENTS,
    LOGS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApp(
    onSwitchToUserApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { FirestoreProvider.get(context) }
    val authRepository = remember { AuthRepository(db) }
    val adminRepository = remember { AdminRepository(db) }

    var verifiedAdminProfile by remember { mutableStateOf<UserProfile?>(null) }
    var currentSection by remember { mutableStateOf(AdminSection.DASHBOARD) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    if (verifiedAdminProfile == null) {
        AdminAuthGate(
            authRepository = authRepository,
            onAdminVerified = { profile ->
                verifiedAdminProfile = profile
            }
        )
        return
    }

    val admin = verifiedAdminProfile!!

    BackHandler(enabled = drawerState.isOpen || currentSection != AdminSection.DASHBOARD) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            currentSection = AdminSection.DASHBOARD
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                drawerContentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(photoUrl = admin.photoURL, displayName = admin.displayName, size = 48)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(admin.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                RoleBadge(admin.role)
                            }
                            Text("@${admin.username}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                }

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = currentSection == AdminSection.DASHBOARD,
                    onClick = {
                        currentSection = AdminSection.DASHBOARD
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_dashboard")
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("User Management") },
                    selected = currentSection == AdminSection.USERS,
                    onClick = {
                        currentSection = AdminSection.USERS
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_users")
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DynamicFeed, contentDescription = null) },
                    label = { Text("Post Moderation") },
                    selected = currentSection == AdminSection.POSTS,
                    onClick = {
                        currentSection = AdminSection.POSTS
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_posts")
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                    label = { Text("Moderation Reports") },
                    selected = currentSection == AdminSection.REPORTS,
                    onClick = {
                        currentSection = AdminSection.REPORTS
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_reports")
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    label = { Text("Broadcast Announcements") },
                    selected = currentSection == AdminSection.ANNOUNCEMENTS,
                    onClick = {
                        currentSection = AdminSection.ANNOUNCEMENTS
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_broadcasts")
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Audit Activity Logs") },
                    selected = currentSection == AdminSection.LOGS,
                    onClick = {
                        currentSection = AdminSection.LOGS
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_logs")
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Global App Settings") },
                    selected = currentSection == AdminSection.SETTINGS,
                    onClick = {
                        currentSection = AdminSection.SETTINGS
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_settings")
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(color = Color(0xFF334155))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    label = { Text("Open User Experience") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        onSwitchToUserApp()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_switch_user")
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFEF4444)) },
                    label = { Text("Admin Sign Out", color = Color(0xFFEF4444)) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            authRepository.signOut()
                            verifiedAdminProfile = null
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_sign_out")
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Synapse Admin", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            RoleBadge(admin.role)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("admin_menu_drawer_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Admin Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onSwitchToUserApp, modifier = Modifier.testTag("switch_to_user_app_icon")) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = "Switch to User App")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F172A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF090D16))
            ) {
                when (currentSection) {
                    AdminSection.DASHBOARD -> AdminDashboardScreen(adminRepository = adminRepository)
                    AdminSection.USERS -> UserManagementScreen(currentAdmin = admin, adminRepository = adminRepository)
                    AdminSection.POSTS -> PostManagementScreen(currentAdmin = admin, adminRepository = adminRepository)
                    AdminSection.REPORTS -> ReportManagementScreen(currentAdmin = admin, adminRepository = adminRepository)
                    AdminSection.ANNOUNCEMENTS -> AdminBroadcastScreen(currentAdmin = admin, adminRepository = adminRepository)
                    AdminSection.LOGS -> AdminLogsScreen(adminRepository = adminRepository)
                    AdminSection.SETTINGS -> AdminSettingsScreen(currentAdmin = admin, adminRepository = adminRepository)
                }
            }
        }
    }
}
