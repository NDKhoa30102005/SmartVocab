package com.example.smartvocab.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvocab.data.model.AppNotification
import com.example.smartvocab.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * ViewModel chịu trách nhiệm quản lý danh sách thông báo và các thao tác liên quan (đọc, xóa).
 */
class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userId: String
        get() = auth.currentUser?.uid ?: "mock_user_id"

    private val _notifications = mutableStateOf<List<AppNotification>>(emptyList())
    val notifications: State<List<AppNotification>> = _notifications

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    init {
        loadNotifications()
    }

    /**
     * Tải danh sách thông báo của người dùng từ Firestore.
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _notifications.value = repository.getNotifications(userId)
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi tải thông báo: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val result = repository.markAsRead(userId, notificationId)
            if (result.isSuccess) {
                _notifications.value = _notifications.value.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
            }
        }
    }

    /**
     * Đánh dấu toàn bộ thông báo là đã đọc.
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            val result = repository.markAllAsRead(userId)
            if (result.isSuccess) {
                _notifications.value = _notifications.value.map {
                    it.copy(isRead = true)
                }
            }
        }
    }

    /**
     * Xóa thông báo khỏi Firestore và cập nhật lại UI state.
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val result = repository.deleteNotification(userId, notificationId)
            if (result.isSuccess) {
                _notifications.value = _notifications.value.filter { it.id != notificationId }
            }
        }
    }
}
