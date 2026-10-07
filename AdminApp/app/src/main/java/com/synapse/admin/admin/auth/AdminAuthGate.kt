package com.example.admin.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.common.data.AuthRepository
import com.example.common.models.UserProfile
import com.example.common.models.UserRole
import kotlinx.coroutines.launch

@Composable
fun AdminAuthGate(
    authRepository: AuthRepository,
    onAdminVerified: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rejectedUser by remember { mutableStateOf<UserProfile?>(null) }

    val currentUser = authRepository.currentFirebaseUser

    LaunchedEffect(currentUser?.uid) {
        val user = currentUser
        if (user != null) {
            isLoading = true
            val profile = authRepository.ensureUserProfileCreated(user)
            isLoading = false
            if (profile.userRole == UserRole.ADMIN || profile.userRole == UserRole.MODERATOR) {
                rejectedUser = null
                onAdminVerified(profile)
            } else {
                rejectedUser = profile
            }
        } else {
            rejectedUser = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Admin Shield Icon
            Image(
                painter = painterResource(id = R.drawable.ic_synapse_admin_1791399776845),
                contentDescription = "Synapse Admin Control Panel",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Synapse Admin Panel",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Authorized Personnel Only • Zero Trust Moderation",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            if (rejectedUser != null) {
                // Rejected Normal User Screen
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = "Access Denied",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Access Denied",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFFCA5A5)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "User @${rejectedUser?.username} (${rejectedUser?.email}) does not hold administrative or moderator permissions.",
                            fontSize = 13.sp,
                            color = Color(0xFFFECACA),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        authRepository.signOut()
                                        rejectedUser = null
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Sign Out")
                            }

                            // Quick bootstrap option for the first device tester
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        authRepository.updateUserProfile(
                                            rejectedUser!!.copy(role = "admin")
                                        )
                                        val refreshed = authRepository.getUserProfile(rejectedUser!!.uid)
                                        isLoading = false
                                        if (refreshed != null && refreshed.userRole == UserRole.ADMIN) {
                                            rejectedUser = null
                                            onAdminVerified(refreshed)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Promote to Admin")
                            }
                        }
                    }
                }
            } else {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            val res = authRepository.signInWithGoogle(context)
                            isLoading = false
                            res.onFailure {
                                errorMessage = it.localizedMessage ?: "Sign-in cancelled or failed."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("admin_google_signin_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Authenticate Admin with Google", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
