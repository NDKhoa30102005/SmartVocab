package com.example.smartvocab.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvocab.data.model.VocabularySet
import com.example.smartvocab.data.model.VocabularyWord
import com.example.smartvocab.data.repository.FirestoreVocabularyRepository
import com.example.smartvocab.data.repository.VocabularyRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VocabSetDetailViewModel : ViewModel() {
    private val repository: VocabularyRepository = FirestoreVocabularyRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Real-time Detail & Words lists
    private val _vocabSet = mutableStateOf<VocabularySet?>(null)
    val vocabSet: State<VocabularySet?> = _vocabSet

    private val _words = mutableStateOf<List<VocabularyWord>>(emptyList())
    val words: State<List<VocabularyWord>> = _words

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private var setListener: ListenerRegistration? = null
    private var wordsJob: Job? = null

    // Form inputs for Add/Edit Word
    val term = mutableStateOf("")
    val ipa = mutableStateOf("")
    val partOfSpeech = mutableStateOf("Danh từ")
    val definition = mutableStateOf("")
    val example = mutableStateOf("")
    val exampleTranslation = mutableStateOf("")
    val synonyms = mutableStateOf("")
    val antonyms = mutableStateOf("")
    val collocations = mutableStateOf("")
    val notes = mutableStateOf("")

    private val _isSavingWord = mutableStateOf(false)
    val isSavingWord: State<Boolean> = _isSavingWord

    private val _saveWordError = mutableStateOf<String?>(null)
    val saveWordError: State<String?> = _saveWordError

    private val _isWordSaved = mutableStateOf(false)
    val isWordSaved: State<Boolean> = _isWordSaved

    private var editingWordId: String? = null
    private var parentSetId: String = ""
    val isWordEditMode: Boolean
        get() = editingWordId != null

    fun loadSetDetails(setId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        // Observe vocabulary set document changes in real-time
        setListener?.remove()
        setListener = firestore.collection("vocabulary_sets").document(setId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = "Lỗi khi tải bộ từ: ${error.localizedMessage}"
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _vocabSet.value = snapshot.toObject(VocabularySet::class.java)
                }
            }

        // Observe vocabulary words collection changes in real-time
        wordsJob?.cancel()
        wordsJob = viewModelScope.launch {
            try {
                repository.getWords(setId).collect { wordList ->
                    _words.value = wordList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Lỗi khi tải danh sách từ: ${e.localizedMessage}"
            }
        }
    }

    fun toggleWordLearned(setId: String, wordId: String, isLearned: Boolean) {
        viewModelScope.launch {
            repository.toggleWordLearned(setId, wordId, isLearned)
        }
    }

    fun deleteWord(setId: String, wordId: String) {
        viewModelScope.launch {
            repository.deleteWord(setId, wordId)
        }
    }

    // Add / Edit Word logical methods
    fun initializeWordForm(setId: String?, wordId: String?) {
        resetWordForm()
        parentSetId = setId ?: "ielts_academic"
        if (wordId == null || wordId == "null" || wordId.isBlank()) return
        editingWordId = wordId
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val word = repository.getWordById(wordId)
                if (word != null) {
                    term.value = word.term
                    ipa.value = word.ipa
                    partOfSpeech.value = word.partOfSpeech
                    definition.value = word.definition
                    example.value = word.example
                    exampleTranslation.value = word.exampleTranslation
                    synonyms.value = word.synonyms
                    antonyms.value = word.antonyms
                    collocations.value = word.collocations
                    notes.value = word.notes
                    parentSetId = word.setId
                }
            } catch (e: Exception) {
                _saveWordError.value = "Lỗi khi tải từ vựng: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateExampleAuto() {
        if (term.value.isBlank()) {
            _saveWordError.value = "Vui lòng nhập từ vựng trước khi tự động tạo câu ví dụ"
            return
        }
        _saveWordError.value = null
        val wordStr = term.value.trim()
        
        val sentencePair = when (wordStr.lowercase()) {
            "achieve" -> Pair("They worked hard to achieve their goal of studying abroad.", "Họ đã nỗ lực làm việc để đạt được mục tiêu đi du học của mình.")
            "elicit" -> Pair("The teacher's question was designed to elicit responses from students.", "Câu hỏi của giáo viên được thiết kế để khơi gợi ý kiến từ học sinh.")
            "profound" -> Pair("The implications of this discovery are profound.", "Tác động của phát hiện này là vô cùng sâu sắc.")
            "ubiquitous" -> Pair("Smartphones have become ubiquitous in modern society.", "Điện thoại thông minh đã trở nên phổ biến ở khắp mọi nơi trong xã hội hiện đại.")
            "ephemeral" -> Pair("Fame in the internet age is often ephemeral.", "Sự nổi tiếng trong thời đại internet thường rất phù du, chóng tàn.")
            else -> Pair("We should learn how to use this $wordStr in daily conversation.", "Chúng ta nên học cách sử dụng từ '$wordStr' này trong giao tiếp hàng ngày.")
        }
        
        example.value = sentencePair.first
        exampleTranslation.value = sentencePair.second
    }

    fun saveWord() {
        if (term.value.isBlank()) {
            _saveWordError.value = "Từ vựng không được để trống"
            return
        }
        if (definition.value.isBlank()) {
            _saveWordError.value = "Nghĩa tiếng Việt không được để trống"
            return
        }
        val userId = auth.currentUser?.uid ?: return

        _isSavingWord.value = true
        _saveWordError.value = null

        viewModelScope.launch {
            val result = if (editingWordId == null) {
                val newWord = VocabularyWord(
                    setId = parentSetId,
                    term = term.value.trim(),
                    ipa = ipa.value.trim(),
                    partOfSpeech = partOfSpeech.value,
                    definition = definition.value.trim(),
                    example = example.value.trim(),
                    exampleTranslation = exampleTranslation.value.trim(),
                    synonyms = synonyms.value.trim(),
                    antonyms = antonyms.value.trim(),
                    collocations = collocations.value.trim(),
                    notes = notes.value.trim(),
                    isLearned = false,
                    userId = userId
                )
                repository.addWord(newWord)
            } else {
                val original = repository.getWordById(editingWordId!!)
                if (original != null) {
                    val updatedWord = original.copy(
                        term = term.value.trim(),
                        ipa = ipa.value.trim(),
                        partOfSpeech = partOfSpeech.value,
                        definition = definition.value.trim(),
                        example = example.value.trim(),
                        exampleTranslation = exampleTranslation.value.trim(),
                        synonyms = synonyms.value.trim(),
                        antonyms = antonyms.value.trim(),
                        collocations = collocations.value.trim(),
                        notes = notes.value.trim()
                    )
                    repository.updateWord(updatedWord)
                } else {
                    Result.failure(Exception("Không tìm thấy từ vựng để cập nhật"))
                }
            }

            _isSavingWord.value = false
            if (result.isSuccess) {
                _isWordSaved.value = true
            } else {
                _saveWordError.value = result.exceptionOrNull()?.localizedMessage ?: "Lưu thất bại"
            }
        }
    }

    fun resetWordForm() {
        term.value = ""
        ipa.value = ""
        partOfSpeech.value = "Danh từ"
        definition.value = ""
        example.value = ""
        exampleTranslation.value = ""
        synonyms.value = ""
        antonyms.value = ""
        collocations.value = ""
        notes.value = ""
        _isSavingWord.value = false
        _saveWordError.value = null
        _isWordSaved.value = false
        editingWordId = null
    }

    override fun onCleared() {
        super.onCleared()
        setListener?.remove()
        wordsJob?.cancel()
    }
}
