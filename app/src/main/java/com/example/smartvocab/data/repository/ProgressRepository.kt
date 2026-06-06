package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.ProgressSummary
import com.example.smartvocab.data.model.DailyActivity
import com.example.smartvocab.data.model.LearningSettings
import com.example.smartvocab.data.Achievement
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

/**
 * Lớp Repository kết nối trực tiếp với Firebase Firestore để xử lý các dữ liệu liên quan đến tiến trình học.
 * Sử dụng cấu trúc collection top-level và lọc theo userId.
 */
class ProgressRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Lấy tên người dùng từ Firestore để hiển thị lời chào cá nhân hóa.
     */
    suspend fun getUserName(userId: String): String {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            doc.getString("fullName") ?: "Học viên"
        } catch (e: Exception) {
            "Học viên"
        }
    }

    /**
     * Lấy dữ liệu tóm tắt tiến trình học tập của người dùng bằng cách tính toán động từ Firestore.
     * Không tự động seed dữ liệu mẫu. Nếu chưa có dữ liệu, trả về ProgressSummary trống.
     */
    suspend fun getProgressSummary(userId: String): ProgressSummary {
        return try {
            // 1. Lấy dữ liệu từ learning_progress
            val progressSnapshot = firestore.collection("learning_progress")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val totalWordsLearned = progressSnapshot.size()

            if (totalWordsLearned == 0) {
                // Trả về mặc định khi chưa có dữ liệu học tập
                return ProgressSummary(
                    totalWordsLearned = 0,
                    masteredWords = 0,
                    reviewDueWords = 0,
                    streakDays = 0,
                    accuracy = 0.0,
                    retentionRate = 0.0,
                    levelEstimate = "Beginner",
                    lastStudyDate = ""
                )
            }

            // 2. Tính số từ đã làm chủ (status = "MASTERED")
            val masteredWords = progressSnapshot.documents.count { doc ->
                doc.getString("status") == "MASTERED"
            }

            // 3. Tính số từ cần ôn tập (status = "REVIEW" hoặc nextReviewDate <= ngày hiện tại)
            val now = Timestamp.now()
            val reviewDueWords = progressSnapshot.documents.count { doc ->
                val status = doc.getString("status")
                val nextReviewDate = doc.getTimestamp("nextReviewDate")
                status == "REVIEW" || (nextReviewDate != null && nextReviewDate.seconds <= now.seconds)
            }

            // 4. Lấy dữ liệu từ daily_learning_plans để tính độ chính xác và số ngày streak
            val dailyPlansSnapshot = firestore.collection("daily_learning_plans")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val totalCorrect = dailyPlansSnapshot.documents.sumOf { it.getLong("correctAnswers") ?: 0L }
            val totalAnswers = dailyPlansSnapshot.documents.sumOf { it.getLong("totalAnswers") ?: 0L }
            
            // Độ chính xác = correctAnswers / totalAnswers * 100
            val accuracy = if (totalAnswers > 0) {
                (totalCorrect.toDouble() / totalAnswers * 100)
            } else {
                0.0
            }

            // Tỷ lệ ghi nhớ = masteredWords / totalWordsLearned * 100
            val retentionRate = if (totalWordsLearned > 0) {
                (masteredWords.toDouble() / totalWordsLearned * 100)
            } else {
                0.0
            }

            // StreakDays tính theo số lượng ngày có hoạt động thực tế (learnedWords > 0 hoặc reviewedWords > 0)
            val streakDays = dailyPlansSnapshot.documents.count { doc ->
                val learned = doc.getLong("learnedWords") ?: 0L
                val reviewed = doc.getLong("reviewedWords") ?: 0L
                learned > 0L || reviewed > 0L
            }

            // Ước lượng trình độ
            val levelEstimate = when {
                totalWordsLearned < 300 -> "Beginner"
                totalWordsLearned <= 1000 -> "Intermediate"
                else -> "Advanced"
            }

            // Lấy ngày học cuối cùng từ kế hoạch hàng ngày
            val lastStudyDate = dailyPlansSnapshot.documents
                .mapNotNull { it.getString("date") }
                .lastOrNull() ?: ""

            ProgressSummary(
                totalWordsLearned = totalWordsLearned,
                masteredWords = masteredWords,
                reviewDueWords = reviewDueWords,
                streakDays = streakDays,
                accuracy = accuracy,
                retentionRate = retentionRate,
                levelEstimate = levelEstimate,
                lastStudyDate = lastStudyDate
            )
        } catch (e: Exception) {
            ProgressSummary()
        }
    }

    /**
     * Lấy danh sách hoạt động học tập từ collection top-level daily_learning_plans.
     * Sắp xếp theo thứ tự thứ trong tuần từ T2 đến CN.
     * Không tự động seed dữ liệu mẫu.
     */
    suspend fun getDailyActivity(userId: String): List<DailyActivity> {
        return try {
            val snapshot = firestore.collection("daily_learning_plans")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            if (snapshot.isEmpty) {
                emptyList()
            } else {
                snapshot.toObjects(DailyActivity::class.java)
                    .sortedWith(compareBy { getDayOrder(it.date) })
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Lấy cấu hình học tập & thông báo của người dùng từ collection top-level user_settings.
     * Nếu chưa có cấu hình, trả về cài đặt mặc định cục bộ mà không tự động seed lên Firestore.
     */
    suspend fun getLearningSettings(userId: String): LearningSettings {
        return try {
            val query = firestore.collection("user_settings")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            if (query.isEmpty) {
                LearningSettings(userId = userId)
            } else {
                query.documents.first().toObject(LearningSettings::class.java) ?: LearningSettings(userId = userId)
            }
        } catch (e: Exception) {
            LearningSettings(userId = userId)
        }
    }

    /**
     * Cập nhật cấu hình cài đặt của người dùng lên collection top-level user_settings.
     */
    suspend fun updateLearningSettings(userId: String, settings: LearningSettings): Result<Unit> = runCatching {
        val query = firestore.collection("user_settings")
            .whereEqualTo("userId", userId)
            .get()
            .await()
        
        val docRef = if (!query.isEmpty) {
            query.documents.first().reference
        } else {
            firestore.collection("user_settings").document()
        }
        
        val finalSettings = settings.copy(id = docRef.id, userId = userId)
        docRef.set(finalSettings).await()
    }

    /**
     * Lấy danh sách thành tựu của người dùng từ collection top-level achievements.
     */
    suspend fun getAchievements(userId: String): List<Achievement> {
        return try {
            val snapshot = firestore.collection("achievements")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            if (snapshot.isEmpty) {
                emptyList()
            } else {
                snapshot.toObjects(Achievement::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Helper xác định thứ tự các thứ trong tuần để hiển thị biểu đồ từ Thứ 2 đến Chủ nhật.
     */
    private fun getDayOrder(day: String): Int {
        return when (day) {
            "T2" -> 1
            "T3" -> 2
            "T4" -> 3
            "T5" -> 4
            "T6" -> 5
            "T7" -> 6
            "CN" -> 7
            else -> 8
        }
    }
}
