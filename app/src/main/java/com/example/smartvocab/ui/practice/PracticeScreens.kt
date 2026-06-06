package com.example.smartvocab.ui.practice

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.smartvocab.navigation.Screen
import kotlinx.coroutines.delay
import com.example.smartvocab.viewmodel.VocabularySetsViewModel
import com.example.smartvocab.data.model.VocabularySet
import com.example.smartvocab.data.model.VocabularyWord
import com.example.smartvocab.data.repository.FirestoreVocabularyRepository
import com.example.smartvocab.data.repository.await
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

// Flashcard Selection Tab
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardTab(
    parentNavController: NavHostController,
    viewModel: VocabularySetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState

    // Reload sets when entering
    LaunchedEffect(key1 = true) {
        viewModel.loadSets()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Thẻ ghi nhớ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp
                    )
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chọn một bộ từ vựng dưới đây để bắt đầu ôn tập bằng thẻ ghi nhớ (Flashcard).",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Có lỗi xảy ra",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                uiState.sets.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy bộ từ vựng nào.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.sets) { set ->
                            PracticeVocabularySetCard(
                                set = set,
                                onClick = {
                                    parentNavController.navigate(Screen.FlashcardLearning.createRoute(set.id))
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
fun PracticeVocabularySetCard(
    set: VocabularySet,
    onClick: () -> Unit
) {
    val progressPercent = (set.progress * 100).toInt()
    val wordsLearned = (set.progress * set.wordCount).toInt()

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = set.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = set.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = set.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordsLearned/${set.wordCount} từ đã thuộc",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$progressPercent%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            LinearProgressIndicator(
                progress = set.progress,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun PracticeCard(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(24.dp)
            )
            .aspectRatio(1.1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Question Data Class Helper
data class QuizQuestionData(
    val word: VocabularyWord,
    val options: List<String>,
    val correctIndex: Int
)

// Active Quiz Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(navController: NavHostController, setId: String?) {
    val repository = remember { FirestoreVocabularyRepository() }
    val coroutineScope = rememberCoroutineScope()

    var questions by remember { mutableStateOf<List<QuizQuestionData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(setId) {
        isLoading = true
        errorMessage = null
        try {
            val allWordsSnapshot = FirebaseFirestore.getInstance().collection("vocabulary_words").get().await()
            val globalWords = allWordsSnapshot.toObjects(VocabularyWord::class.java)
            
            val sessionWords = if (setId.isNullOrBlank() || setId == "null") {
                globalWords.shuffled().take(5)
            } else {
                globalWords.filter { it.setId == setId }.shuffled().take(5)
            }
            
            if (sessionWords.isEmpty()) {
                errorMessage = "Bộ từ này chưa có từ vựng nào để làm Quiz."
                isLoading = false
                return@LaunchedEffect
            }
            
            val generated = sessionWords.map { targetWord ->
                val correctOption = targetWord.definition
                val distractors = globalWords.filter { it.id != targetWord.id }
                    .map { it.definition }
                    .distinct()
                    .shuffled()
                    .take(3)
                
                val finalDistractors = (distractors + listOf(
                    "Một khái niệm không xác định.",
                    "Hành động lặp đi lặp lại nhiều lần.",
                    "Trạng thái không rõ ràng."
                )).take(3)
                
                val options = (finalDistractors + correctOption).shuffled()
                val correctIndex = options.indexOf(correctOption)
                
                QuizQuestionData(
                    word = targetWord,
                    options = options,
                    correctIndex = correctIndex
                )
            }
            questions = generated
            isLoading = false
        } catch(e: Exception) {
            errorMessage = "Lỗi khi tải Quiz: ${e.localizedMessage}"
            isLoading = false
        }
    }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var correctAnswersCount by remember { mutableStateOf(0) }
    var isResultSubmitted by remember { mutableStateOf(false) }
    var isAnswering by remember { mutableStateOf(false) }
    
    // Timer state
    var timeLeft by remember { mutableStateOf(165) } // 2:45 minutes
    
    LaunchedEffect(key1 = isLoading) {
        if (isLoading) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)
    
    val currentQuestion = questions.getOrNull(currentQuestionIndex)

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
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage ?: "Đã xảy ra lỗi", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Quay lại")
                            }
                        }
                    }
                }
                questions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Không có câu hỏi nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                currentQuestion != null -> {
                    // Progress Section
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
                                text = "Câu ${currentQuestionIndex + 1} / ${questions.size}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${((currentQuestionIndex + 1).toFloat() / questions.size * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = (currentQuestionIndex + 1).toFloat() / questions.size,
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

                    // Question Card
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

                    // Options List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        currentQuestion.options.forEachIndexed { index, option ->
                            val isSelected = selectedOptionIndex == index
                            val letter = ('A' + index).toString()
                            
                            Card(
                                onClick = { if (!isAnswering) selectedOptionIndex = index },
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

                    // Bottom Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isAnswering) return@Button
                                selectedOptionIndex = null
                                
                                if (currentQuestionIndex < questions.size - 1) {
                                    currentQuestionIndex++
                                } else {
                                    if (!isResultSubmitted) {
                                        isResultSubmitted = true
                                        val timeSpent = 165 - timeLeft
                                        coroutineScope.launch {
                                            repository.submitQuizResult(setId ?: "global", correctAnswersCount, questions.size, timeSpent)
                                            val elapsed = String.format("%02d:%02d", timeSpent / 60, timeSpent % 60)
                                            navController.navigate(Screen.QuizResult.createRoute(correctAnswersCount, questions.size, elapsed)) {
                                                popUpTo(Screen.QuizQuestion.route) { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isAnswering,
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
                                if (isAnswering || selectedOptionIndex == null) return@Button
                                isAnswering = true
                                val isCorrect = selectedOptionIndex == currentQuestion.correctIndex
                                if (isCorrect) {
                                    correctAnswersCount++
                                }
                                
                                val word = currentQuestion.word
                                coroutineScope.launch {
                                    try {
                                        repository.submitQuizAnswer(word.setId, word.id, isCorrect)
                                    } catch (e: Exception) {
                                        // Ignore
                                    } finally {
                                        selectedOptionIndex = null
                                        isAnswering = false
                                        if (currentQuestionIndex < questions.size - 1) {
                                            currentQuestionIndex++
                                        } else {
                                            if (!isResultSubmitted) {
                                                isResultSubmitted = true
                                                val timeSpent = 165 - timeLeft
                                                repository.submitQuizResult(setId ?: "global", correctAnswersCount, questions.size, timeSpent)
                                                val elapsed = String.format("%02d:%02d", timeSpent / 60, timeSpent % 60)
                                                navController.navigate(Screen.QuizResult.createRoute(correctAnswersCount, questions.size, elapsed)) {
                                                    popUpTo(Screen.QuizQuestion.route) { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = selectedOptionIndex != null && !isAnswering,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            if (isAnswering) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Gửi câu trả lời", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Quiz Result Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
    navController: NavHostController,
    score: Int,
    total: Int,
    time: String
) {
    val accuracy = if (total == 0) 0 else (score * 100) / total

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Kết quả luyện tập",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Đóng")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // SVG Circle Progress equivalent
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                val strokeColor = MaterialTheme.colorScheme.secondary
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                val progress = accuracy / 100f
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    drawArc(
                        color = strokeColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$accuracy%",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Độ chính xác",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Motivational message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (accuracy >= 80) "Làm tốt lắm!" else "Hãy cố gắng lên!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (accuracy >= 80) "Bạn đang làm chủ các từ vựng này." else "Hãy tiếp tục luyện tập để ghi nhớ lâu hơn.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Stats Bento Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Correct card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier
                        .weight(1f)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "$score", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "ĐÚNG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Incorrect card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier
                        .weight(1f)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${total - score}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "SAI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Time card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier
                        .weight(1f)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = time, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "THỜI GIAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tiếp tục", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                OutlinedButton(
                    onClick = {
                        // Restart quiz
                        navController.navigate(Screen.QuizQuestion.createRoute()) {
                            popUpTo(Screen.QuizResult.route) { inclusive = true }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Luyện tập lại", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
