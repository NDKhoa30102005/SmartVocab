package com.example.smartvocab.data.repository

import com.example.smartvocab.data.model.VocabularySet
import com.example.smartvocab.data.model.VocabularyWord
import com.example.smartvocab.data.model.LearningProgress
import com.example.smartvocab.data.model.ReviewLog
import com.example.smartvocab.data.model.DailyActivity
import com.example.smartvocab.data.model.PracticeResult
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
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
    suspend fun updateFlashcardProgress(setId: String, wordId: String, rating: String): Result<Unit>
    suspend fun submitQuizAnswer(setId: String, wordId: String, isCorrect: Boolean): Result<Unit>
    suspend fun submitQuizResult(setId: String, score: Int, totalQuestions: Int, timeSpent: Int): Result<Unit>
    suspend fun seedDefaultData(userId: String): Result<Unit>
}

class FirestoreVocabularyRepository : VocabularyRepository {
    private val firestore = FirebaseFirestore.getInstance()

    override fun getVocabularySets(userId: String): Flow<List<VocabularySet>> = callbackFlow {
        var setsList = emptyList<VocabularySet>()
        var progressList = emptyList<LearningProgress>()

        fun updateCombinedSets() {
            val progressBySet = progressList.groupBy { it.setId }
            val combined = setsList.map { set ->
                val setProgress = progressBySet[set.id] ?: emptyList()
                val totalLearned = setProgress.count { it.status != "NEW" || it.repetitionCount > 0 }
                val ratio = if (set.wordCount > 0) totalLearned.toFloat() / set.wordCount else 0f
                set.copy(progress = ratio.coerceIn(0f, 1f))
            }
            trySend(combined)
        }

        val setsReg = firestore.collection("vocabulary_sets")
            .whereIn("userId", listOf(userId, "system"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    setsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(VocabularySet::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.createdAt }
                    updateCombinedSets()
                }
            }

        val progressReg = firestore.collection("learning_progress")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    progressList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(LearningProgress::class.java)?.copy(id = doc.id)
                    }
                    updateCombinedSets()
                }
            }

        awaitClose {
            setsReg.remove()
            progressReg.remove()
        }
    }

    override suspend fun addVocabularySet(set: VocabularySet): Result<Unit> = runCatching {
        val docRef = if (set.id.isBlank()) {
            firestore.collection("vocabulary_sets").document()
        } else {
            firestore.collection("vocabulary_sets").document(set.id)
        }
        val now = Timestamp.now()
        val finalSet = set.copy(
            id = docRef.id,
            createdAt = now,
            updatedAt = now
        )
        docRef.set(finalSet).await()
    }

    override suspend fun updateVocabularySet(set: VocabularySet): Result<Unit> = runCatching {
        val now = Timestamp.now()
        val finalSet = set.copy(updatedAt = now)
        firestore.collection("vocabulary_sets").document(set.id).set(finalSet).await()
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        var wordsList = emptyList<VocabularyWord>()
        var progressList = emptyList<LearningProgress>()

        fun updateCombinedList() {
            val progressMap = progressList.associateBy { it.wordId }
            val combined = wordsList.map { word ->
                val progressDoc = progressMap[word.id]
                val learned = progressDoc?.let { it.status != "NEW" || it.repetitionCount > 0 } ?: false
                word.copy(isLearned = learned)
            }
            trySend(combined)
        }

        val wordsReg = firestore.collection("vocabulary_words")
            .whereEqualTo("setId", setId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    wordsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(VocabularyWord::class.java)?.copy(id = doc.id)
                    }.sortedBy { it.createdAt }
                    updateCombinedList()
                }
            }

        val progressReg = firestore.collection("learning_progress")
            .whereEqualTo("userId", userId)
            .whereEqualTo("setId", setId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    progressList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(LearningProgress::class.java)?.copy(id = doc.id)
                    }
                    updateCombinedList()
                }
            }

        awaitClose {
            wordsReg.remove()
            progressReg.remove()
        }
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
            doc.toObject(VocabularySet::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getWordById(wordId: String): VocabularyWord? {
        return try {
            val doc = firestore.collection("vocabulary_words").document(wordId).get().await()
            doc.toObject(VocabularyWord::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun toggleWordLearned(setId: String, wordId: String, isLearned: Boolean): Result<Unit> = runCatching {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching
        val docId = "${userId}_$wordId"
        val progressRef = firestore.collection("learning_progress").document(docId)
        val setRef = firestore.collection("vocabulary_sets").document(setId)
        
        val now = Timestamp.now()
        val nextReview = Timestamp(now.seconds + 24 * 3600, 0)
        
        val setSnap = firestore.collection("vocabulary_sets").document(setId).get().await()
        val isSystemSet = setSnap.getString("userId") == "system"

        firestore.runTransaction { transaction ->
            if (isLearned) {
                val progress = LearningProgress(
                    id = docId,
                    userId = userId,
                    wordId = wordId,
                    setId = setId,
                    status = "REVIEW",
                    easeFactor = 2.5,
                    intervalDays = 1,
                    repetitionCount = 1,
                    correctCount = 1,
                    wrongCount = 0,
                    nextReviewDate = nextReview,
                    lastReviewedAt = now,
                    createdAt = now,
                    updatedAt = now
                )
                transaction.set(progressRef, progress)
            } else {
                val progress = LearningProgress(
                    id = docId,
                    userId = userId,
                    wordId = wordId,
                    setId = setId,
                    status = "NEW",
                    easeFactor = 2.5,
                    intervalDays = 0,
                    repetitionCount = 0,
                    correctCount = 0,
                    wrongCount = 0,
                    nextReviewDate = null,
                    lastReviewedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
                transaction.set(progressRef, progress)
            }
            if (!isSystemSet) {
                transaction.update(setRef, "lastStudiedAt", now)
            }
        }.await()

        ProgressRepository().updateAchievements(userId)
    }

    private suspend fun updateWordProgress(userId: String, setId: String, wordId: String, rating: String, activityType: String): Result<Unit> = runCatching {
        val docId = "${userId}_$wordId"
        val progressRef = firestore.collection("learning_progress").document(docId)
        
        val setSnap = firestore.collection("vocabulary_sets").document(setId).get().await()
        val isSystemSet = setSnap.getString("userId") == "system"
        val setRef = firestore.collection("vocabulary_sets").document(setId)
        
        val now = Timestamp.now()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val dailyDocId = "${userId}_$todayStr"
        val dailyRef = firestore.collection("daily_learning_plans").document(dailyDocId)
        
        firestore.runTransaction { transaction ->
            val progressSnap = transaction.get(progressRef)
            val currentProgress = if (progressSnap.exists()) {
                progressSnap.toObject(LearningProgress::class.java) ?: LearningProgress()
            } else {
                LearningProgress(
                    id = docId,
                    userId = userId,
                    wordId = wordId,
                    setId = setId,
                    status = "NEW",
                    easeFactor = 2.5,
                    intervalDays = 0,
                    repetitionCount = 0,
                    correctCount = 0,
                    wrongCount = 0,
                    createdAt = now,
                    updatedAt = now
                )
            }
            
            val isCorrect = rating == "good" || rating == "easy"
            val newCorrectCount = if (isCorrect) currentProgress.correctCount + 1 else currentProgress.correctCount
            val newWrongCount = if (!isCorrect) currentProgress.wrongCount + 1 else currentProgress.wrongCount
            
            val newRepetitionCount: Int
            val newEaseFactor: Double
            val newIntervalDays: Int
            
            if (isCorrect) {
                newRepetitionCount = currentProgress.repetitionCount + 1
                val q = when (rating) {
                    "good" -> 4
                    "easy" -> 5
                    else -> 4
                }
                val efDiff = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
                newEaseFactor = (currentProgress.easeFactor + efDiff).coerceAtLeast(1.3)
                newIntervalDays = when (newRepetitionCount) {
                    1 -> 1
                    2 -> 6
                    else -> Math.round(currentProgress.intervalDays * newEaseFactor).toInt().coerceAtLeast(1)
                }
            } else {
                newRepetitionCount = 0
                val q = when (rating) {
                    "hard" -> 3
                    "again" -> 1
                    else -> 3
                }
                val efDiff = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
                newEaseFactor = (currentProgress.easeFactor + efDiff).coerceAtLeast(1.3)
                newIntervalDays = 1
            }
            
            var newStatus = currentProgress.status
            if (currentProgress.status == "NEW") {
                newStatus = if (isCorrect) "REVIEW" else "LEARNING"
            } else if (currentProgress.status == "LEARNING") {
                if (isCorrect) newStatus = "REVIEW"
            } else if (currentProgress.status == "REVIEW") {
                if (!isCorrect) {
                    newStatus = "LEARNING"
                } else if (newRepetitionCount >= 4) {
                    newStatus = "MASTERED"
                }
            } else if (currentProgress.status == "MASTERED") {
                if (!isCorrect) newStatus = "LEARNING"
            }
            
            val nextReview = Timestamp(now.seconds + newIntervalDays * 24 * 3600, 0)
            
            val updatedProgress = currentProgress.copy(
                status = newStatus,
                easeFactor = newEaseFactor,
                intervalDays = newIntervalDays,
                repetitionCount = newRepetitionCount,
                correctCount = newCorrectCount,
                wrongCount = newWrongCount,
                nextReviewDate = nextReview,
                lastReviewedAt = now,
                updatedAt = now
            )
            
            transaction.set(progressRef, updatedProgress)
            
            val logRef = firestore.collection("review_logs").document()
            val reviewLog = ReviewLog(
                id = logRef.id,
                userId = userId,
                wordId = wordId,
                setId = setId,
                sessionId = "",
                activityType = activityType,
                rating = rating,
                isCorrect = isCorrect,
                reviewedAt = now
            )
            transaction.set(logRef, reviewLog)
            
            val dailySnap = transaction.get(dailyRef)
            val isNewWord = (currentProgress.status == "NEW")
            val isReviewWord = !isNewWord
            
            val learnedIncrement = if (isNewWord) 1 else 0
            val reviewedIncrement = if (isReviewWord) 1 else 0
            val correctIncrement = if (isCorrect) 1 else 0
            val wrongIncrement = if (!isCorrect) 1 else 0
            
            val dailyPlan = if (dailySnap.exists()) {
                val currentDaily = dailySnap.toObject(DailyActivity::class.java) ?: DailyActivity()
                currentDaily.copy(
                    learnedWords = currentDaily.learnedWords + learnedIncrement,
                    reviewedWords = currentDaily.reviewedWords + reviewedIncrement,
                    correctAnswers = currentDaily.correctAnswers + correctIncrement,
                    wrongAnswers = currentDaily.wrongAnswers + wrongIncrement,
                    totalAnswers = currentDaily.totalAnswers + 1,
                    updatedAt = now
                )
            } else {
                DailyActivity(
                    id = dailyDocId,
                    userId = userId,
                    date = todayStr,
                    learnedWords = learnedIncrement,
                    reviewedWords = reviewedIncrement,
                    correctAnswers = correctIncrement,
                    wrongAnswers = wrongIncrement,
                    totalAnswers = 1,
                    studyMinutes = 1,
                    createdAt = now,
                    updatedAt = now
                )
            }
            transaction.set(dailyRef, dailyPlan)
            
            if (!isSystemSet) {
                transaction.update(setRef, "lastStudiedAt", now)
            }
        }.await()
        
        ProgressRepository().updateAchievements(userId)
    }

    override suspend fun updateFlashcardProgress(setId: String, wordId: String, rating: String): Result<Unit> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        return updateWordProgress(userId, setId, wordId, rating, "REVIEW")
    }

    override suspend fun submitQuizAnswer(setId: String, wordId: String, isCorrect: Boolean): Result<Unit> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val rating = if (isCorrect) "good" else "again"
        return updateWordProgress(userId, setId, wordId, rating, "QUIZ")
    }

    override suspend fun submitQuizResult(setId: String, score: Int, totalQuestions: Int, timeSpent: Int): Result<Unit> = runCatching {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching
        val now = Timestamp.now()
        
        val docRef = firestore.collection("practice_results").document()
        val result = PracticeResult(
            id = docRef.id,
            userId = userId,
            setId = setId,
            type = "QUIZ",
            score = score,
            totalQuestions = totalQuestions,
            timeSpent = timeSpent,
            completedAt = now
        )
        docRef.set(result).await()
        
        ProgressRepository().updateAchievements(userId)
    }

    private suspend fun updateSetStats(setId: String) {
        try {
            val wordsSnapshot = firestore.collection("vocabulary_words")
                .whereEqualTo("setId", setId)
                .get()
                .await()
            val totalCount = wordsSnapshot.size()
            
            val setDoc = firestore.collection("vocabulary_sets").document(setId)
            val setSnap = setDoc.get().await()
            if (setSnap.exists() && setSnap.getString("userId") != "system") {
                setDoc.update(
                    "wordCount", totalCount,
                    "updatedAt", Timestamp.now()
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun seedDefaultData(userId: String): Result<Unit> = runCatching {
        val batch = firestore.batch()
        val now = Timestamp.now()
        
        // Define Sets
        val set1Id = "ielts_academic_$userId"
        val set1 = VocabularySet(
            id = set1Id,
            title = "Advanced IELTS",
            description = "Bộ từ vựng học thuật thiết yếu nâng cao cho mục tiêu IELTS 7.5+",
            category = "IELTS",
            wordCount = 5,
            progress = 0.4f,
            lastStudiedAt = Timestamp(now.seconds - 2 * 3600, 0),
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
            lastStudiedAt = Timestamp(now.seconds - 24 * 3600, 0),
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
            lastStudiedAt = Timestamp(now.seconds - 3 * 24 * 3600, 0),
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
            lastStudiedAt = null,
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
