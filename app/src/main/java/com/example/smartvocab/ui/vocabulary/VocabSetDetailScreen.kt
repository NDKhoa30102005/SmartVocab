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
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.FileDownload

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
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
    var showImportDialog by remember { mutableStateOf(false) }

    var selectedCsvUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCsvFileName by remember { mutableStateOf<String?>(null) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedCsvUri = uri
        if (uri != null) {
            var name: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
            selectedCsvFileName = name ?: "file.csv"
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val csvData = viewModel.generateCsvData()
                    outputStream.write(csvData.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Xuất file CSV thành công!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi xuất file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }


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
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Nhập hàng loạt từ", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            if (setWords.isNotEmpty()) {
                                csvExportLauncher.launch("${currentSet.title}.csv")
                            } else {
                                Toast.makeText(context, "Bộ từ vựng trống, không thể xuất file!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Xuất bộ từ ra file CSV", tint = MaterialTheme.colorScheme.primary)
                    }
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

    // Bulk Import Dialog
    if (showImportDialog) {
        var activeTab by remember { mutableStateOf(0) }
        val importText by viewModel.importText
        val isImporting by viewModel.isImporting
        val importError by viewModel.importError
        val isImported by viewModel.isImported

        val isImportingCsv by viewModel.isImportingCsv
        val csvImportError by viewModel.csvImportError
        val isCsvImported by viewModel.isCsvImported

        val auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid ?: ""

        val previewWords = remember(importText) {
            viewModel.parseImportText(importText, currentSet.id, currentUserId)
        }

        val parsedCsvCount = remember(selectedCsvUri, showImportDialog) {
            if (selectedCsvUri != null) {
                try {
                    context.contentResolver.openInputStream(selectedCsvUri!!)?.use { stream ->
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(stream, "UTF-8"))
                        var linesCount = 0
                        var line = reader.readLine()
                        var isHeader = true
                        while (line != null) {
                            if (isHeader) {
                                isHeader = false
                                line = reader.readLine()
                                continue
                            }
                            if (line.isNotBlank()) {
                                val cols = viewModel.parseCsvLine(line)
                                if (cols.isNotEmpty() && (cols.getOrNull(0)?.isNotBlank() == true) && (cols.getOrNull(3)?.isNotBlank() == true)) {
                                    linesCount++
                                }
                            }
                            line = reader.readLine()
                        }
                        linesCount
                    } ?: 0
                } catch (e: Exception) {
                    -1
                }
            } else {
                0
            }
        }

        LaunchedEffect(isImported) {
            if (isImported) {
                Toast.makeText(context, "Nhập thành công ${previewWords.size} từ vựng", Toast.LENGTH_SHORT).show()
                showImportDialog = false
                viewModel.resetImportForm()
            }
        }

        LaunchedEffect(isCsvImported) {
            if (isCsvImported) {
                Toast.makeText(context, "Nhập thành công $parsedCsvCount từ vựng từ file CSV", Toast.LENGTH_SHORT).show()
                showImportDialog = false
                selectedCsvUri = null
                selectedCsvFileName = null
                viewModel.resetCsvImportForm()
            }
        }

        AlertDialog(
            onDismissRequest = { 
                if (!isImporting && !isImportingCsv) {
                    showImportDialog = false
                    selectedCsvUri = null
                    selectedCsvFileName = null
                    viewModel.resetImportForm()
                    viewModel.resetCsvImportForm()
                }
            },
            title = {
                Text(
                    text = "Nhập hàng loạt từ vựng",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tab Headers
                    TabRow(
                        selectedTabIndex = activeTab,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { if (!isImporting && !isImportingCsv) activeTab = 0 },
                            text = { Text("Dán văn bản", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { if (!isImporting && !isImportingCsv) activeTab = 1 },
                            text = { Text("Nhập file CSV", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Scrollable content area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (activeTab == 0) {
                            // TEXT IMPORT TAB
                            Text(
                                text = "Nhập danh sách từ vựng định dạng:\nTừ vựng - Nghĩa (mỗi từ một dòng)",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Ví dụ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("hello - xin chào\nworld : thế giới\nelicit – khơi gợi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            OutlinedTextField(
                                value = importText,
                                onValueChange = { viewModel.importText.value = it },
                                placeholder = { Text("Dán văn bản vào đây...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                enabled = !isImporting
                            )

                            if (importText.isNotBlank()) {
                                Surface(
                                    color = if (previewWords.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = if (previewWords.isNotEmpty()) "✓ Tìm thấy ${previewWords.size} từ hợp lệ" else "⚠ Không tìm thấy từ hợp lệ",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (previewWords.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            if (importError != null) {
                                Text(importError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        } else {
                            // CSV IMPORT TAB
                            Text(
                                text = "Tạo file Excel/Google Sheets có 10 cột theo đúng thứ tự: từ vựng, phiên âm, loại từ, nghĩa, ví dụ, dịch ví dụ, đồng nghĩa, trái nghĩa, cụm từ, ghi chú. Lưu file dưới dạng đuôi .csv (UTF-8) để tải lên.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Show CSV schema guide
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Thứ tự cột bắt buộc trong file CSV:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("1. Từ vựng (Bắt buộc)\n2. Phiên âm\n3. Loại từ\n4. Nghĩa tiếng Việt (Bắt buộc)\n5. Ví dụ\n6. Dịch ví dụ\n7. Đồng nghĩa\n8. Trái nghĩa\n9. Cụm từ đi kèm\n10. Ghi chú", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // File Pick Area
                            if (selectedCsvFileName == null) {
                                OutlinedButton(
                                    onClick = { csvPickerLauncher.launch("*/*") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isImportingCsv
                                ) {
                                    Text("Chọn file CSV từ thiết bị", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedCsvFileName!!,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = when (parsedCsvCount) {
                                                    -1 -> "⚠ Lỗi đọc file hoặc định dạng sai"
                                                    0 -> "⚠ File không chứa từ vựng hợp lệ"
                                                    else -> "✓ Tìm thấy $parsedCsvCount từ hợp lệ chuẩn bị nhập"
                                                },
                                                fontSize = 11.sp,
                                                color = if (parsedCsvCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                selectedCsvUri = null
                                                selectedCsvFileName = null
                                            },
                                            enabled = !isImportingCsv
                                        ) {
                                            Text("Thay đổi", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }

                            if (csvImportError != null) {
                                Text(csvImportError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (activeTab == 0) {
                    Button(
                        onClick = { viewModel.importWords(currentSet.id) },
                        enabled = !isImporting && previewWords.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Nhập vào bộ từ", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            selectedCsvUri?.let { uri ->
                                try {
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        viewModel.importCsv(stream, currentSet.id)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi mở file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isImportingCsv && selectedCsvUri != null && parsedCsvCount > 0,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isImportingCsv) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Nhập từ file CSV", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showImportDialog = false
                        selectedCsvUri = null
                        selectedCsvFileName = null
                        viewModel.resetImportForm()
                        viewModel.resetCsvImportForm()
                    },
                    enabled = !isImporting && !isImportingCsv
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
