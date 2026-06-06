package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.AppNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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
            snapshot.toObjects(AppNotification::class.java)
        } catch (e: Exception) {
            // Trường hợp chưa tạo index Firestore cho orderBy, thực hiện sắp xếp cục bộ để tránh lỗi truy vấn
            try {
                val snapshot = firestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                snapshot.toObjects(AppNotification::class.java)
                    .sortedByDescending { it.createdAt }
            } catch (ex: Exception) {
                emptyList()
            }
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
