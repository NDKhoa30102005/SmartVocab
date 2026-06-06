package com.example.smartvocab.data.model

/**
 * Lớp dữ liệu lưu trữ hoạt động học tập trong một ngày cụ thể (dùng vẽ biểu đồ).
 */
data class DailyActivity(
    val id: String = "",             // Mã định danh tài liệu
    val userId: String = "",         // Mã người dùng liên kết
    val date: String = "",           // Thứ trong tuần (ví dụ: "T2", "T3",..., "CN")
    val learnedWords: Int = 0,       // Số từ mới đã học trong ngày
    val reviewedWords: Int = 0,      // Số từ đã ôn tập trong ngày
    val correctAnswers: Int = 0,     // Số câu trả lời đúng
    val totalAnswers: Int = 0        // Tổng số câu trả lời trong ngày
)
