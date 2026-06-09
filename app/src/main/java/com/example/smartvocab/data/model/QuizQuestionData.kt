package com.example.smartvocab.data.model

/**
 * Lớp dữ liệu hỗ trợ lưu cấu trúc một câu hỏi trắc nghiệm Quiz.
 */
data class QuizQuestionData(
    val word: VocabularyWord,
    val options: List<String>,
    val correctIndex: Int
)
