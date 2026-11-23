package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KalamoraRepository
import com.example.data.PreferencesRepository
import com.example.data.friendlyFirestoreMessage
import com.example.data.model.Book
import com.example.data.model.Chapter
import com.example.data.model.DashboardStats
import com.example.data.model.RecentWorkItem
import com.example.data.model.SaveStatus
import com.example.data.model.Shayari
import com.example.data.model.TextStatsUtil
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val VM_TAG = "KalamoraVM"
private const val STOP_TIMEOUT_MS = 5000L

@OptIn(ExperimentalCoroutinesApi::class)
class KalamoraViewModel(
    private val repository: KalamoraRepository,
    private val preferences: PreferencesRepository,
    val currentUserId: String
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _chapterSaveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val chapterSaveStatus: StateFlow<SaveStatus> = _chapterSaveStatus.asStateFlow()

    private val _shayariSaveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val shayariSaveStatus: StateFlow<SaveStatus> = _shayariSaveStatus.asStateFlow()

    private val _activeOwnerBookId = MutableStateFlow<String?>(null)
    private val _activePublicBookId = MutableStateFlow<String?>(null)
    private val _activePublicShayariId = MutableStateFlow<String?>(null)

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    init {
        viewModelScope.launch {
            val firebaseUser = Firebase.auth.currentUser
            if (firebaseUser != null && firebaseUser.uid == currentUserId) {
                repository.ensureUserProfile(firebaseUser).onFailure { err ->
                    Log.w(VM_TAG, "Failed to ensure profile: ${err.message}")
                }
            }
        }
    }

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { profile ->
            if (profile != null) {
                preferences.setInterfaceLanguage(profile.preferredLanguage)
            }
            UiState.Success(profile)
        }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val myBooks: StateFlow<UiState<List<Book>>> = repository.observeUserBooks(currentUserId)
        .map<List<Book>, UiState<List<Book>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val myShayari: StateFlow<UiState<List<Shayari>>> = repository.observeUserShayari(currentUserId)
        .map<List<Shayari>, UiState<List<Shayari>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publicBooks: StateFlow<UiState<List<Book>>> = repository.observePublicBooks()
        .map<List<Book>, UiState<List<Book>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publicShayari: StateFlow<UiState<List<Shayari>>> = repository.observePublicShayari()
        .map<List<Shayari>, UiState<List<Shayari>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val dashboardStats: StateFlow<DashboardStats> = combine(myBooks, myShayari) { booksState, shayariState ->
        val books = (booksState as? UiState.Success)?.data.orEmpty()
        val shayari = (shayariState as? UiState.Success)?.data.orEmpty()
        val bookWords = books.sumOf { it.totalWordCount }
        val shayariWords = shayari.sumOf { it.wordCount }
        val publishedBooks = books.count { it.isPublicBook() }
        val publishedShayari = shayari.count { it.isPublicShayari() }
        val draftBooks = books.size - publishedBooks
        val draftShayari = shayari.size - publishedShayari
        DashboardStats(
            totalBooks = books.size,
            totalShayari = shayari.size,
            totalWords = bookWords + shayariWords,
            totalDrafts = draftBooks + draftShayari,
            totalPublished = publishedBooks + publishedShayari
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardStats())

    val recentWorks: StateFlow<List<RecentWorkItem>> = combine(myBooks, myShayari) { booksState, shayariState ->
        val books = (booksState as? UiState.Success)?.data.orEmpty().map { RecentWorkItem.BookItem(it) }
        val shayari = (shayariState as? UiState.Success)?.data.orEmpty().map { RecentWorkItem.ShayariItem(it) }
        (books + shayari)
            .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            .take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Active Book Studio Chapters (Owner)
    val activeOwnerChapters: StateFlow<UiState<List<Chapter>>> = _activeOwnerBookId
        .flatMapLatest { bookId ->
            if (bookId.isNullOrBlank()) {
                flowOf<UiState<List<Chapter>>>(UiState.Success(emptyList()))
            } else {
                repository.observeOwnerChapters(bookId, currentUserId)
                    .map<List<Chapter>, UiState<List<Chapter>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    // Public / Preview Reader Book & Chapters
    val activeReaderBook: StateFlow<UiState<Book?>> = _activePublicBookId
        .flatMapLatest { bookId ->
            if (bookId.isNullOrBlank()) {
                flowOf<UiState<Book?>>(UiState.Success(null))
            } else {
                repository.observeBook(bookId)
                    .map<Book?, UiState<Book?>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val activeReaderChapters: StateFlow<UiState<List<Chapter>>> = combine(_activePublicBookId, activeReaderBook) { bookId, bookState ->
        Pair(bookId, (bookState as? UiState.Success)?.data)
    }.flatMapLatest { (bookId, book) ->
        if (bookId.isNullOrBlank() || book == null) {
            flowOf<UiState<List<Chapter>>>(UiState.Success(emptyList()))
        } else {
            val chaptersFlow = if (book.ownerId == currentUserId) {
                repository.observeOwnerChapters(bookId, currentUserId)
            } else {
                repository.observePublicChapters(bookId)
            }
            chaptersFlow
                .map<List<Chapter>, UiState<List<Chapter>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val activeReaderShayari: StateFlow<UiState<Shayari?>> = _activePublicShayariId
        .flatMapLatest { shayariId ->
            if (shayariId.isNullOrBlank()) {
                flowOf<UiState<Shayari?>>(UiState.Success(null))
            } else {
                repository.observeShayariById(shayariId)
                    .map<Shayari?, UiState<Shayari?>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error(friendlyFirestoreMessage(firestoreError ?: error)))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun selectBookForStudio(bookId: String?) {
        _activeOwnerBookId.value = bookId
        _chapterSaveStatus.value = SaveStatus.Idle
    }

    fun selectBookForReader(bookId: String?) {
        _activePublicBookId.value = bookId
    }

    fun selectShayariForReader(shayariId: String?) {
        _activePublicShayariId.value = shayariId
    }

    fun markChapterUnsaved() {
        _chapterSaveStatus.value = SaveStatus.Unsaved
    }

    fun resetChapterSaveStatus() {
        _chapterSaveStatus.value = SaveStatus.Idle
    }

    fun markShayariUnsaved() {
        _shayariSaveStatus.value = SaveStatus.Unsaved
    }

    fun resetShayariSaveStatus() {
        _shayariSaveStatus.value = SaveStatus.Idle
    }

    // --- Book Operations ---

    fun createBook(
        title: String,
        subtitle: String?,
        authorName: String,
        description: String,
        category: String,
        language: String,
        coverTheme: String,
        coverURL: String?,
        makePublic: Boolean,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createBook(
                title = title,
                subtitle = subtitle,
                authorName = authorName,
                description = description,
                category = category,
                language = language,
                coverTheme = coverTheme,
                coverURL = coverURL,
                makePublic = makePublic
            )
            result.onSuccess { bookId ->
                onCreated(bookId)
            }.onFailure { err ->
                _toastMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    fun updateBookMetadata(
        bookId: String,
        title: String,
        subtitle: String?,
        authorName: String,
        description: String,
        category: String,
        language: String,
        coverTheme: String,
        coverURL: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateBookMetadata(
                bookId = bookId,
                title = title,
                subtitle = subtitle,
                authorName = authorName,
                description = description,
                category = category,
                language = language,
                coverTheme = coverTheme,
                coverURL = coverURL
            )
            result.onSuccess {
                _toastMessage.value = "Book details updated"
                onDone()
            }.onFailure { err ->
                _toastMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    fun setBookVisibility(bookId: String, makePublic: Boolean) {
        viewModelScope.launch {
            val result = repository.setBookVisibility(bookId, makePublic)
            result.onSuccess {
                _toastMessage.value = if (makePublic) {
                    "Book published to Public Library"
                } else {
                    "Book switched to Private Draft"
                }
            }.onFailure { err ->
                _toastMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    fun deleteBook(bookId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteBookWithChapters(bookId)
            result.onSuccess {
                _toastMessage.value = "Book deleted"
                onDeleted()
            }.onFailure { err ->
                _toastMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    // --- Chapter Operations ---

    fun addChapter(bookId: String, bookVisibility: String, title: String, onAdded: (String) -> Unit = {}) {
        viewModelScope.launch {
            val currentChapters = (activeOwnerChapters.value as? UiState.Success)?.data.orEmpty()
            val nextOrder = (currentChapters.maxOfOrNull { it.chapterOrder } ?: -1L) + 1L
            val newCount = (currentChapters.size + 1).toLong()
            val result = repository.createChapter(
                bookId = bookId,
                bookVisibility = bookVisibility,
                title = title,
                chapterOrder = nextOrder,
                newChapterCount = newCount
            )
            result.onSuccess { chId ->
                onAdded(chId)
            }.onFailure { err ->
                _toastMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    suspend fun saveChapterNow(
        bookId: String,
        chapterId: String,
        title: String,
        content: String
    ): Boolean {
        _chapterSaveStatus.value = SaveStatus.Saving
        val currentChapters = (activeOwnerChapters.value as? UiState.Success)?.data.orEmpty()
        val newWords = TextStatsUtil.countWords(content)
        val totalBookWords = currentChapters.sumOf { ch ->
            if (ch.id == chapterId) newWords else ch.wordCount
        }
        val result = repository.saveChapterContent(
            bookId = bookId,
            chapterId = chapterId,
            title = title,
            content = content,
            newBookTotalWordCount = totalBookWords
        )
        return if (result.isSuccess) {
            _chapterSaveStatus.value = SaveStatus.Saved(System.currentTimeMillis())
            true
        } else {
            val msg = friendlyFirestoreMessage(result.exceptionOrNull() ?: Exception("Save failed"))
            _chapterSaveStatus.value = SaveStatus.Error(msg)
            false
        }
    }

    fun reorderChapter(bookId: String, chapterId: String, moveUp: Boolean) {
        viewModelScope.launch {
            val list = (activeOwnerChapters.value as? UiState.Success)?.data.orEmpty().toMutableList()
            val index = list.indexOfFirst { it.id == chapterId }
            if (index == -1) return@launch
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex !in list.indices) return@launch
            val item = list.removeAt(index)
            list.add(targetIndex, item)
            repository.reorderChapters(bookId, list.map { it.id }).onFailure { err ->
                _toastMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    fun deleteChapter(bookId: String, chapterId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val currentChapters = (activeOwnerChapters.value as? UiState.Success)?.data.orEmpty()
            val remaining = currentChapters.filter { it.id != chapterId }
            val result = repository.deleteChapter(
                bookId = bookId,
                chapterId = chapterId,
                remainingChapterCount = remaining.size.toLong(),
                newBookTotalWordCount = remaining.sumOf { it.wordCount }
            )
            result.onSuccess {
                onDeleted()
            }.onFailure { err ->
                _toastMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    // --- Shayari Operations ---

    suspend fun saveShayariNow(
        existingId: String?,
        authorName: String,
        title: String,
        content: String,
        category: String,
        language: String,
        makePublic: Boolean
    ): Result<String> {
        _shayariSaveStatus.value = SaveStatus.Saving
        val result: Result<String> = if (existingId.isNullOrBlank()) {
            repository.createShayari(
                authorName = authorName,
                title = title,
                content = content,
                category = category,
                language = language,
                makePublic = makePublic
            )
        } else {
            repository.updateShayari(
                shayariId = existingId,
                authorName = authorName,
                title = title,
                content = content,
                category = category,
                language = language,
                makePublic = makePublic
            ).map { existingId }
        }
        return if (result.isSuccess) {
            _shayariSaveStatus.value = SaveStatus.Saved(System.currentTimeMillis())
            result
        } else {
            val msg = friendlyFirestoreMessage(result.exceptionOrNull() ?: Exception("Save failed"))
            _shayariSaveStatus.value = SaveStatus.Error(msg)
            result
        }
    }

    fun setShayariVisibility(shayariId: String, makePublic: Boolean) {
        viewModelScope.launch {
            repository.setShayariVisibility(shayariId, makePublic)
                .onSuccess {
                    _toastMessage.value = if (makePublic) {
                        "Shayari published to Public Library"
                    } else {
                        "Shayari switched to Private Draft"
                    }
                }
                .onFailure { err ->
                    _toastMessage.value = friendlyFirestoreMessage(err)
                }
        }
    }

    fun deleteShayari(shayariId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteShayari(shayariId)
                .onSuccess {
                    _toastMessage.value = "Shayari deleted"
                    onDeleted()
                }
                .onFailure { err ->
                    _toastMessage.value = friendlyFirestoreMessage(err)
                }
        }
    }

    // --- Profile & Settings ---

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String?,
        photoURL: String?,
        preferredLanguage: String,
        darkMode: Boolean
    ) {
        viewModelScope.launch {
            preferences.setInterfaceLanguage(preferredLanguage)
            repository.updateUserProfile(
                displayName = displayName,
                username = username,
                bio = bio,
                photoURL = photoURL,
                preferredLanguage = preferredLanguage,
                darkMode = darkMode
            ).onSuccess {
                _toastMessage.value = "Profile saved"
            }.onFailure { err ->
                _toastMessage.value = friendlyFirestoreMessage(err)
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setInterfaceLanguage(lang: String) {
        viewModelScope.launch {
            preferences.setInterfaceLanguage(lang)
            val profile = (userProfile.value as? UiState.Success)?.data
            if (profile != null) {
                repository.updateUserProfile(
                    displayName = profile.displayName.ifBlank { "Writer" },
                    username = profile.username.ifBlank { "writer_ink" },
                    bio = profile.bio,
                    photoURL = profile.photoURL,
                    preferredLanguage = lang,
                    darkMode = profile.darkMode
                )
            }
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isDeletingAccount.value = true
            val dataResult = repository.deleteAllUserData()
            if (dataResult.isFailure) {
                _isDeletingAccount.value = false
                _toastMessage.value = friendlyFirestoreMessage(dataResult.exceptionOrNull() ?: Exception("Delete failed"))
                return@launch
            }
            try {
                Firebase.auth.currentUser?.delete()?.await()
                _isDeletingAccount.value = false
                onComplete()
            } catch (e: Exception) {
                _isDeletingAccount.value = false
                // All Firestore content is already erased; sign out cleanly and notify if re-auth is required for auth record
                _toastMessage.value = "Your books, shayari, and profile data were erased. Please sign in again if needed to finalize Google auth removal."
                onComplete()
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
