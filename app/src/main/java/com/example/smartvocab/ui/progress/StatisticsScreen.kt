package com.example.smartvocab.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.smartvocab.data.Achievement
import com.example.smartvocab.data.model.DailyActivity

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartvocab.viewmodel.ProgressViewModel
import com.example.smartvocab.ui.dashboard.getTodayDayOfWeek
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Calendar.getInstance().time)
}

fun getChartLabel(dateStr: String, period: Int): String {
    if (period == 7) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val date = sdf.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance()
            cal.time = date
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                Calendar.SUNDAY -> "CN"
                else -> dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    } else {
        val parts = dateStr.split("-")
        return if (parts.size == 3) parts[2] else dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsTab(
    parentNavController: NavHostController,
    viewModel: ProgressViewModel = viewModel()
) {
    val summary by viewModel.progressSummary
    val activities by viewModel.dailyActivities
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val achievements by viewModel.achievements
    val selectedPeriod by viewModel.selectedPeriod

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

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Đã xảy ra lỗi",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadProgress() }) {
                        Text("Thử lại")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Column {
                    Text(
                        text = "Thống kê học tập",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Theo dõi tiến độ và thành tích của bạn",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!viewModel.hasLearningData) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Chưa có dữ liệu để thống kê.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Level Estimate Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Ước lượng trình độ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = summary.levelEstimate,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bento Grid Metrics
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Metric 1: Tổng từ đã học
                        MetricCard(
                            title = "Tổng từ đã học",
                            value = "${summary.totalWordsLearned}",
                            icon = Icons.Default.School,
                            iconColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        )

                        // Metric 2: Từ đã ghi nhớ
                        MetricCard(
                            title = "Từ đã ghi nhớ",
                            value = "${summary.masteredWords}",
                            icon = Icons.Default.TaskAlt,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Metric 3: Từ cần ôn
                        MetricCard(
                            title = "Từ cần ôn",
                            value = "${summary.reviewDueWords}",
                            icon = Icons.Default.Update,
                            iconColor = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f)
                        )

                        // Metric 4: Chuỗi học
                        MetricCard(
                            title = "Chuỗi học (ngày)",
                            value = "${summary.streakDays}",
                            icon = Icons.Default.LocalFireDepartment,
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Metric 5: Độ chính xác
                        MetricCard(
                            title = "Độ chính xác",
                            value = "${summary.accuracy.toInt()}%",
                            icon = Icons.Default.QueryStats,
                            iconColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        )

                        // Metric 6: Tỷ lệ ghi nhớ
                        MetricCard(
                            title = "Tỷ lệ ghi nhớ",
                            value = "${summary.retentionRate.toInt()}%",
                            icon = Icons.Default.Psychology,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Charts Section (Hoạt động)
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
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hoạt động",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Interval Selector
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.padding(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedPeriod == 7) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
                                        shadowElevation = if (selectedPeriod == 7) 1.dp else 0.dp,
                                        modifier = Modifier.clickable { viewModel.setSelectedPeriod(7) }
                                    ) {
                                        Text(
                                            text = "7 Ngày",
                                            fontSize = 11.sp,
                                            fontWeight = if (selectedPeriod == 7) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedPeriod == 7) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedPeriod == 30) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
                                        shadowElevation = if (selectedPeriod == 30) 1.dp else 0.dp,
                                        modifier = Modifier.clickable { viewModel.setSelectedPeriod(30) }
                                    ) {
                                        Text(
                                            text = "30 Ngày",
                                            fontSize = 11.sp,
                                            fontWeight = if (selectedPeriod == 30) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedPeriod == 30) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bar Chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val chartActivities = viewModel.chartActivities
                            val maxLearned = chartActivities.maxOfOrNull { it.learnedWords } ?: 0
                            val maxLearnedVal = if (maxLearned > 0) maxLearned else 1
                            val todayDateStr = getTodayDateString()
                            val chartData = chartActivities.map { activity ->
                                val percentage = if (maxLearnedVal > 0) activity.learnedWords.toFloat() / maxLearnedVal else 0f
                                val label = getChartLabel(activity.date, selectedPeriod)
                                ChartBar(
                                    day = label,
                                    percentage = if (percentage == 0f) 0f else percentage.coerceIn(0.05f, 1f),
                                    valueLabel = activity.learnedWords.toString(),
                                    isActive = activity.date == todayDateStr
                                )
                            }

                            chartData.forEach { bar ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    // Value above bar
                                    if (selectedPeriod == 7 && bar.valueLabel != "0") {
                                        Text(
                                            text = bar.valueLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (bar.isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (bar.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else if (selectedPeriod == 30 && bar.valueLabel != "0") {
                                        Text(
                                            text = bar.valueLabel,
                                            fontSize = 8.sp,
                                            fontWeight = if (bar.isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (bar.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Bar Column representation
                                    Box(
                                        modifier = Modifier
                                            .width(if (selectedPeriod == 30) 6.dp else 16.dp)
                                            .fillMaxHeight(bar.percentage)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                if (bar.isActive) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Day label below
                                    val showLabel = selectedPeriod == 7 || bar.day.toIntOrNull()?.let { it % 5 == 0 || it == 1 } ?: true
                                    Text(
                                        text = if (showLabel) bar.day else "",
                                        fontSize = if (selectedPeriod == 30) 9.sp else 12.sp,
                                        fontWeight = if (bar.isActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (bar.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Achievements Section (Danh hiệu)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Danh hiệu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        achievements.forEach { achievement ->
                            AchievementRow(achievement = achievement)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(20.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(containerColor)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AchievementRow(achievement: Achievement) {
    val iconsMap = mapOf(
        "workspace_premium" to Icons.Default.WorkspacePremium,
        "emoji_events" to Icons.Default.EmojiEvents,
        "task_alt" to Icons.Default.TaskAlt
    )
    val icon = iconsMap[achievement.icon] ?: Icons.Default.Star

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) MaterialTheme.colorScheme.tertiaryFixed
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Texts & Progress
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = achievement.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = achievement.progress,
                        color = if (achievement.isUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "${achievement.currentVal}/${achievement.targetVal}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class ChartBar(
    val day: String,
    val percentage: Float,
    val valueLabel: String,
    val isActive: Boolean = false
)
