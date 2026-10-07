package com.example.admin.posts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.common.models.Post
import com.example.common.models.UserProfile
import com.example.common.ui.components.UserAvatar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PostManagementScreen(
    currentAdmin: UserProfile,
    adminRepository: AdminRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var visibilityFilter by remember { mutableStateOf("all") }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshPosts() {
        coroutineScope.launch {
            isLoading = true
            var list = adminRepository.getAllPosts(searchQuery)
            if (visibilityFilter != "all") {
                list = list.filter { it.visibility == visibilityFilter }
            }
            posts = list
            isLoading = false
        }
    }

    LaunchedEffect(searchQuery, visibilityFilter) {
        refreshPosts()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Post Moderation", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search post content, author or ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("admin_post_search_input"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "public" to "Public", "hidden" to "Hidden / Flagged").forEach { (key, label) ->
                FilterChip(
                    selected = visibilityFilter == key,
                    onClick = { visibilityFilter = key },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF10B981))
            }
        } else if (posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No posts found matching filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(posts, key = { it.postId }) { post ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("admin_post_item_${post.postId}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(photoUrl = post.authorPhotoURL, displayName = post.authorName, size = 36)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Post ID: ${post.postId.take(12)}...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = if (post.visibility == "hidden") Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = post.visibility.uppercase(),
                                        color = if (post.visibility == "hidden") Color(0xFFEF4444) else Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(post.content, fontSize = 14.sp, maxLines = 4)

                            post.createdAt?.let { ts ->
                                val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(ts.toDate())
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (post.visibility == "hidden") {
                                    FilledTonalButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                adminRepository.setPostVisibility(
                                                    postId = post.postId,
                                                    visibility = "public",
                                                    adminUid = currentAdmin.uid,
                                                    adminName = currentAdmin.displayName,
                                                    reason = "Restored by moderator"
                                                )
                                                refreshPosts()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restore")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                adminRepository.setPostVisibility(
                                                    postId = post.postId,
                                                    visibility = "hidden",
                                                    adminUid = currentAdmin.uid,
                                                    adminName = currentAdmin.displayName,
                                                    reason = "Flagged / Hidden by moderator"
                                                )
                                                refreshPosts()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Hide Post")
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            adminRepository.deletePost(
                                                postId = post.postId,
                                                adminUid = currentAdmin.uid,
                                                adminName = currentAdmin.displayName
                                            )
                                            refreshPosts()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
