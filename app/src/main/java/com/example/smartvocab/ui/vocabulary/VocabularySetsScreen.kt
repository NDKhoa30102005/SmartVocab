package com.example.smartvocab.ui.vocabulary

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.smartvocab.data.MockData
import com.example.smartvocab.data.model.VocabularySet
import com.example.smartvocab.navigation.Screen
import com.example.smartvocab.viewmodel.VocabularySetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularySetsTab(
    parentNavController: NavHostController,
    viewModel: VocabularySetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState
    val context = LocalContext.current
    var setDeleteTarget by remember { mutableStateOf<VocabularySet?>(null) }

    // Reload sets whenever this tab is entered
    LaunchedEffect(key1 = true) {
        viewModel.loadSets()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Grid of Sets
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.sets) { set ->
                    VocabularySetCard(
                        set = set,
                        onClick = { parentNavController.navigate(Screen.VocabSetDetail.createRoute(set.id)) },
                        onEditClick = { parentNavController.navigate(Screen.AddEditSet.createRoute(set.id)) },
                        onDeleteClick = { setDeleteTarget = set }
                    )
                }

                // Create New Set Card
                item {
                    Card(
                        onClick = { parentNavController.navigate(Screen.AddEditSet.createRoute(null)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tạo bộ từ mới",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Thêm bộ từ vựng mới của riêng bạn để học tập.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (setDeleteTarget != null) {
        AlertDialog(
            onDismissRequest = { setDeleteTarget = null },
            title = { Text("Xóa bộ từ vựng?", fontWeight = FontWeight.Bold) },
            text = { Text("Hành động này sẽ xóa bộ từ '${setDeleteTarget?.title}' cùng với tất cả từ vựng bên trong và không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        setDeleteTarget?.let {
                            viewModel.deleteVocabularySet(it.id)
                            Toast.makeText(context, "Đã xóa bộ từ vựng", Toast.LENGTH_SHORT).show()
                        }
                        setDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { setDeleteTarget = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}
