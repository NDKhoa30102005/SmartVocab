package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.AppNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp

/**
 * Lớp Repository kết nối trực tiếp với Firebase Firestore để quản lý danh sách thông báo.
 */
class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Lấy danh sách các thông báo của người dùng sắp xếp theo thời gian mới nhất.
     * Nếu không có thông báo nào, tự động seed danh sách thông báo mẫu lên Firestore.
     */
    suspend fun getNotifications(userId: String): List<AppNotification> {
        val collRef = firestore.collection("users")
            .document(userId)
            .collection("notifications")
        return try {
            val snapshot = collRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            if (snapshot.isEmpty) {
                // Seed dữ liệu mẫu nếu chưa có thông báo nào
                val mockData = getMockNotifications()
                for (notification in mockData) {
                    collRef.document(notification.id).set(notification).await()
                }
                mockData
            } else {
                snapshot.toObjects(AppNotification::class.java)
            }
        } catch (e: Exception) {
            getMockNotifications()
        }
    }

    /**
     * Đánh dấu đã đọc một thông báo cụ thể.
     */
    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> = runCatching {
        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
    }

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc (sử dụng Firestore Batch Write để tối ưu).
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> = runCatching {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("notifications")
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
     * Xóa một thông báo khỏi danh sách trên Firestore.
     */
    suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> = runCatching {
        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .document(notificationId)
            .delete()
            .await()
    }

    /**
     * Dữ liệu thông báo mẫu phục vụ demo.
     */
    private fun getMockNotifications(): List<AppNotification> {
        val now = Timestamp.now()
        return listOf(
            AppNotification(
                id = "n1",
                title = "Nhắc nhở ôn tập hôm nay",
                message = "Đã đến lúc ôn tập rồi! Có 15 từ vựng đang chờ bạn củng cố trí nhớ.",
                type = "REVIEW",
                isRead = false,
                createdAt = now
            ),
            AppNotification(
                id = "n2",
                title = "Thành tựu mới đạt được",
                message = "Chúc mừng! Bạn đã nhận được huy hiệu 'Vua từ vựng' khi hoàn thành học 300 từ.",
                type = "ACHIEVEMENT",
                isRead = true,
                createdAt = Timestamp(now.seconds - 7200, 0) // 2 giờ trước
            ),
            AppNotification(
                id = "n3",
                title = "Cập nhật hệ thống thành công",
                message = "Chúng tôi vừa tối ưu hóa danh sách từ vựng IELTS Academic và cập nhật thêm ví dụ thực tế.",
                type = "SYSTEM",
                isRead = true,
                createdAt = Timestamp(now.seconds - 86400, 0) // 1 ngày trước
            )
        )
    }
}
