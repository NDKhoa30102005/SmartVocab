package com.example.smartvocab.data

import com.example.smartvocab.data.model.VocabularySet
import com.example.smartvocab.data.model.VocabularyWord

enum class NotificationType {
    REVIEW, ACHIEVEMENT, SYSTEM
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val timeStamp: String,
    val isUnread: Boolean
)

data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "", // Material symbol icon name
    val progress: Float = 0f, // 0.0 to 1.0
    val currentVal: Int = 0,
    val targetVal: Int = 0,
    val isUnlocked: Boolean = false,
    val userId: String = ""
)

// Mock Data Singleton
object MockData {
    val categories = listOf("Tất cả", "IELTS", "TOEIC", "Kinh doanh", "Công nghệ", "Du lịch")

    val vocabularySets = mutableListOf<VocabularySet>()

    val words = mutableListOf<VocabularyWord>()

    val notifications = mutableListOf(
        AppNotification(
            id = "n1",
            type = NotificationType.REVIEW,
            title = "Nhắc nhở ôn tập hôm nay",
            body = "Đã đến lúc ôn tập rồi! Có 15 từ vựng đang chờ bạn củng cố trí nhớ.",
            timeStamp = "Vừa xong",
            isUnread = true
        ),
        AppNotification(
            id = "n2",
            type = NotificationType.ACHIEVEMENT,
            title = "Thành tựu mới đạt được",
            body = "Chúc mừng! Bạn đã nhận được huy hiệu 'Vua từ vựng' khi hoàn thành học 300 từ.",
            timeStamp = "2 giờ trước",
            isUnread = false
        ),
        AppNotification(
            id = "n3",
            type = NotificationType.SYSTEM,
            title = "Cập nhật hệ thống thành công",
            body = "Chúng tôi vừa tối ưu hóa danh sách từ vựng IELTS Academic và cập nhật thêm ví dụ thực tế.",
            timeStamp = "Hôm qua",
            isUnread = false
        )
    )

    val achievements = listOf(
        Achievement(
            id = "a1",
            title = "Vua từ vựng",
            description = "Học và làm chủ được 300 từ vựng mới",
            icon = "workspace_premium",
            progress = 1.0f,
            currentVal = 342,
            targetVal = 300,
            isUnlocked = true
        ),
        Achievement(
            id = "a2",
            title = "Người chăm chỉ",
            description = "Học liên tục trong vòng 30 ngày",
            icon = "emoji_events",
            progress = 0.40f,
            currentVal = 12,
            targetVal = 30,
            isUnlocked = false
        ),
        Achievement(
            id = "a3",
            title = "Bách phát bách trúng",
            description = "Đạt độ chính xác 100% trong bài luyện tập trắc nghiệm",
            icon = "task_alt",
            progress = 1.0f,
            currentVal = 10,
            targetVal = 10,
            isUnlocked = true
        )
    )
}
