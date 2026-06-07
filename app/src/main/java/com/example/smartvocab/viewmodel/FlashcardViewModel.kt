package com.example.smartvocab.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvocab.data.model.VocabularyWord
import com.example.smartvocab.data.model.LearningProgress
import com.example.smartvocab.data.repository.FirestoreVocabularyRepository
import com.example.smartvocab.data.repository.VocabularyRepository
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

data class FlashcardUiState(
    val isLoading: Boolean = true,
    val words: List<VocabularyWord> = emptyList(),
    val errorMessage: String? = null,
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val againCount: Int = 0,
    val goodCount: Int = 0,
    val isSubmitting: Boolean = false
)

class FlashcardViewModel : ViewModel() {
    private val repository: VocabularyRepository = FirestoreVocabularyRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = mutableStateOf(FlashcardUiState())
    val uiState: State<FlashcardUiState> = _uiState

    private var currentSetId: String? = null

    fun loadWords(setId: String?) {
        currentSetId = setId
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, currentIndex = 0, isFlipped = false)

        val userId = auth.currentUser?.uid ?: ""
        
        viewModelScope.launch {
            if (setId.isNullOrBlank() || setId == "null") {
                firestore.collection("learning_progress")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { progressSnapshot ->
                        val learnedWordIds = progressSnapshot.documents.mapNotNull { doc ->
                            val status = doc.getString("status") ?: "NEW"
                            val repCount = doc.getLong("repetitionCount") ?: 0L
                            if (status != "NEW" || repCount > 0L) {
                                doc.getString("wordId")
                            } else {
                                null
                            }
                        }.toSet()
                        
                        firestore.collection("vocabulary_words")
                            .get()
                            .addOnSuccessListener { wordsSnapshot ->
                                val allWords = wordsSnapshot.toObjects(VocabularyWord::class.java)
                                val dueWords = allWords.filter { it.id !in learnedWordIds }.sortedBy { it.createdAt }
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    words = dueWords
                                )
                            }
                            .addOnFailureListener { exception ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Lỗi khi tải từ vựng: ${exception.localizedMessage}"
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Lỗi khi tải tiến trình: ${exception.localizedMessage}"
                        )
                    }
            } else {
                try {
                    repository.getWords(setId).collect { wordList ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            words = wordList.sortedBy { it.createdAt }
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Lỗi khi tải dữ liệu: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun handleAnswer(rating: String) {
        val state = _uiState.value
        val word = state.words.getOrNull(state.currentIndex) ?: return
        
        val isCorrect = rating == "good" || rating == "easy"
        val newGoodCount = if (isCorrect) state.goodCount + 1 else state.goodCount
        val newAgainCount = if (!isCorrect) state.againCount + 1 else state.againCount

        // 1. Cập nhật UI ngay lập tức để chuyển sang từ tiếp theo
        _uiState.value = state.copy(
            currentIndex = state.currentIndex + 1,
            isFlipped = false,
            goodCount = newGoodCount,
            againCount = newAgainCount
        )

        // 2. Chạy lưu Firestore ở background (không chặn UI)
        viewModelScope.launch {
            try {
                val result = repository.updateFlashcardProgress(word.setId, word.id, rating)
                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    Log.e("FLASHCARD_ERROR", "Loi khi luu tien trinh hoc: ${ex?.message}", ex)
                } else {
                    Log.d("FLASHCARD_SUCCESS", "Da luu tien trinh hoc thanh cong cho tu: ${word.term}")
                }
            } catch (e: Exception) {
                Log.e("FLASHCARD_ERROR", "Exception trong coroutine updateFlashcardProgress: ${e.message}", e)
            }
        }
    }

    fun toggleFlip() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun resetSession() {
        _uiState.value = _uiState.value.copy(
            currentIndex = 0,
            isFlipped = false,
            againCount = 0,
            goodCount = 0
        )
    }
}
