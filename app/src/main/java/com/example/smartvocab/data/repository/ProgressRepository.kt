package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.ProgressSummary
import com.example.smartvocab.data.model.DailyActivity
import com.example.smartvocab.data.model.LearningSettings
import com.example.smartvocab.data.model.LearningProgress
import com.example.smartvocab.data.model.ReviewLog
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
        val daysDiffToday = Math.round((today.time - firstDate.time).toDouble() / (1000 * 60 * 60 * 24)).toInt()
        
        if (daysDiffToday > 1) {
            return 0
        }
        
        var streak = 1
        var currentDate = firstDate
        
        for (i in 1 until parsedDates.size) {
            val nextDate = parsedDates[i]
            val diffInDays = Math.round((currentDate.time - nextDate.time).toDouble() / (1000 * 60 * 60 * 24)).toInt()
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

            val reviewDueWords = learningProgressList.count { 
                it.status != "MASTERED" && (it.status != "NEW" || it.repetitionCount > 0)
            }

            // Lấy danh sách hoạt động đã được chuẩn hóa và sửa sai lệch
            val dailyActivities = getDailyActivity(userId, learningProgressList)

            val totalCorrect = dailyActivities.sumOf { it.correctAnswers.toLong() }
            val totalAnswers = dailyActivities.sumOf { it.totalAnswers.toLong() }
            
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

            val activeDates = dailyActivities.filter { doc ->
                doc.learnedWords > 0 || doc.reviewedWords > 0 || doc.totalAnswers > 0
            }.map { it.date }
            val streakDays = calculateStreak(activeDates)

            val levelEstimate = when {
                totalWordsLearned >= 1000 && accuracy >= 80.0 -> "Advanced"
                totalWordsLearned >= 300 && accuracy >= 60.0 -> "Intermediate"
                else -> "Beginner"
            }

            val lastStudyDate = dailyActivities
                .map { it.date }
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
     * Tự động kiểm tra và sửa sai lệch (self-healing) dựa trên danh sách tiến trình học tập.
     */
    suspend fun getDailyActivity(userId: String): List<DailyActivity> {
        return try {
            val progressSnapshot = firestore.collection("learning_progress")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val progressList = progressSnapshot.documents.mapNotNull { doc ->
                doc.toObject(LearningProgress::class.java)?.copy(id = doc.id)
            }
            getDailyActivity(userId, progressList)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDailyActivity(userId: String, progressList: List<LearningProgress>): List<DailyActivity> {
        val rawActivities = try {
            val snapshot = firestore.collection("daily_learning_plans")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            if (snapshot.isEmpty) {
                emptyList()
            } else {
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(DailyActivity::class.java)?.copy(id = doc.id)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
        return syncAndCorrectDailyActivities(userId, progressList, rawActivities)
    }

    private suspend fun syncAndCorrectDailyActivities(
        userId: String,
        progressList: List<LearningProgress>,
        activities: List<DailyActivity>
    ): List<DailyActivity> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        // 1. Tính toán số từ học thực tế theo ngày từ learning_progress
        val learnedByDate = progressList
            .filter { it.status != "NEW" || it.repetitionCount > 0 }
            .groupBy { 
                val ts = it.createdAt ?: it.lastReviewedAt ?: it.updatedAt
                if (ts != null) sdf.format(ts.toDate()) else ""
            }
            .filterKeys { it.isNotEmpty() }
            .mapValues { it.value.size }

        // 2. Chuyển đổi danh sách hoạt động hiện tại sang Map
        val activityMap = activities.associateBy { it.date }.toMutableMap()

        // Đảm bảo hoạt động hôm nay luôn có trong Map để tự động sửa hoặc khởi tạo nếu chưa có
        if (!activityMap.containsKey(todayStr)) {
            activityMap[todayStr] = DailyActivity(
                id = "${userId}_$todayStr",
                userId = userId,
                date = todayStr,
                learnedWords = 0,
                reviewedWords = 0,
                totalAnswers = 0,
                correctAnswers = 0,
                wrongAnswers = 0,
                studyMinutes = 0,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }

        val updatedActivities = mutableListOf<DailyActivity>()
        val batch = firestore.batch()
        var batchNeeded = false

        for ((date, activity) in activityMap) {
            val correctLearned = learnedByDate[date] ?: 0
            
            // Nếu phát hiện sai lệch về số lượng từ đã học
            if (activity.learnedWords != correctLearned) {
                val correctedActivity = activity.copy(
                    learnedWords = correctLearned,
                    updatedAt = Timestamp.now()
                )
                updatedActivities.add(correctedActivity)
                
                val docRef = firestore.collection("daily_learning_plans").document(correctedActivity.id.ifBlank { "${userId}_$date" })
                batch.set(docRef, correctedActivity)
                batchNeeded = true
            } else {
                updatedActivities.add(activity)
            }
        }

        if (batchNeeded) {
            try {
                batch.commit().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return updatedActivities.sortedBy { it.date }
    }

    /**
     * Lấy cấu hình học tập & thông báo của người dùng từ collection top-level user_settings.
     * Nếu chưa có cấu hình, trả về cài đặt mặc định cục bộ mà không tự động seed lên Firestore.
     */
    suspend fun getLearningSettings(userId: String): LearningSettings {
        return try {
            val doc = firestore.collection("user_settings")
                .document(userId)
                .get()
                .await()
            if (doc.exists()) {
                doc.toObject(LearningSettings::class.java) ?: LearningSettings(userId = userId)
            } else {
                LearningSettings(userId = userId)
            }
        } catch (e: Exception) {
            LearningSettings(userId = userId)
        }
    }

    /**
     * Cập nhật cấu hình cài đặt của người dùng lên collection top-level user_settings.
     */
    suspend fun updateLearningSettings(userId: String, settings: LearningSettings): Result<Unit> = runCatching {
        val docRef = firestore.collection("user_settings").document(userId)
        val finalSettings = settings.copy(id = userId, userId = userId)
        docRef.set(finalSettings).await()
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
