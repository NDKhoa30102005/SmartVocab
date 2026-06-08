package com.example.smartvocab.ui.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
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
import com.example.smartvocab.navigation.Screen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartvocab.viewmodel.ProgressViewModel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import com.example.smartvocab.ui.onboarding.GoalItem
import com.example.smartvocab.ui.onboarding.LevelItem
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    parentNavController: NavHostController,
    viewModel: ProgressViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.learningSettings
    val summary by viewModel.progressSummary
    val userName by viewModel.userName
    val email = FirebaseAuth.getInstance().currentUser?.email ?: "student@example.com"

    var newWordsCount by remember { mutableStateOf(10f) }
    var dailyReminderTime by remember { mutableStateOf("20:00") }
    var dailyReminderEnabled by remember { mutableStateOf(true) }
    var dueReviewReminderEnabled by remember { mutableStateOf(true) }
    var selectedGoal by remember { mutableStateOf("ielts") }
    var selectedLevel by remember { mutableStateOf("A1") }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showLevelDialog by remember { mutableStateOf(false) }

    // Đồng bộ hóa từ Firestore sang local state khi tải xong
    LaunchedEffect(settings) {
        newWordsCount = settings.newWordsPerDay.toFloat()
        dailyReminderTime = settings.reminderTime
        dailyReminderEnabled = settings.dailyReminderEnabled
        dueReviewReminderEnabled = settings.dueReviewReminderEnabled
        selectedGoal = settings.selectedGoal
        selectedLevel = settings.selectedLevel
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "SmartVocab",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile Hero Card (Bento Card)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Image placeholder
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "S",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-2).dp)
                        )
                        // Edit button overlay
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable {
                                    Toast.makeText(context, "Tính năng thay đổi ảnh đang phát triển", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Sửa",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = userName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = email,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Streak badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Chuỗi ${summary.streakDays} ngày",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // 2. Learning Goal & Level Card (Mục tiêu học tập)
            SettingsSectionCard(
                title = "Mục tiêu học tập",
                icon = Icons.Default.Flag
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsRow(
                        label = "Mục tiêu hiện tại",
                        value = getGoalName(selectedGoal),
                        onClick = {
                            showGoalDialog = true
                        }
                    )
                    
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    
                    SettingsRow(
                        label = "Trình độ hiện tại",
                        value = getLevelNameEn(selectedLevel),
                        onClick = {
                            showLevelDialog = true
                        }
                    )
                }
            }

            // 3. Learning Pace Settings
            SettingsSectionCard(
                title = "Tốc độ hàng ngày",
                icon = Icons.Default.Tune
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Slider 1: New words
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Số từ mới mỗi ngày",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${newWordsCount.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = newWordsCount,
                        onValueChange = { newWordsCount = it },
                        valueRange = 1f..50f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            thumbColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. Notifications Configuration
            SettingsSectionCard(
                title = "Cấu hình thông báo",
                icon = Icons.Default.Notifications
            ) {
                // Nhắc nhở học hàng ngày Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nhắc nhở học hàng ngày",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = dailyReminderEnabled,
                        onCheckedChange = { dailyReminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Reminder time selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val calendar = java.util.Calendar.getInstance()
                            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(java.util.Calendar.MINUTE)
                            android.app.TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    dailyReminderTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                },
                                hour,
                                minute,
                                true
                            ).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Giờ nhắc nhở học",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = dailyReminderTime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Nhắc nhở từ đến hạn ôn Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nhắc nhở từ đến hạn ôn",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = dueReviewReminderEnabled,
                        onCheckedChange = { dueReviewReminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

            }

            // Nút Lưu cài đặt
            Button(
                onClick = {
                    val newSettings = settings.copy(
                        newWordsPerDay = newWordsCount.toInt(),
                        reminderTime = dailyReminderTime,
                        dailyReminderEnabled = dailyReminderEnabled,
                        dueReviewReminderEnabled = dueReviewReminderEnabled,
                        selectedGoal = selectedGoal,
                        selectedLevel = selectedLevel
                    )
                    viewModel.updateSettings(newSettings) { success ->
                        if (success) {
                            com.example.smartvocab.util.ReminderManager.scheduleDailyReminder(
                                context,
                                newSettings.reminderTime,
                                newSettings.dailyReminderEnabled
                            )
                            com.example.smartvocab.util.ReminderManager.scheduleDueReviewReminder(
                                context,
                                newSettings.dueReviewReminderEnabled
                            )
                            Toast.makeText(context, "Đã lưu cài đặt thành công!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Lưu cài đặt thất bại!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Lưu cài đặt", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 5. Account Actions
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Change password button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Tính năng đổi mật khẩu đang được phát triển", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Đổi mật khẩu",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Logout button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                FirebaseAuth.getInstance().signOut()
                                parentNavController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = "Đăng xuất",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showGoalDialog) {
        GoalSelectionDialog(
            currentGoal = selectedGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                selectedGoal = newGoal
                showGoalDialog = false
                val newSettings = settings.copy(
                    newWordsPerDay = newWordsCount.toInt(),
                    reminderTime = dailyReminderTime,
                    dailyReminderEnabled = dailyReminderEnabled,
                    dueReviewReminderEnabled = dueReviewReminderEnabled,
                    selectedGoal = newGoal,
                    selectedLevel = selectedLevel
                )
                viewModel.updateSettings(newSettings) { success ->
                    if (success) {
                        Toast.makeText(context, "Đã cập nhật mục tiêu học tập!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showLevelDialog) {
        LevelSelectionDialog(
            currentLevel = selectedLevel,
            onDismiss = { showLevelDialog = false },
            onSave = { newLevel ->
                selectedLevel = newLevel
                showLevelDialog = false
                val newSettings = settings.copy(
                    newWordsPerDay = newWordsCount.toInt(),
                    reminderTime = dailyReminderTime,
                    dailyReminderEnabled = dailyReminderEnabled,
                    dueReviewReminderEnabled = dueReviewReminderEnabled,
                    selectedGoal = selectedGoal,
                    selectedLevel = newLevel
                )
                viewModel.updateSettings(newSettings) { success ->
                    if (success) {
                        Toast.makeText(context, "Đã cập nhật trình độ tiếng Anh!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            // Section Content
            content()
        }
    }
}

@Composable
fun SettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

fun getGoalName(id: String): String {
    return when (id.lowercase()) {
        "ielts" -> "IELTS"
        "toeic" -> "TOEIC"
        "com" -> "Giao tiếp"
        "work" -> "Công việc"
        "travel" -> "Du lịch"
        else -> "Khác"
    }
}

fun getLevelNameEn(code: String): String {
    return when (code.uppercase()) {
        "A1" -> "Beginner"
        "A2" -> "Elementary"
        "B1" -> "Intermediate"
        "B2" -> "Upper Inter."
        "C1" -> "Advanced"
        "C2" -> "Proficient"
        else -> code
    }
}

@Composable
fun GoalSelectionDialog(
    currentGoal: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var selected by remember { mutableStateOf(currentGoal) }
    val goals = listOf(
        GoalItem("ielts", "IELTS", Icons.Default.School),
        GoalItem("toeic", "TOEIC", Icons.Default.WorkspacePremium),
        GoalItem("com", "Giao tiếp", Icons.Default.Forum),
        GoalItem("work", "Công việc", Icons.Default.Work),
        GoalItem("travel", "Du lịch", Icons.Default.FlightTakeoff),
        GoalItem("other", "Khác", Icons.Default.MoreHoriz)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chọn mục tiêu học tập",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lựa chọn mục tiêu để chúng tôi tối ưu lộ trình cho bạn.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    items(goals) { goal ->
                        val isSelected = selected == goal.id
                        Card(
                            onClick = { selected = goal.id },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = goal.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = goal.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun LevelSelectionDialog(
    currentLevel: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var selected by remember { mutableStateOf(currentLevel) }
    val levels = listOf(
        LevelItem("A1", "Beginner", "Sơ cấp"),
        LevelItem("A2", "Elementary", "Cơ bản"),
        LevelItem("B1", "Intermediate", "Trung cấp"),
        LevelItem("B2", "Upper Inter.", "Trung cấp trên"),
        LevelItem("C1", "Advanced", "Cao cấp"),
        LevelItem("C2", "Proficient", "Thành thạo")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chọn trình độ tiếng Anh",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lựa chọn trình độ sát nhất với năng lực hiện tại của bạn.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    items(levels) { level ->
                        val isSelected = selected == level.code
                        Card(
                            onClick = { selected = level.code },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-8).dp, y = 8.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = level.code,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = level.nameEn,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = level.nameVi,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

