package com.example.smartvocab.data.model

/**
 * Lớp dữ liệu lưu trữ cài đặt học tập và cấu hình nhận thông báo của người dùng.
 */
data class LearningSettings(
    val id: String = "",                      // Mã định danh cài đặt
    val userId: String = "",                  // Mã người dùng liên kết
    val newWordsPerDay: Int = 10,             // Số từ mới cần học mỗi ngày
    val reviewWordsPerDay: Int = 20,          // Số từ cần ôn tập mỗi ngày
    val reminderTime: String = "20:00",       // Giờ nhắc nhở học hàng ngày
    @field:JvmField val pushNotificationEnabled: Boolean = true,  // Bật/Tắt thông báo đẩy
    @field:JvmField val emailNotificationEnabled: Boolean = false // Bật/Tắt thông báo qua email
)
