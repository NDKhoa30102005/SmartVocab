package com.example.smartvocab.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvocab.data.model.VocabularySet
import com.example.smartvocab.data.repository.FirestoreVocabularyRepository
import com.example.smartvocab.data.repository.VocabularyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

data class VocabularySetsUiState(
    val isLoading: Boolean = true,
    val sets: List<VocabularySet> = emptyList(),
    val errorMessage: String? = null
)

class VocabularySetsViewModel : ViewModel() {
    private val repository: VocabularyRepository = FirestoreVocabularyRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = mutableStateOf(VocabularySetsUiState())
    val uiState: State<VocabularySetsUiState> = _uiState

    // Form inputs for Add/Edit Set
    val title = mutableStateOf("")
    val description = mutableStateOf("")
    val category = mutableStateOf("IELTS")
    
    private val _isSaving = mutableStateOf(false)
    val isSaving: State<Boolean> = _isSaving

    private val _saveError = mutableStateOf<String?>(null)
    val saveError: State<String?> = _saveError

    private val _isSaved = mutableStateOf(false)
    val isSaved: State<Boolean> = _isSaved

    private var editingSetId: String? = null
    val isEditMode: Boolean
        get() = editingSetId != null

    init {
        loadSets()
    }

    fun loadSets() {
        val userId = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val oldMockIds = listOf(
                "ielts_academic",
                "tech_startup",
                "business_meetings",
                "travel_essential",
                "ielts_academic_$userId",
                "tech_startup_$userId",
                "business_meetings_$userId",
                "travel_essential_$userId"
            )
            for (oldId in oldMockIds) {
                repository.deleteVocabularySet(oldId)
            }
        }
        viewModelScope.launch {
            try {
                // Stream real-time vocabulary sets
                repository.getVocabularySets(userId).collect { sets ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sets = sets
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Lỗi khi tải danh sách: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteVocabularySet(setId: String) {
        viewModelScope.launch {
            repository.deleteVocabularySet(setId)
        }
    }

    // Add / Edit Set Screen logical methods
    fun loadSetForEdit(setId: String?) {
        resetForm()
        if (setId == null || setId == "null" || setId.isBlank()) return
        editingSetId = setId
        
        // Find set from loaded list or fetch from database
        val localSet = _uiState.value.sets.firstOrNull { it.id == setId }
        if (localSet != null) {
            title.value = localSet.title
            description.value = localSet.description
            category.value = localSet.category
        } else {
            viewModelScope.launch {
                val set = repository.getVocabularySetById(setId)
                if (set != null) {
                    title.value = set.title
                    description.value = set.description
                    category.value = set.category
                }
            }
        }
    }

    fun saveSet() {
        if (title.value.isBlank()) {
            _saveError.value = "Tên bộ từ vựng không được để trống"
            return
        }
        val userId = auth.currentUser?.uid ?: return

        _isSaving.value = true
        _saveError.value = null

        viewModelScope.launch {
            val result = if (editingSetId == null) {
                val newSet = VocabularySet(
                    title = title.value.trim(),
                    description = description.value.trim(),
                    category = category.value,
                    userId = userId
                )
                repository.addVocabularySet(newSet)
            } else {
                val original = repository.getVocabularySetById(editingSetId!!)
                if (original != null) {
                    val updatedSet = original.copy(
                        title = title.value.trim(),
                        description = description.value.trim(),
                        category = category.value
                    )
                    repository.updateVocabularySet(updatedSet)
                } else {
                    Result.failure(Exception("Không tìm thấy bộ từ để cập nhật"))
                }
            }

            _isSaving.value = false
            if (result.isSuccess) {
                _isSaved.value = true
            } else {
                _saveError.value = result.exceptionOrNull()?.localizedMessage ?: "Lưu thất bại"
            }
        }
    }

    fun resetForm() {
        title.value = ""
        description.value = ""
        category.value = "IELTS"
        _isSaving.value = false
        _saveError.value = null
        _isSaved.value = false
        editingSetId = null
    }
}
