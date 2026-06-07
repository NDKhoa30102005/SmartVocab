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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val progressMutex = Mutex()

    override fun getVocabularySets(userId: String): Flow<List<VocabularySet>> = callbackFlow {
        var setsList = emptyList<VocabularySet>()
        var progressList = emptyList<LearningProgress>()

        fun updateCombinedSets() {
            val progressBySet = progressList.groupBy { it.setId }
            val combined = setsList.map { set ->
                val setProgress = progressBySet[set.id] ?: emptyList()
                val totalLearned = setProgress.count { it.status != "NEW" || it.repetitionCount > 0 }
                val ratio = if (set.wordCount > 0) totalLearned.toFloat() / set.wordCount else 0f
                set.copy(
                    progress = ratio.coerceIn(0f, 1f)
                )
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
        val wordsSnapshot = firestore.collection("vocabulary_words")
            .whereEqualTo("setId", setId)
            .get()
            .await()
        
        val batch = firestore.batch()
        for (doc in wordsSnapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()

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

    private suspend fun updateWordProgress(
        userId: String,
        setId: String,
        wordId: String,
        isLearned: Boolean,
        activityType: String,
        rating: String? = null
    ): Result<Unit> = progressMutex.withLock {
        runCatching {
            val now = Timestamp.now()
            val progressDocId = "${userId}_$wordId"
            val progressRef = firestore.collection("learning_progress").document(progressDocId)
            val progressSnap = progressRef.get().await()

            val currentProgress = if (progressSnap.exists()) {
                progressSnap.toObject(LearningProgress::class.java) ?: LearningProgress()
            } else {
                LearningProgress(
                    id = progressDocId,
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

            val isNewWord = currentProgress.status == "NEW"

            val isCorrect = rating == null || rating == "good" || rating == "easy"
            val newCorrectCount = if (isCorrect) currentProgress.correctCount + 1 else currentProgress.correctCount
            val newWrongCount = if (!isCorrect) currentProgress.wrongCount + 1 else currentProgress.wrongCount

            var newRepetitionCount = currentProgress.repetitionCount
            var newEaseFactor = currentProgress.easeFactor
            var newIntervalDays = currentProgress.intervalDays

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
                updatedAt = now,
                createdAt = if (isNewWord) now else currentProgress.createdAt
            )

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val dailyDocId = "${userId}_$todayStr"
            val dailyRef = firestore.collection("daily_learning_plans").document(dailyDocId)
            val dailySnap = dailyRef.get().await()

            val learnedIncrement = if (isNewWord) 1 else 0
            val reviewedIncrement = if (!isNewWord) 1 else 0
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

            val batch = firestore.batch()
            batch.set(progressRef, updatedProgress)
            batch.set(dailyRef, dailyPlan)

            val logRef = firestore.collection("review_logs").document()
            val reviewLog = ReviewLog(
                id = logRef.id,
                userId = userId,
                wordId = wordId,
                setId = setId,
                sessionId = "",
                activityType = activityType,
                rating = rating ?: (if (isLearned) "good" else "again"),
                isCorrect = isCorrect,
                reviewedAt = now
            )
            batch.set(logRef, reviewLog)

            val setRef = firestore.collection("vocabulary_sets").document(setId)
            val setSnap = setRef.get().await()
            if (setSnap.exists() && setSnap.getString("userId") != "system") {
                batch.update(setRef, "lastStudiedAt", now)
            }

            batch.commit().await()
            // ProgressRepository().updateAchievements(userId)
            Unit
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
        // ProgressRepository().updateAchievements(userId)
    }

    override suspend fun updateFlashcardProgress(setId: String, wordId: String, rating: String): Result<Unit> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val isLearned = rating == "good" || rating == "easy"
        return updateWordProgress(userId, setId, wordId, isLearned = isLearned, activityType = "REVIEW", rating = rating)
    }

    override suspend fun submitQuizAnswer(setId: String, wordId: String, isCorrect: Boolean): Result<Unit> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        return updateWordProgress(userId, setId, wordId, isLearned = isCorrect, activityType = "QUIZ", rating = if (isCorrect) "good" else "again")
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
        
        // ProgressRepository().updateAchievements(userId)
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
            
            val batch = firestore.batch()
            
            if (setSnap.exists() && setSnap.getString("userId") != "system") {
                batch.update(setDoc, 
                    "wordCount", totalCount,
                    "updatedAt", Timestamp.now()
                )
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun seedDefaultData(userId: String): Result<Unit> = runCatching {
        // 1. Tải các bộ từ vựng hệ thống (system sets)
        val systemSetsSnap = firestore.collection("vocabulary_sets")
            .whereEqualTo("userId", "system")
            .get()
            .await()

        val batch = firestore.batch()
        val now = Timestamp.now()

        for (doc in systemSetsSnap.documents) {
            val systemSet = doc.toObject(VocabularySet::class.java) ?: continue
            val systemSetId = doc.id
            val newUserSetId = "${systemSetId}_$userId"

            // Tạo bản sao bộ từ cho người dùng
            val newUserSet = systemSet.copy(
                id = newUserSetId,
                userId = userId,
                createdAt = now,
                updatedAt = now,
                lastStudiedAt = null,
                progress = 0f
            )
            batch.set(firestore.collection("vocabulary_sets").document(newUserSetId), newUserSet)

            // 2. Tải tất cả từ vựng thuộc bộ từ hệ thống này
            val wordsSnap = firestore.collection("vocabulary_words")
                .whereEqualTo("setId", systemSetId)
                .get()
                .await()

            for (wordDoc in wordsSnap.documents) {
                val systemWord = wordDoc.toObject(VocabularyWord::class.java) ?: continue
                val newWordId = "${wordDoc.id}_$userId"
                val newWord = systemWord.copy(
                    id = newWordId,
                    setId = newUserSetId,
                    userId = userId
                )
                batch.set(firestore.collection("vocabulary_words").document(newWordId), newWord)
            }
        }

        // Thực hiện ghi batch
        batch.commit().await()
    }
}
