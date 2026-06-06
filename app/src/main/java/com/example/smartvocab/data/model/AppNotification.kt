package com.example.smartvocab.data.model

import com.google.firebase.Timestamp

/**
 * Lớp dữ liệu lưu trữ thông tin của một thông báo.
 */
data class AppNotification(
    val id: String = "",                 // Mã định danh thông báo
    val userId: String = "",             // Mã người dùng liên kết
    val title: String = "",             // Tiêu đề thông báo
    val message: String = "",           // Nội dung chi tiết
    val type: String = "SYSTEM",         // Loại thông báo ("REVIEW", "ACHIEVEMENT", "SYSTEM")
    @field:JvmField val isRead: Boolean = false, // Trạng thái đã đọc hay chưa
    val createdAt: Timestamp? = null     // Thời gian tạo từ Firestore Timestamp
) {
    /**
     * Hàm helper chuyển đổi thời gian sang chuỗi hiển thị tương đối dạng tiếng Việt.
     */
    fun getRelativeTime(): String {
        val timeMs = createdAt?.seconds?.times(1000) ?: return "Vừa xong"
        val diff = System.currentTimeMillis() - timeMs
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Vừa xong"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            else -> "$days ngày trước"
        }
    }
}
