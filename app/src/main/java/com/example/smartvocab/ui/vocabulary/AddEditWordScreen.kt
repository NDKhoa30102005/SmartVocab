package com.example.smartvocab.ui.vocabulary

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.smartvocab.ui.components.M3TextField
import com.example.smartvocab.ui.components.PrimaryButton
import com.example.smartvocab.viewmodel.VocabSetDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWordScreen(
    navController: NavHostController,
    setId: String?,
    wordId: String?,
    viewModel: VocabSetDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val isSavingWord by viewModel.isSavingWord
    val saveWordError by viewModel.saveWordError
    val isWordSaved by viewModel.isWordSaved

    LaunchedEffect(key1 = wordId) {
        viewModel.initializeWordForm(setId, wordId)
    }

    LaunchedEffect(key1 = isWordSaved) {
        if (isWordSaved) {
            Toast.makeText(context, "Đã lưu từ vựng thành công!", Toast.LENGTH_SHORT).show()
            viewModel.resetWordForm()
            navController.popBackStack()
        }
    }

    LaunchedEffect(key1 = saveWordError) {
        saveWordError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isWordEditMode) "Sửa từ vựng" else "Thêm từ mới",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.resetWordForm()
                        navController.popBackStack() 
                    }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveWord() },
                        enabled = viewModel.term.value.isNotBlank() && viewModel.definition.value.isNotBlank() && !isSavingWord
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Lưu")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Info Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Thông tin cơ bản",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    M3TextField(
                        value = viewModel.term.value,
                        onValueChange = { viewModel.term.value = it },
                        label = "Từ vựng *"
                    )

                    M3TextField(
                        value = viewModel.ipa.value,
                        onValueChange = { viewModel.ipa.value = it },
                        label = "Phiên âm (IPA)"
                    )

                    // Part of speech choice chips
                    Column {
                        Text(text = "Từ loại", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Danh từ", "Động từ", "Tính từ", "Trạng từ").forEach { pos ->
                                val selected = viewModel.partOfSpeech.value == pos
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .clickable { viewModel.partOfSpeech.value = pos }
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = pos,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    M3TextField(
                        value = viewModel.definition.value,
                        onValueChange = { viewModel.definition.value = it },
                        label = "Nghĩa tiếng Việt *"
                    )
                }
            }

            // Context Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Ngữ cảnh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = viewModel.example.value,
                        onValueChange = { viewModel.example.value = it },
                        label = { Text("Câu ví dụ tiếng Anh") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    OutlinedTextField(
                        value = viewModel.exampleTranslation.value,
                        onValueChange = { viewModel.exampleTranslation.value = it },
                        label = { Text("Dịch câu ví dụ") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    TextButton(
                        onClick = {
                            viewModel.generateExampleAuto()
                            Toast.makeText(context, "Đã tạo câu ví dụ tự động!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tạo câu ví dụ tự động", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Connections Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Liên kết",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    M3TextField(
                        value = viewModel.collocations.value,
                        onValueChange = { viewModel.collocations.value = it },
                        label = "Cụm từ đi kèm (Collocations)"
                    )

                    M3TextField(
                        value = viewModel.synonyms.value,
                        onValueChange = { viewModel.synonyms.value = it },
                        label = "Từ đồng nghĩa"
                    )

                    M3TextField(
                        value = viewModel.antonyms.value,
                        onValueChange = { viewModel.antonyms.value = it },
                        label = "Từ trái nghĩa"
                    )
                }
            }

            // Notes Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Ghi chú cá nhân",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = viewModel.notes.value,
                        onValueChange = { viewModel.notes.value = it },
                        label = { Text("Ghi chú cá nhân (Mẹo ghi nhớ...)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Bottom Save Button
            PrimaryButton(
                text = if (viewModel.isWordEditMode) "Lưu thay đổi" else "Lưu từ vựng",
                onClick = { viewModel.saveWord() },
                enabled = viewModel.term.value.isNotBlank() && viewModel.definition.value.isNotBlank() && !isSavingWord,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
