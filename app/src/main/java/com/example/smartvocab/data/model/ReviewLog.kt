package com.example.smartvocab.data.model

import com.google.firebase.Timestamp

/**
 * Lớp dữ liệu lưu trữ nhật ký ôn tập của từng từ vựng.
 * Ánh xạ tới collection top-level 'review_logs'.
 */
data class ReviewLog(
    val id: String = "",
    val userId: String = "",
    val wordId: String = "",
    val setId: String = "",
    val sessionId: String = "",
    val activityType: String = "REVIEW", // LEARN, REVIEW, QUIZ, LISTENING, FILL_BLANK
    val rating: String = "",            // again, hard, good, easy
    @field:JvmField val isCorrect: Boolean = false,
    val reviewedAt: Timestamp? = null
)
