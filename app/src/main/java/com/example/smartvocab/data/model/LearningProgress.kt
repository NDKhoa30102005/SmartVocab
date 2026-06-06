package com.example.smartvocab.data.model

import com.google.firebase.Timestamp

/**
 * Lớp dữ liệu lưu trữ tiến trình học tập của một từ vựng cụ thể cho một người dùng.
 * Ánh xạ tới collection top-level 'learning_progress'.
 */
data class LearningProgress(
    val id: String = "",                     // Mã định danh dạng {userId}_{wordId}
    val userId: String = "",                 // Mã người dùng
    val wordId: String = "",                 // Mã từ vựng
    val setId: String = "",                  // Mã bộ từ vựng
    val status: String = "NEW",              // Trạng thái ("NEW", "LEARNING", "REVIEW", "MASTERED")
    val easeFactor: Double = 2.5,            // Hệ số dễ (Spaced Repetition)
    val intervalDays: Int = 0,               // Số ngày khoảng cách
    val repetitionCount: Int = 0,            // Số lần lặp lại
    val correctCount: Int = 0,               // Số lần trả lời đúng
    val wrongCount: Int = 0,                 // Số lần trả lời sai
    val nextReviewDate: Timestamp? = null,   // Thời gian ôn tập tiếp theo
    val lastReviewedAt: Timestamp? = null,   // Lần ôn tập cuối cùng
    val createdAt: Timestamp? = null,        // Thời gian tạo
    val updatedAt: Timestamp? = null         // Thời gian cập nhật
)
