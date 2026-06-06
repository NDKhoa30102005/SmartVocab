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
import com.example.smartvocab.viewmodel.VocabularySetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSetScreen(
    navController: NavHostController,
    setId: String?,
    viewModel: VocabularySetsViewModel = viewModel()
) {
    val context = LocalContext.current
    val isSaving by viewModel.isSaving
    val saveError by viewModel.saveError
    val isSaved by viewModel.isSaved

    LaunchedEffect(key1 = setId) {
        viewModel.loadSetForEdit(setId)
    }

    LaunchedEffect(key1 = isSaved) {
        if (isSaved) {
            Toast.makeText(context, "Đã lưu bộ từ vựng thành công!", Toast.LENGTH_SHORT).show()
            viewModel.resetForm()
            navController.popBackStack()
        }
    }

    LaunchedEffect(key1 = saveError) {
        saveError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isEditMode) "Sửa bộ từ vựng" else "Tạo bộ từ mới",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.resetForm()
                        navController.popBackStack() 
                    }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveSet() },
                        enabled = viewModel.title.value.isNotBlank() && !isSaving
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
                        text = "Thông tin bộ từ vựng",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    M3TextField(
                        value = viewModel.title.value,
                        onValueChange = { viewModel.title.value = it },
                        label = "Tên bộ từ *"
                    )

                    M3TextField(
                        value = viewModel.description.value,
                        onValueChange = { viewModel.description.value = it },
                        label = "Mô tả ngắn"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = if (viewModel.isEditMode) "Lưu thay đổi" else "Tạo bộ từ vựng",
                onClick = { viewModel.saveSet() },
                enabled = viewModel.title.value.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
