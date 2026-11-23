package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val photoURL: String? = null,
    val bio: String? = null,
    val preferredLanguage: String = "en",
    val darkMode: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Book(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val description: String = "",
    val authorName: String = "",
    val category: String = "Fiction",
    val language: String = "English",
    val coverURL: String? = null,
    val coverTheme: String = "indigo_gold",
    val visibility: String = "private",
    val status: String = "draft",
    val chapterCount: Long = 0L,
    val totalWordCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val publishedAt: Timestamp? = null
) {
    fun isPublicBook(): Boolean = visibility == "public" && status == "published"
}

@Keep
@IgnoreExtraProperties
data class Chapter(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val bookId: String = "",
    val bookVisibility: String = "private",
    val title: String = "",
    val content: String = "",
    val chapterOrder: Long = 0L,
    val wordCount: Long = 0L,
    val characterCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Shayari(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val authorName: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "Love",
    val language: String = "Hindi",
    val visibility: String = "private",
    val status: String = "draft",
    val wordCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val publishedAt: Timestamp? = null
) {
    fun isPublicShayari(): Boolean = visibility == "public" && status == "published"
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface SaveStatus {
    data object Idle : SaveStatus
    data object Unsaved : SaveStatus
    data object Saving : SaveStatus
    data class Saved(val timestampMs: Long) : SaveStatus
    data class Error(val message: String) : SaveStatus
}

enum class LibraryFilter { ALL, DRAFTS, PUBLISHED, RECENT }

enum class SortOption { RECENTLY_EDITED, TITLE_AZ, NEWEST }

data class DashboardStats(
    val totalBooks: Int = 0,
    val totalShayari: Int = 0,
    val totalWords: Long = 0L,
    val totalDrafts: Int = 0,
    val totalPublished: Int = 0
)

sealed interface RecentWorkItem {
    val id: String
    val title: String
    val subtitle: String
    val updatedAt: Timestamp?
    val isPublic: Boolean
    val wordCount: Long

    data class BookItem(val book: Book) : RecentWorkItem {
        override val id: String = book.id
        override val title: String = book.title
        override val subtitle: String = "${book.category} • ${book.chapterCount} ch"
        override val updatedAt: Timestamp? = book.updatedAt ?: book.createdAt
        override val isPublic: Boolean = book.isPublicBook()
        override val wordCount: Long = book.totalWordCount
    }

    data class ShayariItem(val shayari: Shayari) : RecentWorkItem {
        override val id: String = shayari.id
        override val title: String = shayari.title
        override val subtitle: String = "${shayari.category} • ${shayari.language}"
        override val updatedAt: Timestamp? = shayari.updatedAt ?: shayari.createdAt
        override val isPublic: Boolean = shayari.isPublicShayari()
        override val wordCount: Long = shayari.wordCount
    }
}

object KalamoraCatalog {
    val bookCategories = listOf(
        "Fiction", "Non-fiction", "Romance", "Mystery", "Biography",
        "Travel", "History", "Adventure", "Self-help", "Philosophy", "Other"
    )

    val shayariCategories = listOf(
        "Love", "Sadness", "Friendship", "Life", "Motivation",
        "Patriotism", "Loneliness", "Nature", "Philosophy", "Other"
    )

    val writingLanguages = listOf(
        "English", "Hindi", "Hinglish", "Urdu", "Sanskrit",
        "Marathi", "Bengali", "Punjabi", "Gujarati", "Tamil", "Telugu", "Other"
    )
}

object TextStatsUtil {
    private val whitespaceRegex = Regex("\\s+")

    fun countWords(text: String): Long {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0L
        return trimmed.split(whitespaceRegex).count { token ->
            token.any { ch -> ch.isLetterOrDigit() || ch in '\u0900'..'\u097F' }
        }.toLong()
    }

    fun countCharacters(text: String): Long = text.length.toLong()
}
