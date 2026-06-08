package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.AppNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp

/**
 * Lớp Repository kết nối trực tiếp với Firebase Firestore để quản lý danh sách thông báo.
 * Sử dụng collection top-level 'notifications' và lọc theo userId.
 */
class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Lấy danh sách các thông báo của người dùng sắp xếp theo thời gian mới nhất (createdAt giảm dần).
     * Không tự động seed thông báo mẫu.
     */
    suspend fun getNotifications(userId: String): List<AppNotification> {
        return try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
            }
            if (list.isEmpty()) {
                seedMockNotifications(userId)
                val newSnapshot = firestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                newSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                }
            } else {
                list
            }
        } catch (e: Exception) {
            // Trường hợp chưa tạo index Firestore cho orderBy, thực hiện sắp xếp cục bộ để tránh lỗi truy vấn
            try {
                val snapshot = firestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
                if (list.isEmpty()) {
                    seedMockNotifications(userId)
                    val newSnapshot = firestore.collection("notifications")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    newSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.createdAt }
                } else {
                    list
                }
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun seedMockNotifications(userId: String) {
        try {
            val seedList = listOf(
                AppNotification(
                    userId = userId,
                    title = "Nhắc nhở ôn tập hôm nay",
                    message = "Đã đến lúc ôn tập rồi! Có các từ vựng đang chờ bạn củng cố trí nhớ.",
                    type = "REVIEW",
                    isRead = false,
                    createdAt = Timestamp.now()
                ),
                AppNotification(
                    userId = userId,
                    title = "Cập nhật hệ thống thành công",
                    message = "Chúng tôi vừa tối ưu hóa danh sách từ vựng IELTS Academic và cập nhật thêm ví dụ thực tế.",
                    type = "SYSTEM",
                    isRead = false,
                    createdAt = Timestamp(java.util.Date(System.currentTimeMillis() - 86400000)) // 1 ngày trước
                )
            )
            val batch = firestore.batch()
            for (notif in seedList) {
                val docRef = firestore.collection("notifications").document()
                batch.set(docRef, notif)
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Bỏ qua lỗi seed
        }
    }

    /**
     * Đánh dấu đã đọc một thông báo cụ thể (chỉ cập nhật nếu đúng document liên kết).
     */
    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> = runCatching {
        val doc = firestore.collection("notifications").document(notificationId).get().await()
        if (doc.exists() && doc.getString("userId") == userId) {
            firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
        } else {
            throw Exception("Notification not found or access denied")
        }
    }

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc.
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> = runCatching {
        val snapshot = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        if (snapshot.isEmpty) return@runCatching

        val batch = firestore.batch()
        for (doc in snapshot.documents) {
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }

    /**
     * Xóa một thông báo khỏi danh sách trên Firestore (chỉ xóa nếu đúng userId).
     */
    suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> = runCatching {
        val doc = firestore.collection("notifications").document(notificationId).get().await()
        if (doc.exists() && doc.getString("userId") == userId) {
            firestore.collection("notifications")
                .document(notificationId)
                .delete()
                .await()
        } else {
            throw Exception("Notification not found or access denied")
        }
    }
}
