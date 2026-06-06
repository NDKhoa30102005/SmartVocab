package com.example.smartvocab.data.model

data class VocabularyWord(
    val id: String = "",
    val setId: String = "",
    val term: String = "",
    val ipa: String = "",
    val partOfSpeech: String = "",
    val definition: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
    val synonyms: String = "",
    val antonyms: String = "",
    val collocations: String = "",
    val notes: String = "",
    @field:JvmField val isLearned: Boolean = false,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
