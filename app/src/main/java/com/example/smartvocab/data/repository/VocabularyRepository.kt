package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.VocabularySet
import com.example.smartvocab.data.model.VocabularyWord
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Extension helper to await Firebase Task results asynchronously
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Firestore task failed"))
        }
    }
}

interface VocabularyRepository {
    fun getVocabularySets(userId: String): Flow<List<VocabularySet>>
    suspend fun addVocabularySet(set: VocabularySet): Result<Unit>
    suspend fun updateVocabularySet(set: VocabularySet): Result<Unit>
    suspend fun deleteVocabularySet(setId: String): Result<Unit>
    fun getWords(setId: String): Flow<List<VocabularyWord>>
    suspend fun addWord(word: VocabularyWord): Result<Unit>
    suspend fun updateWord(word: VocabularyWord): Result<Unit>
    suspend fun deleteWord(setId: String, wordId: String): Result<Unit>
    suspend fun getVocabularySetById(setId: String): VocabularySet?
    suspend fun getWordById(wordId: String): VocabularyWord?
    suspend fun toggleWordLearned(setId: String, wordId: String, isLearned: Boolean): Result<Unit>
    suspend fun seedDefaultData(userId: String): Result<Unit>
}

class FirestoreVocabularyRepository : VocabularyRepository {
    private val firestore = FirebaseFirestore.getInstance()

    override fun getVocabularySets(userId: String): Flow<List<VocabularySet>> = callbackFlow {
        val listener = firestore.collection("vocabulary_sets")
            .whereIn("userId", listOf(userId, "system"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sets = snapshot.toObjects(VocabularySet::class.java)
                        .sortedByDescending { it.createdAt }
                    trySend(sets)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addVocabularySet(set: VocabularySet): Result<Unit> = runCatching {
        val docRef = if (set.id.isBlank()) {
            firestore.collection("vocabulary_sets").document()
        } else {
            firestore.collection("vocabulary_sets").document(set.id)
        }
        val finalSet = set.copy(id = docRef.id)
        docRef.set(finalSet).await()
    }

    override suspend fun updateVocabularySet(set: VocabularySet): Result<Unit> = runCatching {
        firestore.collection("vocabulary_sets").document(set.id).set(set).await()
    }

    override suspend fun deleteVocabularySet(setId: String): Result<Unit> = runCatching {
        // Delete all words in this set first
        val wordsSnapshot = firestore.collection("vocabulary_words")
            .whereEqualTo("setId", setId)
            .get()
            .await()
        
        val batch = firestore.batch()
        for (doc in wordsSnapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()

        // Delete the set
        firestore.collection("vocabulary_sets").document(setId).delete().await()
    }

    override fun getWords(setId: String): Flow<List<VocabularyWord>> = callbackFlow {
        val listener = firestore.collection("vocabulary_words")
            .whereEqualTo("setId", setId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val words = snapshot.toObjects(VocabularyWord::class.java)
                        .sortedByDescending { it.createdAt }
                    trySend(words)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addWord(word: VocabularyWord): Result<Unit> = runCatching {
        val docRef = firestore.collection("vocabulary_words").document()
        val finalWord = word.copy(id = docRef.id)
        docRef.set(finalWord).await()
        updateSetStats(finalWord.setId)
    }

    override suspend fun updateWord(word: VocabularyWord): Result<Unit> = runCatching {
        firestore.collection("vocabulary_words").document(word.id).set(word).await()
        updateSetStats(word.setId)
    }

    override suspend fun deleteWord(setId: String, wordId: String): Result<Unit> = runCatching {
        firestore.collection("vocabulary_words").document(wordId).delete().await()
        updateSetStats(setId)
    }

    override suspend fun getVocabularySetById(setId: String): VocabularySet? {
        return try {
            val doc = firestore.collection("vocabulary_sets").document(setId).get().await()
            doc.toObject(VocabularySet::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getWordById(wordId: String): VocabularyWord? {
        return try {
            val doc = firestore.collection("vocabulary_words").document(wordId).get().await()
            doc.toObject(VocabularyWord::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun toggleWordLearned(setId: String, wordId: String, isLearned: Boolean): Result<Unit> = runCatching {
        firestore.collection("vocabulary_words").document(wordId)
            .update("isLearned", isLearned)
            .await()
        updateSetStats(setId)
    }

    private suspend fun updateSetStats(setId: String) {
        try {
            val wordsSnapshot = firestore.collection("vocabulary_words")
                .whereEqualTo("setId", setId)
                .get()
                .await()
            
            val words = wordsSnapshot.toObjects(VocabularyWord::class.java)
            val totalCount = words.size
            val learnedCount = words.count { it.isLearned }
            val progress = if (totalCount == 0) 0f else learnedCount.toFloat() / totalCount
            
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = "Học lần cuối: " + sdf.format(Date())

            firestore.collection("vocabulary_sets").document(setId)
                .update(
                    "wordCount", totalCount,
                    "progress", progress,
                    "lastStudied", dateStr
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun seedDefaultData(userId: String): Result<Unit> = runCatching {
        val batch = firestore.batch()
        
        // Define Sets
        val set1Id = "ielts_academic_$userId"
        val set1 = VocabularySet(
            id = set1Id,
            title = "Advanced IELTS",
            description = "Bộ từ vựng học thuật thiết yếu nâng cao cho mục tiêu IELTS 7.5+",
            category = "IELTS",
            wordCount = 5,
            progress = 0.4f,
            lastStudied = "Học lần cuối: 2 giờ trước",
            userId = userId
        )
        
        val set2Id = "tech_startup_$userId"
        val set2 = VocabularySet(
            id = set2Id,
            title = "Tech Startup Jargon",
            description = "Thuật ngữ tiếng Anh chuyên ngành công nghệ và khởi nghiệp",
            category = "Công nghệ",
            wordCount = 2,
            progress = 1.0f,
            lastStudied = "Học lần cuối: Hôm qua",
            userId = userId
        )
        
        val set3Id = "business_meetings_$userId"
        val set3 = VocabularySet(
            id = set3Id,
            title = "Business Meetings",
            description = "Từ vựng thông dụng trong các cuộc họp và thương thảo doanh nghiệp",
            category = "Kinh doanh",
            wordCount = 1,
            progress = 0.0f,
            lastStudied = "Học lần cuối: 3 ngày trước",
            userId = userId
        )
        
        val set4Id = "travel_essential_$userId"
        val set4 = VocabularySet(
            id = set4Id,
            title = "Du lịch bản xứ",
            description = "Những từ và cụm từ tiếng Anh giao tiếp khi đi du lịch nước ngoài",
            category = "Du lịch",
            wordCount = 0,
            progress = 0.0f,
            lastStudied = "Chưa học",
            userId = userId
        )
        
        // Set Documents
        batch.set(firestore.collection("vocabulary_sets").document(set1Id), set1)
        batch.set(firestore.collection("vocabulary_sets").document(set2Id), set2)
        batch.set(firestore.collection("vocabulary_sets").document(set3Id), set3)
        batch.set(firestore.collection("vocabulary_sets").document(set4Id), set4)
        
        // Define Words
        val words = listOf(
            // IELTS
            VocabularyWord(
                id = "w1_$userId",
                setId = set1Id,
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
                isLearned = true,
                userId = userId
            ),
            VocabularyWord(
                id = "w2_$userId",
                setId = set1Id,
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
                isLearned = false,
                userId = userId
            ),
            VocabularyWord(
                id = "w3_$userId",
                setId = set1Id,
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
                isLearned = false,
                userId = userId
            ),
            VocabularyWord(
                id = "w4_$userId",
                setId = set1Id,
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
                isLearned = true,
                userId = userId
            ),
            VocabularyWord(
                id = "w5_$userId",
                setId = set1Id,
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
                isLearned = false,
                userId = userId
            ),
            
            // Tech Startup
            VocabularyWord(
                id = "w101_$userId",
                setId = set2Id,
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
                isLearned = true,
                userId = userId
            ),
            VocabularyWord(
                id = "w102_$userId",
                setId = set2Id,
                term = "Scalable",
                ipa = "/ˈskeɪləbl/",
                partOfSpeech = "Tính từ",
                definition = "Có khả năng mở rộng (về quy mô hệ thống, mô hình) mà không làm tăng đáng kể chi phí.",
                example = "We need to build a scalable architecture that can support millions of users.",
                exampleTranslation = "Chúng ta cần xây dựng một kiến trúc có khả năng mở rộng để hỗ trợ hàng triệu người dùng.",
                synonyms = "Expandable",
                antonyms = "",
                collocations = "scalable business, highly scalable",
                notes = "",
                isLearned = true,
                userId = userId
            ),
            
            // Business Meetings
            VocabularyWord(
                id = "w201_$userId",
                setId = set3Id,
                term = "Consensus",
                ipa = "/kənˈsensəs/",
                partOfSpeech = "Danh từ",
                definition = "Sự đồng thuận chung, sự nhất trí ý kiến giữa mọi người trong nhóm.",
                example = "After hours of discussion, we finally reached a consensus.",
                exampleTranslation = "Sau nhiều giờ thảo luận, cuối cùng chúng tôi đã đạt được sự đồng thuận.",
                synonyms = "Agreement, Accord",
                antonyms = "Disagreement, Dissent",
                collocations = "reach a consensus, general consensus",
                notes = "",
                isLearned = false,
                userId = userId
            )
        )
        
        for (word in words) {
            batch.set(firestore.collection("vocabulary_words").document(word.id), word)
        }
        
        batch.commit().await()
    }
}
