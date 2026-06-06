package com.example.smartvocab.data.model

data class VocabularySet(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val wordCount: Int = 0,
    val progress: Float = 0f, // 0.0 to 1.0
    val lastStudied: String = "Chưa học",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
