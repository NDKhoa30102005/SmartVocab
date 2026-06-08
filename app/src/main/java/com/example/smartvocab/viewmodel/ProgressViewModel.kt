package com.example.smartvocab.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvocab.data.model.ProgressSummary
import com.example.smartvocab.data.model.DailyActivity
import com.example.smartvocab.data.model.LearningSettings
import com.example.smartvocab.data.repository.ProgressRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * ViewModel chịu trách nhiệm quản lý dữ liệu tiến trình học tập và cài đặt cấu hình học của người dùng.
 * Sử dụng mô hình MVVM đơn giản, gọi Repository để tương tác với Firestore.
 */
class ProgressViewModel(
    private val repository: ProgressRepository = ProgressRepository()
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // Lấy userId của người dùng hiện tại, nếu chưa đăng nhập dùng ID mặc định phục vụ demo
    private val userId: String
        get() = auth.currentUser?.uid ?: "mock_user_id"

    private val _userName = mutableStateOf("Học viên")
    val userName: State<String> = _userName

    private val _progressSummary = mutableStateOf(ProgressSummary())
    val progressSummary: State<ProgressSummary> = _progressSummary

    private val _dailyActivities = mutableStateOf<List<DailyActivity>>(emptyList())
    val dailyActivities: State<List<DailyActivity>> = _dailyActivities

    private val _learningSettings = mutableStateOf(LearningSettings())
    val learningSettings: State<LearningSettings> = _learningSettings



    private val _selectedPeriod = mutableStateOf(7)
    val selectedPeriod: State<Int> = _selectedPeriod

    fun setSelectedPeriod(days: Int) {
        _selectedPeriod.value = days
    }

    val chartActivities: List<DailyActivity>
        get() {
            val all = _dailyActivities.value
            val days = _selectedPeriod.value
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // Generate last N dates in chronological order
            val dates = (0 until days).map { i ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DATE, -i)
                sdf.format(cal.time)
            }.reversed()
            
            val activityMap = all.associateBy { it.date }
            
            return dates.map { dateStr ->
                activityMap[dateStr] ?: DailyActivity(
                    date = dateStr,
                    userId = userId,
                    learnedWords = 0,
                    reviewedWords = 0,
                    totalAnswers = 0,
                    correctAnswers = 0,
                    wrongAnswers = 0
                )
            }
        }

    val hasLearningData: Boolean
        get() = _progressSummary.value.totalWordsLearned > 0 || _dailyActivities.value.isNotEmpty()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    init {
        loadProgress()
    }

    /**
     * Tải dữ liệu từ Firestore bất đồng bộ qua Coroutines.
     */
    fun loadProgress() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val currentUid = userId
                _userName.value = repository.getUserName(currentUid)
                _progressSummary.value = repository.getProgressSummary(currentUid)
                _dailyActivities.value = repository.getDailyActivity(currentUid)
                _learningSettings.value = repository.getLearningSettings(currentUid)
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi tải tiến trình: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Lưu cấu hình học tập mới lên Firestore và cập nhật lại state của UI.
     */
    fun updateSettings(newSettings: LearningSettings, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.updateLearningSettings(userId, newSettings)
            if (result.isSuccess) {
                _learningSettings.value = newSettings
                onResult(true)
            } else {
                _errorMessage.value = "Lỗi lưu cấu hình: ${result.exceptionOrNull()?.localizedMessage}"
                onResult(false)
            }
            _isLoading.value = false
        }
    }
}
