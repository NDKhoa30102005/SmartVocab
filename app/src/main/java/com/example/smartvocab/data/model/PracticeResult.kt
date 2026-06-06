package com.example.smartvocab.data.model

import com.google.firebase.Timestamp

/**
 * Lớp dữ liệu lưu trữ kết quả luyện tập của người dùng (khi hoàn thành bài kiểm tra).
 * Ánh xạ tới collection top-level 'practice_results'.
 */
data class PracticeResult(
    val id: String = "",
    val userId: String = "",
    val setId: String = "",
    val type: String = "QUIZ",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val timeSpent: Int = 0, // Tính theo giây
    val completedAt: Timestamp? = null
)
