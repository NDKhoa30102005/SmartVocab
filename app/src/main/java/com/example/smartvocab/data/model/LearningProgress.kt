package com.example.smartvocab.data.model

import com.google.firebase.Timestamp

/**
 * Lớp dữ liệu lưu trữ tiến trình học tập của một từ vựng cụ thể.
 * Ánh xạ tới collection top-level 'learning_progress'.
 */
data class LearningProgress(
    val id: String = "",                 // Mã định danh tiến trình
    val userId: String = "",             // Mã người dùng liên kết
    val wordId: String = "",             // Mã từ vựng liên kết
    val status: String = "NEW",          // Trạng thái từ vựng ("NEW", "REVIEW", "MASTERED")
    val easeFactor: Double = 2.5,        // Hệ số dễ (phục vụ thuật toán Spaced Repetition)
    val intervalDays: Int = 0,           // Số ngày chờ đến lần ôn tập tiếp theo
    val repetitionCount: Int = 0,        // Số lần lặp lại học/ôn tập từ này
    val nextReviewDate: Timestamp? = null, // Thời gian ôn tập tiếp theo
    val lastReviewedAt: Timestamp? = null  // Lần ôn tập cuối cùng
)
