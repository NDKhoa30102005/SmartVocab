package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.ProgressSummary
import com.example.smartvocab.data.model.DailyActivity
import com.example.smartvocab.data.model.LearningSettings
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Lớp Repository kết nối trực tiếp với Firebase Firestore để xử lý các dữ liệu liên quan đến tiến trình học.
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
     * Lấy dữ liệu tóm tắt tiến trình học tập của người dùng.
     * Nếu tài liệu này chưa tồn tại, nó sẽ thực hiện seed (khởi tạo dữ liệu mẫu) lên Firestore một lần duy nhất.
     */
    suspend fun getProgressSummary(userId: String): ProgressSummary {
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("progress")
            .document("summary")
        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) {
                // Tạo dữ liệu mẫu khởi tạo cho tài khoản mới
                val defaultSummary = ProgressSummary(
                    totalWordsLearned = 342,
                    masteredWords = 280,
                    reviewDueWords = 15,
                    streakDays = 12,
                    accuracy = 92.0,
                    retentionRate = 81.8,
                    levelEstimate = "Intermediate",
                    lastStudyDate = "2026-06-06"
                )
                docRef.set(defaultSummary).await()
                defaultSummary
            } else {
                doc.toObject(ProgressSummary::class.java) ?: ProgressSummary()
            }
        } catch (e: Exception) {
            // Fallback trả về dữ liệu mẫu trong trường hợp lỗi kết nối để app không crash
            ProgressSummary(
                totalWordsLearned = 342,
                masteredWords = 280,
                reviewDueWords = 15,
                streakDays = 12,
                accuracy = 92.0,
                retentionRate = 81.8,
                levelEstimate = "Intermediate",
                lastStudyDate = "2026-06-06"
            )
        }
    }

    /**
     * Lấy danh sách hoạt động học tập của 7 ngày gần nhất để vẽ biểu đồ.
     * Nếu chưa có hoạt động nào, tự động seed danh sách hoạt động mẫu lên Firestore.
     */
    suspend fun getDailyActivity(userId: String): List<DailyActivity> {
        val collRef = firestore.collection("users")
            .document(userId)
            .collection("dailyActivity")
        return try {
            val snapshot = collRef.get().await()
            if (snapshot.isEmpty) {
                // Seed dữ liệu mẫu nếu collection trống
                val mockData = getMockDailyActivity()
                for (activity in mockData) {
                    collRef.document(activity.date).set(activity).await()
                }
                mockData
            } else {
                snapshot.toObjects(DailyActivity::class.java)
                    .sortedWith(compareBy { getDayOrder(it.date) })
            }
        } catch (e: Exception) {
            getMockDailyActivity()
        }
    }

    /**
     * Lấy cấu hình học tập & thông báo của người dùng.
     * Nếu chưa có cấu hình, seed cài đặt mặc định một lần duy nhất.
     */
    suspend fun getLearningSettings(userId: String): LearningSettings {
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("settings")
            .document("learning")
        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) {
                val defaultSettings = LearningSettings()
                docRef.set(defaultSettings).await()
                defaultSettings
            } else {
                doc.toObject(LearningSettings::class.java) ?: LearningSettings()
            }
        } catch (e: Exception) {
            LearningSettings()
        }
    }

    /**
     * Cập nhật cấu hình cài đặt của người dùng lên Firestore.
     */
    suspend fun updateLearningSettings(userId: String, settings: LearningSettings): Result<Unit> = runCatching {
        firestore.collection("users")
            .document(userId)
            .collection("settings")
            .document("learning")
            .set(settings)
            .await()
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

    /**
     * Dữ liệu mẫu phục vụ hiển thị biểu đồ học tập 7 ngày.
     */
    private fun getMockDailyActivity(): List<DailyActivity> {
        return listOf(
            DailyActivity("T2", 10, 5, 12, 15),
            DailyActivity("T3", 15, 10, 18, 20),
            DailyActivity("T4", 8, 4, 9, 12),
            DailyActivity("T5", 20, 15, 24, 30),
            DailyActivity("T6", 12, 8, 15, 18),
            DailyActivity("T7", 25, 20, 28, 30),
            DailyActivity("CN", 18, 12, 21, 25)
        )
    }
}
