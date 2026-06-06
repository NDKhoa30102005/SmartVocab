package com.example.smartvocab.data.model

import com.google.firebase.Timestamp

/**
 * Lớp dữ liệu lưu trữ hoạt động học tập trong một ngày cụ thể.
 * Ánh xạ tới collection top-level 'daily_learning_plans'.
 */
data class DailyActivity(
    val id: String = "",
    val userId: String = "",
    val date: String = "", // Định dạng yyyy-MM-dd
    val learnedWords: Int = 0,
    val reviewedWords: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val totalAnswers: Int = 0,
    val studyMinutes: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
