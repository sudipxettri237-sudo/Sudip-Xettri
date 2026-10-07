package com.example.user.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.data.PostRepository
import com.example.common.data.SocialRepository
import com.example.common.models.Comment
import com.example.common.models.Post
import com.example.common.models.Story
import com.example.common.models.UserProfile
import com.example.common.ui.components.PostCard
import com.example.common.ui.components.UserAvatar
import com.example.user.stories.StoriesBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    currentProfile: UserProfile,
    postRepository: PostRepository,
    socialRepository: SocialRepository,
    onNavigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var commentingPost by remember { mutableStateOf<Post?>(null) }
    var reportingPost by remember { mutableStateOf<Post?>(null) }

    // Collect feed posts
    LaunchedEffect(currentProfile.uid) {
        postRepository.observeFeed(currentProfile.uid).collect { list ->
            posts = list
        }
    }

    // Collect stories
    LaunchedEffect(Unit) {
        socialRepository.observeStories().collect { list ->
            stories = list
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatePostDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_post_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rememberLazyListState()
            ) {
                // Stories section
                item {
                    StoriesBar(
                        stories = stories,
                        currentUserName = currentProfile.displayName,
                        currentUserPhoto = currentProfile.photoURL,
                        onCreateStoryClick = { showCreateStoryDialog = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }

                if (posts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Your feed is waiting for inspiration!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Tap the + button to share the first post.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(posts, key = { it.postId }) { post ->
                        PostCard(
                            post = post,
                            currentUserId = currentProfile.uid,
                            onLikeClick = {
                                coroutineScope.launch {
                                    postRepository.toggleLike(post.postId, currentProfile.uid)
                                }
                            },
                            onCommentClick = { commentingPost = post },
                            onShareClick = {
                                // increment share count
                            },
                            onReportClick = { reportingPost = post },
                            onDeleteClick = {
                                coroutineScope.launch {
                                    postRepository.deletePost(post.postId)
                                }
                            },
                            onProfileClick = { onNavigateToProfile(post.authorId) }
                        )
                    }
                }
            }
        }
    }

    // Create Post Dialog
    if (showCreatePostDialog) {
        CreatePostDialog(
            currentProfile = currentProfile,
            onDismiss = { showCreatePostDialog = false },
            onSubmit = { text, mediaUrl ->
                coroutineScope.launch {
                    postRepository.createPost(
                        authorId = currentProfile.uid,
                        authorName = currentProfile.displayName,
                        authorUsername = currentProfile.username,
                        authorPhotoURL = currentProfile.photoURL,
                        content = text,
                        mediaUrl = mediaUrl,
                        mediaType = if (mediaUrl.isNotBlank()) "image" else "none"
                    )
                    showCreatePostDialog = false
                }
            }
        )
    }

    // Create Story Dialog
    if (showCreateStoryDialog) {
        CreateStoryDialog(
            onDismiss = { showCreateStoryDialog = false },
            onSubmit = { mediaUrl, caption ->
                coroutineScope.launch {
                    socialRepository.createStory(
                        authorId = currentProfile.uid,
                        authorName = currentProfile.displayName,
                        authorPhotoURL = currentProfile.photoURL,
                        mediaUrl = mediaUrl,
                        caption = caption
                    )
                    showCreateStoryDialog = false
                }
            }
        )
    }

    // Comments Sheet
    commentingPost?.let { post ->
        CommentsSheet(
            post = post,
            currentProfile = currentProfile,
            postRepository = postRepository,
            onDismiss = { commentingPost = null }
        )
    }

    // Report Dialog
    reportingPost?.let { post ->
        ReportDialog(
            targetType = "post",
            targetId = post.postId,
            targetSummary = "Post by ${post.authorName}: ${post.content.take(30)}",
            currentProfile = currentProfile,
            socialRepository = socialRepository,
            onDismiss = { reportingPost = null }
        )
    }
}

@Composable
fun CreatePostDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSubmit: (content: String, mediaUrl: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }

    val presetImages = listOf(
        "https://picsum.photos/seed/cyber/800/600" to "Cyber",
        "https://picsum.photos/seed/nature/800/600" to "Nature",
        "https://picsum.photos/seed/design/800/600" to "Design"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Post", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("What's sparking in your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("post_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text("Image URL (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_media_url_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Quick presets:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    presetImages.forEach { (url, label) ->
                        FilterChip(
                            selected = mediaUrl == url,
                            onClick = { mediaUrl = if (mediaUrl == url) "" else url },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank() || mediaUrl.isNotBlank()) onSubmit(text, mediaUrl) },
                enabled = text.isNotBlank() || mediaUrl.isNotBlank(),
                modifier = Modifier.testTag("submit_post_button")
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onSubmit: (mediaUrl: String, caption: String) -> Unit
) {
    var mediaUrl by remember { mutableStateOf("https://picsum.photos/seed/story/600/900") }
    var caption by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Story", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text("Story Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (mediaUrl.isNotBlank()) onSubmit(mediaUrl, caption) },
                enabled = mediaUrl.isNotBlank(),
                modifier = Modifier.testTag("submit_story_button")
            ) {
                Text("Share Story")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    post: Post,
    currentProfile: UserProfile,
    postRepository: PostRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(post.postId) {
        postRepository.observeComments(post.postId).collect {
            comments = it
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Comments (${comments.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 350.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Be the first to share your perspective!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(comments, key = { it.commentId }) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            UserAvatar(photoUrl = c.authorPhotoURL, displayName = c.authorName, size = 32)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(c.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(c.content, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            val textToSend = commentText
                            commentText = ""
                            coroutineScope.launch {
                                postRepository.addComment(
                                    postId = post.postId,
                                    authorId = currentProfile.uid,
                                    authorName = currentProfile.displayName,
                                    authorPhotoURL = currentProfile.photoURL,
                                    content = textToSend
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ReportDialog(
    targetType: String,
    targetId: String,
    targetSummary: String,
    currentProfile: UserProfile,
    socialRepository: SocialRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedReason by remember { mutableStateOf("Inappropriate Content") }
    val reasons = listOf("Spam or Bot", "Harassment or Hate", "Inappropriate Content", "Misinformation", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $targetType", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Help keep Synapse a safe and welcoming space.", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        socialRepository.createReport(
                            reporterId = currentProfile.uid,
                            reporterName = currentProfile.displayName,
                            targetType = targetType,
                            targetId = targetId,
                            targetSummary = targetSummary,
                            reason = selectedReason
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
