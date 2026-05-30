package com.example.smartvocab.data

// Models definition
data class VocabularySet(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val wordCount: Int,
    val progress: Float, // 0.0 to 1.0
    val lastStudied: String
)

data class Word(
    val id: String,
    val setId: String,
    val term: String,
    val ipa: String,
    val partOfSpeech: String, // e.g., "Danh từ", "Động từ", "Tính từ"
    val definition: String,
    val example: String,
    val exampleTranslation: String,
    val synonyms: String = "",
    val antonyms: String = "",
    val collocations: String = "",
    val notes: String = "",
    var isLearned: Boolean = false
)

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
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // Material symbol icon name
    val progress: Float, // 0.0 to 1.0
    val currentVal: Int,
    val targetVal: Int,
    val isUnlocked: Boolean
)

// Mock Data Singleton
object MockData {
    val categories = listOf("Tất cả", "IELTS", "TOEIC", "Kinh doanh", "Công nghệ", "Du lịch")

    val vocabularySets = mutableListOf(
        VocabularySet(
            id = "ielts_academic",
            title = "Advanced IELTS",
            description = "Bộ từ vựng học thuật thiết yếu nâng cao cho mục tiêu IELTS 7.5+",
            category = "IELTS",
            wordCount = 120,
            progress = 0.60f,
            lastStudied = "Học lần cuối: 2 giờ trước"
        ),
        VocabularySet(
            id = "tech_startup",
            title = "Tech Startup Jargon",
            description = "Thuật ngữ tiếng Anh chuyên ngành công nghệ và khởi nghiệp",
            category = "Công nghệ",
            wordCount = 45,
            progress = 0.85f,
            lastStudied = "Học lần cuối: Hôm qua"
        ),
        VocabularySet(
            id = "business_meetings",
            title = "Business Meetings",
            description = "Từ vựng thông dụng trong các cuộc họp và thương thảo doanh nghiệp",
            category = "Kinh doanh",
            wordCount = 60,
            progress = 0.10f,
            lastStudied = "Học lần cuối: 3 ngày trước"
        ),
        VocabularySet(
            id = "travel_essential",
            title = "Du lịch bản xứ",
            description = "Những từ và cụm từ tiếng Anh giao tiếp khi đi du lịch nước ngoài",
            category = "Du lịch",
            wordCount = 50,
            progress = 0.0f,
            lastStudied = "Chưa học"
        )
    )

    val words = mutableListOf(
        // Words for ielts_academic
        Word(
            id = "w1",
            setId = "ielts_academic",
            term = "Achieve",
            ipa = "/əˈtʃiːv/",
            partOfSpeech = "Động từ",
            definition = "Đạt được, giành được (mục tiêu, kết quả) nhờ nỗ lực, kỹ năng hoặc sự kiên trì.",
            example = "She worked hard to achieve her goal of studying abroad.",
            exampleTranslation = "Cô ấy đã làm việc chăm chỉ để đạt được mục tiêu đi du học của mình.",
            synonyms = "Attain, Accomplish",
            antonyms = "Fail, Lose",
            collocations = "achieve a goal, achieve success",
            notes = "Từ khóa quan trọng trong phần nói và viết học thuật.",
            isLearned = true
        ),
        Word(
            id = "w2",
            setId = "ielts_academic",
            term = "Elicit",
            ipa = "/ɪˈlɪsɪt/",
            partOfSpeech = "Động từ",
            definition = "Gợi ra, khơi gợi (một phản ứng, câu trả lời hoặc sự thật) từ ai đó.",
            example = "The teacher's question was designed to elicit responses from students.",
            exampleTranslation = "Câu hỏi của giáo viên được thiết kế để gợi mở phản hồi từ học sinh.",
            synonyms = "Evoke, Extract",
            antonyms = "Suppress, Hide",
            collocations = "elicit a response, elicit information",
            notes = "Thường đi kèm với tân ngữ chỉ thông tin hoặc phản ứng cảm xúc.",
            isLearned = false
        ),
        Word(
            id = "w3",
            setId = "ielts_academic",
            term = "Profound",
            ipa = "/prəˈfaʊnd/",
            partOfSpeech = "Tính từ",
            definition = "Sâu sắc, uyên thâm, có ảnh hưởng cực kỳ lớn hoặc sâu rộng.",
            example = "The implications of this discovery are profound.",
            exampleTranslation = "Những tác động của khám phá này là vô cùng sâu sắc.",
            synonyms = "Deep, Significant",
            antonyms = "Superficial, Mild",
            collocations = "profound effect, profound influence",
            notes = "Có thể dùng để miêu tả cảm xúc hoặc tri thức sâu sắc.",
            isLearned = false
        ),
        Word(
            id = "w4",
            setId = "ielts_academic",
            term = "Ubiquitous",
            ipa = "/juːˈbɪkwɪtəs/",
            partOfSpeech = "Tính từ",
            definition = "Có mặt ở khắp mọi nơi cùng một lúc, vô cùng phổ biến.",
            example = "Smartphones have become ubiquitous in modern society.",
            exampleTranslation = "Điện thoại thông minh đã trở nên phổ biến ở khắp mọi nơi trong xã hội hiện đại.",
            synonyms = "Omnipresent, Widespread",
            antonyms = "Rare, Scarce",
            collocations = "ubiquitous presence, become ubiquitous",
            notes = "Xuất phát từ tiếng Latin 'ubique' nghĩa là 'ở mọi nơi'.",
            isLearned = true
        ),
        Word(
            id = "w5",
            setId = "ielts_academic",
            term = "Ephemeral",
            ipa = "/ɪˈfem(ə)rəl/",
            partOfSpeech = "Tính từ",
            definition = "Phù du, chóng tàn, chỉ tồn tại trong một thời gian cực kỳ ngắn ngủi.",
            example = "Fame in the internet age is often ephemeral.",
            exampleTranslation = "Sự nổi tiếng trong thời đại internet thường rất phù du chóng tàn.",
            synonyms = "Transient, Fleeting",
            antonyms = "Permanent, Eternal",
            collocations = "ephemeral nature, ephemeral glory",
            notes = "Thường dùng để mô tả cái đẹp, thời tiết, hoặc danh vọng ngắn ngủi.",
            isLearned = false
        ),
        
        // Words for tech_startup
        Word(
            id = "w101",
            setId = "tech_startup",
            term = "Pivot",
            ipa = "/ˈpɪvət/",
            partOfSpeech = "Danh từ / Động từ",
            definition = "Bước chuyển hướng chiến lược (trong kinh doanh) khi hướng đi cũ không hiệu quả.",
            example = "The company decided to pivot to a software-as-a-service model.",
            exampleTranslation = "Công ty quyết định chuyển hướng sang mô hình phần mềm dưới dạng dịch vụ.",
            synonyms = "Shift, Turn",
            antonyms = "Persist, Stay",
            collocations = "make a pivot, strategic pivot",
            notes = "Thuật ngữ khởi nghiệp cực kỳ phổ biến của Silicon Valley.",
            isLearned = true
        ),
        Word(
            id = "w102",
            setId = "tech_startup",
            term = "Scalable",
            ipa = "/ˈskeɪləbl/",
            partOfSpeech = "Tính từ",
            definition = "Có khả năng mở rộng (về quy mô hệ thống, mô hình) mà không làm tăng đáng kể chi phí.",
            example = "We need to build a scalable architecture that can support millions of users.",
            exampleTranslation = "Chúng ta cần xây dựng một kiến trúc có khả năng mở rộng để hỗ trợ hàng triệu người dùng.",
            synonyms = "Expandable",
            collocations = "scalable business, highly scalable",
            isLearned = true
        ),
        
        // Words for business_meetings
        Word(
            id = "w201",
            setId = "business_meetings",
            term = "Consensus",
            ipa = "/kənˈsensəs/",
            partOfSpeech = "Danh từ",
            definition = "Sự đồng thuận chung, sự nhất trí ý kiến giữa mọi người trong nhóm.",
            example = "After hours of discussion, we finally reached a consensus.",
            exampleTranslation = "Sau nhiều giờ thảo luận, cuối cùng chúng tôi đã đạt được sự đồng thuận.",
            synonyms = "Agreement, Accord",
            antonyms = "Disagreement, Dissent",
            collocations = "reach a consensus, general consensus",
            isLearned = false
        )
    )

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
