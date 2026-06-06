package com.example.smartvocab.ui.vocabulary

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.smartvocab.data.model.VocabularyWord
import com.example.smartvocab.navigation.Screen
import com.example.smartvocab.viewmodel.VocabSetDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabSetDetailScreen(
    navController: NavHostController,
    setId: String,
    viewModel: VocabSetDetailViewModel = viewModel()
) {
    val set by viewModel.vocabSet
    val setWords by viewModel.words
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val context = LocalContext.current
    var wordDeleteTarget by remember { mutableStateOf<VocabularyWord?>(null) }

    LaunchedEffect(key1 = setId) {
        viewModel.loadSetDetails(setId)
    }

    if (isLoading && set == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (set == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorMessage ?: "Không tìm thấy bộ từ vựng")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Quay lại")
                }
            }
        }
        return
    }

    val currentSet = set!!

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentSet.title,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AddEditSet.createRoute(currentSet.id)) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Chỉnh sửa bộ từ", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddEditWord.createRoute(setId = currentSet.id, wordId = null)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm từ")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Stats Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tiến độ học tập",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${setWords.filter { it.isLearned }.size} / ${setWords.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Đã nhớ",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
                
                // Progress
                val progressRatio = if (setWords.isEmpty()) 0f else setWords.filter { it.isLearned }.size.toFloat() / setWords.size
                LinearProgressIndicator(
                    progress = progressRatio,
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            // Quick Study Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.FlashcardLearning.createRoute(currentSet.id)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thẻ ghi nhớ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = { navController.navigate(Screen.QuizQuestion.createRoute(currentSet.id)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Làm Quiz", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            // Divider
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Words List
            if (setWords.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có từ vựng nào",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(setWords) { word ->
                        WordItemCard(
                            word = word,
                            onSpeakClick = {
                                Toast.makeText(context, "Phát âm: ${word.term}", Toast.LENGTH_SHORT).show()
                            },
                            onToggleLearned = { isLearned ->
                                viewModel.toggleWordLearned(currentSet.id, word.id, isLearned)
                            },
                            onEditClick = {
                                navController.navigate(Screen.AddEditWord.createRoute(setId = currentSet.id, wordId = word.id))
                            },
                            onDeleteClick = {
                                wordDeleteTarget = word
                            }
                        )
                    }
                }
            }
        }
    }

    // Word Delete Confirmation Dialog
    if (wordDeleteTarget != null) {
        AlertDialog(
            onDismissRequest = { wordDeleteTarget = null },
            title = { Text("Xóa từ vựng?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa từ '${wordDeleteTarget?.term}' khỏi bộ từ này?") },
            confirmButton = {
                Button(
                    onClick = {
                        wordDeleteTarget?.let {
                            viewModel.deleteWord(currentSet.id, it.id)
                            Toast.makeText(context, "Đã xóa từ vựng", Toast.LENGTH_SHORT).show()
                        }
                        wordDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { wordDeleteTarget = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}
