package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.ProgressSummary
import com.example.smartvocab.data.model.DailyActivity
import com.example.smartvocab.data.model.LearningSettings
import com.example.smartvocab.data.model.LearningProgress
import com.example.smartvocab.data.model.ReviewLog
import com.example.smartvocab.data.Achievement
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Lớp Repository kết nối trực tiếp với Firebase Firestore để xử lý các dữ liệu liên quan đến tiến trình học.
 * Sử dụng cấu trúc collection top-level và lọc theo userId.
 */
class ProgressRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Helper tính chuỗi streak liên tiếp từ danh sách các ngày học yyyy-MM-dd
     */
    fun calculateStreak(activeDates: List<String>): Int {
        if (activeDates.isEmpty()) return 0
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDates = activeDates.mapNotNull { 
            try { sdf.parse(it) } catch(e: Exception) { null } 
        }.distinct().sortedDescending()
        
        if (parsedDates.isEmpty()) return 0
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DATE, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val firstDate = parsedDates[0]
        val daysDiffToday = ((today.time - firstDate.time) / (1000 * 60 * 60 * 24)).toInt()
        
        if (daysDiffToday > 1) {
            return 0
        }
        
        var streak = 1
        var currentDate = firstDate
        
        for (i in 1 until parsedDates.size) {
            val nextDate = parsedDates[i]
            val diffInDays = ((currentDate.time - nextDate.time) / (1000 * 60 * 60 * 24)).toInt()
            if (diffInDays == 1) {
                streak++
                currentDate = nextDate
            } else if (diffInDays > 1) {
                break
            }
        }
        return streak
    }

    /**
     * Cập nhật thành tựu dựa trên dữ liệu thật của người dùng
     */
    suspend fun updateAchievements(userId: String) {
        try {
            val setsSnapshot = firestore.collection("vocabulary_sets")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val customSetsCount = setsSnapshot.size()
            val firstSetUnlocked = customSetsCount >= 1
            
            val dailyPlansSnapshot = firestore.collection("daily_learning_plans")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val activeDates = dailyPlansSnapshot.documents.filter { doc ->
                val learned = doc.getLong("learnedWords") ?: 0L
                val reviewed = doc.getLong("reviewedWords") ?: 0L
                val answers = doc.getLong("totalAnswers") ?: 0L
                learned > 0L || reviewed > 0L || answers > 0L
            }.mapNotNull { it.getString("date") }
            val streak = calculateStreak(activeDates)
            
            val progressSnapshot = firestore.collection("learning_progress")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "MASTERED")
                .get()
                .await()
            val masteredCount = progressSnapshot.size()
            
            val practiceResultsSnapshot = firestore.collection("practice_results")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val hasPerfectQuiz = practiceResultsSnapshot.documents.any { doc ->
                val score = doc.getLong("score") ?: 0L
                val total = doc.getLong("totalQuestions") ?: 0L
                score > 0L && score == total
            }
            
            val maxLearnedInDay = dailyPlansSnapshot.documents.mapNotNull { it.getLong("learnedWords")?.toInt() }.maxOrNull() ?: 0
            val speedLearnerUnlocked = maxLearnedInDay >= 50
            
            val batch = firestore.batch()
            
            val list = listOf(
                Achievement(
                    id = "first_set",
                    title = "Nhà sáng tạo",
                    description = "Mở khóa khi tạo ít nhất một bộ từ vựng cá nhân",
                    icon = "workspace_premium",
                    progress = if (firstSetUnlocked) 1f else 0f,
                    currentVal = customSetsCount,
                    targetVal = 1,
                    isUnlocked = firstSetUnlocked,
                    userId = userId
                ),
                Achievement(
                    id = "streak_7",
                    title = "Bền bỉ 7 ngày",
                    description = "Đạt chuỗi học tập liên tiếp 7 ngày",
                    icon = "local_fire_department",
                    progress = (streak.toFloat() / 7f).coerceIn(0f, 1f),
                    currentVal = streak,
                    targetVal = 7,
                    isUnlocked = streak >= 7,
                    userId = userId
                ),
                Achievement(
                    id = "streak_30",
                    title = "Chăm chỉ 30 ngày",
                    description = "Đạt chuỗi học tập liên tiếp 30 ngày",
                    icon = "emoji_events",
                    progress = (streak.toFloat() / 30f).coerceIn(0f, 1f),
                    currentVal = streak,
                    targetVal = 30,
                    isUnlocked = streak >= 30,
                    userId = userId
                ),
                Achievement(
                    id = "word_master",
                    title = "Vua từ vựng",
                    description = "Học và làm chủ 300 từ",
                    icon = "workspace_premium",
                    progress = (masteredCount.toFloat() / 300f).coerceIn(0f, 1f),
                    currentVal = masteredCount,
                    targetVal = 300,
                    isUnlocked = masteredCount >= 300,
                    userId = userId
                ),
                Achievement(
                    id = "perfect_quiz",
                    title = "Bách phát bách trúng",
                    description = "Đạt độ chính xác 100% trong bài trắc nghiệm",
                    icon = "task_alt",
                    progress = if (hasPerfectQuiz) 1f else 0f,
                    currentVal = if (hasPerfectQuiz) 1 else 0,
                    targetVal = 1,
                    isUnlocked = hasPerfectQuiz,
                    userId = userId
                ),
                Achievement(
                    id = "speed_learner",
                    title = "Học siêu tốc",
                    description = "Học từ mới nhiều hơn hoặc bằng 50 từ trong một ngày",
                    icon = "bolt",
                    progress = (maxLearnedInDay.toFloat() / 50f).coerceIn(0f, 1f),
                    currentVal = maxLearnedInDay,
                    targetVal = 50,
                    isUnlocked = speedLearnerUnlocked,
                    userId = userId
                )
            )
            
            for (ach in list) {
                val docRef = firestore.collection("achievements").document("${userId}_${ach.id}")
                batch.set(docRef, ach)
            }
            
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
            val progressSnapshot = firestore.collection("learning_progress")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val learningProgressList = progressSnapshot.documents.mapNotNull { doc ->
                doc.toObject(LearningProgress::class.java)?.copy(id = doc.id)
            }
            val totalWordsLearned = learningProgressList.count { it.status != "NEW" || it.repetitionCount > 0 }

            if (totalWordsLearned == 0) {
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

            val masteredWords = learningProgressList.count { it.status == "MASTERED" }

            val now = Timestamp.now()
            val reviewDueWords = learningProgressList.count { 
                it.status == "REVIEW" && it.nextReviewDate != null && it.nextReviewDate.seconds <= now.seconds 
            }

            val dailyPlansSnapshot = firestore.collection("daily_learning_plans")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val totalCorrect = dailyPlansSnapshot.documents.sumOf { it.getLong("correctAnswers") ?: 0L }
            val totalAnswers = dailyPlansSnapshot.documents.sumOf { it.getLong("totalAnswers") ?: 0L }
            
            val accuracy = if (totalAnswers > 0) {
                (totalCorrect.toDouble() / totalAnswers * 100)
            } else {
                0.0
            }

            val reviewLogsSnapshot = firestore.collection("review_logs")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val reviewLogs = reviewLogsSnapshot.documents.filter { doc ->
                val activityType = doc.getString("activityType")
                activityType == null || activityType == "REVIEW"
            }
            val totalReviews = reviewLogs.size
            val goodEasyReviews = reviewLogs.count { doc ->
                val rating = doc.getString("rating")
                rating == "good" || rating == "easy"
            }
            val retentionRate = if (totalReviews > 0) {
                (goodEasyReviews.toDouble() / totalReviews * 100)
            } else {
                0.0
            }

            val activeDates = dailyPlansSnapshot.documents.filter { doc ->
                val learned = doc.getLong("learnedWords") ?: 0L
                val reviewed = doc.getLong("reviewedWords") ?: 0L
                val answers = doc.getLong("totalAnswers") ?: 0L
                learned > 0L || reviewed > 0L || answers > 0L
            }.mapNotNull { it.getString("date") }
            val streakDays = calculateStreak(activeDates)

            val levelEstimate = when {
                totalWordsLearned >= 1000 && accuracy >= 80.0 -> "Advanced"
                totalWordsLearned >= 300 && accuracy >= 60.0 -> "Intermediate"
                else -> "Beginner"
            }

            val lastStudyDate = dailyPlansSnapshot.documents
                .mapNotNull { it.getString("date") }
                .sortedDescending()
                .firstOrNull() ?: ""

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
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(DailyActivity::class.java)?.copy(id = doc.id)
                }.sortedBy { it.date }
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
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Achievement::class.java)?.copy(id = doc.id)
                }
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
