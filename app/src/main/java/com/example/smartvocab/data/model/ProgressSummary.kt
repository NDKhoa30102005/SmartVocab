package com.example.smartvocab.data.model

/**
 * Lớp dữ liệu lưu trữ thông tin tóm tắt tiến trình học tập của người dùng.
 * Có giá trị mặc định để Firestore có thể tự động ánh xạ dữ liệu (deserialize).
 */
data class ProgressSummary(
    val totalWordsLearned: Int = 0,   // Tổng số từ đã học
    val masteredWords: Int = 0,       // Số từ đã làm chủ/ghi nhớ
    val reviewDueWords: Int = 0,      // Số từ cần ôn tập hôm nay
    val streakDays: Int = 0,          // Chuỗi số ngày học liên tiếp
    val accuracy: Double = 0.0,       // Độ chính xác trung bình (%)
    val retentionRate: Double = 0.0,   // Tỷ lệ ghi nhớ (%)
    val levelEstimate: String = "Beginner", // Ước lượng trình độ (Beginner/Intermediate/Advanced)
    val lastStudyDate: String = ""    // Ngày học cuối cùng (định dạng YYYY-MM-DD)
)
