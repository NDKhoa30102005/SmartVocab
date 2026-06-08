package com.example.smartvocab.data.model

import com.google.firebase.Timestamp

/**
 * Lớp dữ liệu lưu trữ cài đặt học tập và cấu hình nhận thông báo của người dùng.
 * Ánh xạ tới collection top-level 'user_settings'.
 */
data class LearningSettings(
    val id: String = "",
    val userId: String = "",
    val newWordsPerDay: Int = 10,
    val reviewWordsPerDay: Int = 20,
    val reminderTime: String = "20:00",
    @field:JvmField val dailyReminderEnabled: Boolean = true,
    @field:JvmField val dueReviewReminderEnabled: Boolean = true,
    @field:JvmField val pushNotificationEnabled: Boolean = true,
    val updatedAt: Timestamp? = null
)
