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
}

class FirestoreVocabularyRepository : VocabularyRepository {
    private val firestore = FirebaseFirestore.getInstance()

    override fun getVocabularySets(userId: String): Flow<List<VocabularySet>> = callbackFlow {
        val listener = firestore.collection("vocabulary_sets")
            .whereEqualTo("userId", userId)
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
        val docRef = firestore.collection("vocabulary_sets").document()
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
}
