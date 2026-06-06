package com.example.smartvocab.data.model

import com.google.firebase.Timestamp

/**
 * Lớp dữ liệu cho bộ từ vựng, ánh xạ tới collection 'vocabulary_sets'.
 */
data class VocabularySet(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val wordCount: Int = 0,
    val userId: String = "",
    val icon: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val lastStudiedAt: Timestamp? = null,
    val progress: Float = 0f // Tiến trình học của user đối với bộ từ
)
