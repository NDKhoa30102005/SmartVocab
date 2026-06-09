package com.example.smartvocab.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvocab.data.model.QuizQuestionData
import com.example.smartvocab.data.model.VocabularyWord
import com.example.smartvocab.data.repository.FirestoreVocabularyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lớp dữ liệu đại diện cho trạng thái UI State của màn hình Quiz.
 */
data class QuizUiState(
    val questions: List<QuizQuestionData> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val correctAnswersCount: Int = 0,
    val isResultSubmitted: Boolean = false,
    val timeLeft: Int = 165 // 2 phút 45 giây
)

/**
 * ViewModel quản lý logic nghiệp vụ và trạng thái của QuizScreen theo mô hình MVVM.
 */
class QuizViewModel(
    private val repository: FirestoreVocabularyRepository = FirestoreVocabularyRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(QuizUiState())
    val uiState: State<QuizUiState> = _uiState

    private var timerJob: Job? = null

    /**
     * Tải câu hỏi từ vựng từ Firestore và tạo ngẫu nhiên câu trả lời nhiễu.
     */
    fun loadQuiz(setId: String?) {
        viewModelScope.launch {
            _uiState.value = QuizUiState(isLoading = true, errorMessage = null)
            try {
                val globalWords = repository.getAllWords().getOrThrow()

                val sessionWords = if (setId.isNullOrBlank() || setId == "null") {
                    globalWords.shuffled().take(5)
                } else {
                    globalWords.filter { it.setId == setId }.shuffled().take(5)
                }

                if (sessionWords.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Bộ từ này chưa có từ vựng nào để làm Quiz.",
                        isLoading = false
                    )
                    return@launch
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
                
                _uiState.value = _uiState.value.copy(
                    questions = generated,
                    isLoading = false
                )
                
                startTimer()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Lỗi khi tải Quiz: ${e.localizedMessage}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Khởi chạy Coroutine đếm ngược thời gian làm bài.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeft > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(timeLeft = _uiState.value.timeLeft - 1)
            }
        }
    }

    /**
     * Chọn phương án trả lời.
     */
    fun selectOption(index: Int) {
        _uiState.value = _uiState.value.copy(selectedOptionIndex = index)
    }

    /**
     * Bỏ qua câu hỏi hiện tại.
     */
    fun skipQuestion(setId: String?, onNavigateToResult: (score: Int, total: Int, timeSpentText: String) -> Unit) {
        val state = _uiState.value
        val timeSpent = 165 - state.timeLeft
        
        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.value = state.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                selectedOptionIndex = null
            )
        } else {
            if (!state.isResultSubmitted) {
                _uiState.value = state.copy(isResultSubmitted = true)
                submitQuizResult(setId ?: "global", state.correctAnswersCount, state.questions.size, timeSpent, onNavigateToResult)
            }
        }
    }

    /**
     * Nộp câu trả lời cho câu hỏi hiện tại và chuyển sang câu tiếp theo.
     */
    fun submitAnswer(setId: String?, onNavigateToResult: (score: Int, total: Int, timeSpentText: String) -> Unit) {
        val state = _uiState.value
        val selectedIndex = state.selectedOptionIndex ?: return
        
        val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex) ?: return
        val isCorrect = selectedIndex == currentQuestion.correctIndex
        val newCorrectCount = if (isCorrect) state.correctAnswersCount + 1 else state.correctAnswersCount
        
        // Gửi kết quả từng câu dưới background ngầm
        val word = currentQuestion.word
        viewModelScope.launch {
            try {
                repository.submitQuizAnswer(word.setId, word.id, isCorrect)
            } catch (e: Exception) {
                // Bỏ qua lỗi mạng đơn lẻ
            }
        }
        
        val timeSpent = 165 - state.timeLeft
        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.value = state.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                selectedOptionIndex = null,
                correctAnswersCount = newCorrectCount
            )
        } else {
            if (!state.isResultSubmitted) {
                _uiState.value = state.copy(isResultSubmitted = true, correctAnswersCount = newCorrectCount)
                submitQuizResult(setId ?: "global", newCorrectCount, state.questions.size, timeSpent, onNavigateToResult)
            }
        }
    }

    /**
     * Xử lý khi hết giờ làm bài Quiz.
     */
    fun handleTimeout(setId: String?, onNavigateToResult: (score: Int, total: Int, timeSpentText: String) -> Unit) {
        val state = _uiState.value
        if (!state.isResultSubmitted) {
            _uiState.value = state.copy(isResultSubmitted = true)
            submitQuizResult(setId ?: "global", state.correctAnswersCount, state.questions.size, 165, onNavigateToResult)
        }
    }

    /**
     * Gửi toàn bộ kết quả bài kiểm tra lên Firestore khi kết thúc lượt làm bài.
     */
    private fun submitQuizResult(
        setId: String,
        score: Int,
        total: Int,
        timeSpent: Int,
        onNavigateToResult: (score: Int, total: Int, timeSpentText: String) -> Unit
    ) {
        timerJob?.cancel()
        viewModelScope.launch {
            try {
                repository.submitQuizResult(setId, score, total, timeSpent)
            } catch (e: Exception) {
                // Bỏ qua lỗi kết nối
            }
            val elapsed = String.format("%02d:%02d", timeSpent / 60, timeSpent % 60)
            onNavigateToResult(score, total, elapsed)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
