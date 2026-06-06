package com.example.smartvocab.ui.notification

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.smartvocab.data.model.AppNotification
import com.example.smartvocab.navigation.Screen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartvocab.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavHostController,
    viewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val notificationList by viewModel.notifications
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage

    fun markAllAsRead() {
        viewModel.markAllAsRead()
        Toast.makeText(context, "Đã đánh dấu tất cả là đã đọc!", Toast.LENGTH_SHORT).show()
    }

    fun deleteNotification(id: String) {
        viewModel.deleteNotification(id)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Thông báo",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Quay lại", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage ?: "Lỗi tải thông báo", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadNotifications() }) {
                            Text("Thử lại")
                        }
                    }
                }
            } else {
                // Action Row
                if (notificationList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { markAllAsRead() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    "Đánh dấu tất cả đã đọc",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Notification List
                if (notificationList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chưa có thông báo nào",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notificationList, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onDelete = { deleteNotification(notification.id) },
                                onCardClick = {
                                    viewModel.markAsRead(notification.id)
                                    if (notification.type == "REVIEW") {
                                        navController.navigate(Screen.FlashcardLearning.createRoute())
                                    }
                                },
                                onActionClick = {
                                    viewModel.markAsRead(notification.id)
                                    navController.navigate(Screen.FlashcardLearning.createRoute())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: AppNotification,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val icon = when (notification.type) {
        "REVIEW" -> Icons.Default.MenuBook
        "ACHIEVEMENT" -> Icons.Default.EmojiEvents
        else -> Icons.Default.SystemUpdate
    }

    val iconBg = when (notification.type) {
        "REVIEW" -> MaterialTheme.colorScheme.primaryContainer
        "ACHIEVEMENT" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val iconTint = when (notification.type) {
        "REVIEW" -> MaterialTheme.colorScheme.onPrimaryContainer
        "ACHIEVEMENT" -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onCardClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Unread Indicator left line
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(6.dp)
                        .height(100.dp) // Set height or rely on container. Using dynamic line is better:
                        // Instead of absolute height, let's make it match parent or use small layout. 
                        // Actually, let's use matchParentSize() for the modifier to span exactly the height:
                )
                // Let's use Spacer with matchParentSize() inside Box to draw the left indicator:
                Spacer(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(6.dp)
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(iconBg)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Content
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = notification.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = notification.getRelativeTime(),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = notification.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        // Special Action button if Review Reminder
                        if (notification.type == "REVIEW" && !notification.isRead) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onActionClick,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Ôn tập ngay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .offset(y = (-4).dp, x = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Xóa thông báo",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
