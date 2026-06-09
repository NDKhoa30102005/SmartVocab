package com.example.smartvocab.ui.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.smartvocab.navigation.Screen
import com.example.smartvocab.viewmodel.QuizViewModel

/**
 * Màn hình làm Quiz trắc nghiệm từ vựng tiếng Anh.
 * Đã được di chuyển sang package com.example.smartvocab.ui.learning cùng chỗ với FlashcardScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    navController: NavHostController, 
    setId: String?,
    viewModel: QuizViewModel = viewModel()
) {
    val uiState by viewModel.uiState

    // Tải Quiz lần đầu khi vào màn hình
    LaunchedEffect(setId) {
        viewModel.loadQuiz(setId)
    }

    // Tự động nộp bài khi hết giờ
    LaunchedEffect(uiState.timeLeft) {
        if (uiState.timeLeft == 0 && uiState.questions.isNotEmpty()) {
            viewModel.handleTimeout(setId) { score, total, elapsed ->
                navController.navigate(Screen.QuizResult.createRoute(score, total, elapsed, setId)) {
                    popUpTo(Screen.QuizQuestion.route) { inclusive = true }
                }
            }
        }
    }

    val minutes = uiState.timeLeft / 60
    val seconds = uiState.timeLeft % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)
    
    val currentQuestion = uiState.questions.getOrNull(uiState.currentQuestionIndex)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SmartVocab",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Đóng")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = timerText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.errorMessage ?: "Đã xảy ra lỗi", 
                                color = MaterialTheme.colorScheme.error, 
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Quay lại")
                            }
                        }
                    }
                }
                uiState.questions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không có câu hỏi nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                currentQuestion != null -> {
                    // Thanh tiến trình
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Câu ${uiState.currentQuestionIndex + 1} / ${uiState.questions.size}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${((uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size },
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Thẻ câu hỏi
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
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Chọn nghĩa đúng của từ ",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "\"${currentQuestion.word.term}\"",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Chọn định nghĩa phù hợp nhất dựa trên cách sử dụng chuẩn.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Danh sách các đáp án lựa chọn
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        currentQuestion.options.forEachIndexed { index, option ->
                            val isSelected = uiState.selectedOptionIndex == index
                            val letter = ('A' + index).toString()
                            
                            Card(
                                onClick = { viewModel.selectOption(index) },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    ) {
                                        Text(
                                            text = letter,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Text(
                                        text = option,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Nút thao tác dưới cùng
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.skipQuestion(setId) { score, total, elapsed ->
                                    navController.navigate(Screen.QuizResult.createRoute(score, total, elapsed, setId)) {
                                        popUpTo(Screen.QuizQuestion.route) { inclusive = true }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Bỏ qua", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.submitAnswer(setId) { score, total, elapsed ->
                                    navController.navigate(Screen.QuizResult.createRoute(score, total, elapsed, setId)) {
                                        popUpTo(Screen.QuizQuestion.route) { inclusive = true }
                                    }
                                }
                            },
                            enabled = uiState.selectedOptionIndex != null,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Gửi câu trả lời", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
