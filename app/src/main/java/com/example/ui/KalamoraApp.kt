package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.KalamoraRepository
import com.example.data.PreferencesRepository
import com.example.data.model.Book
import com.example.data.model.Shayari
import com.example.data.model.UiState
import com.example.ui.auth.WelcomeAuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.stringsForLanguage
import com.example.ui.screens.BookReaderScreen
import com.example.ui.screens.BookWritingStudioScreen
import com.example.ui.screens.CreateEditBookDialog
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MyBooksScreen
import com.example.ui.screens.MyShayariScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.PublicLibraryScreen
import com.example.ui.screens.ShayariReaderScreen
import com.example.ui.screens.ShayariStudioScreen
import com.example.ui.theme.KalamoraTheme
import com.example.ui.viewmodel.KalamoraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class MainTab { HOME, MY_BOOKS, MY_SHAYARI, PROFILE }

sealed interface KalamoraRoute {
    data object MainTabs : KalamoraRoute
    data class BookStudio(val bookId: String) : KalamoraRoute
    data class ShayariStudio(val shayariId: String?) : KalamoraRoute
    data object PublicLibrary : KalamoraRoute
    data class BookReader(val bookId: String, val returnTo: KalamoraRoute) : KalamoraRoute
    data class ShayariReader(val shayariId: String, val returnTo: KalamoraRoute) : KalamoraRoute
}

@Composable
fun KalamoraApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { PreferencesRepository(context) }

    val themeMode by preferences.themeModeFlow.collectAsStateWithLifecycle(initialValue = "system")
    val interfaceLanguage by preferences.interfaceLanguageFlow.collectAsStateWithLifecycle(initialValue = "en")
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val strings = remember(interfaceLanguage) { stringsForLanguage(interfaceLanguage) }

    CompositionLocalProvider(LocalStrings provides strings) {
        KalamoraTheme(themeMode = themeMode, interfaceLanguage = interfaceLanguage) {
            val user = currentUser
            if (user == null) {
                WelcomeAuthScreen(
                    onAuthSuccess = { /* Reactive authStateFlow transitions automatically */ },
                    interfaceLanguage = interfaceLanguage,
                    onToggleLanguage = { lang ->
                        scope.launch { preferences.setInterfaceLanguage(lang) }
                    }
                )
            } else {
                val uid = user.uid
                key(uid) {
                    AuthenticatedKalamoraShell(
                        currentUserId = uid,
                        themeMode = themeMode,
                        interfaceLanguage = interfaceLanguage
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedKalamoraShell(
    currentUserId: String,
    themeMode: String,
    interfaceLanguage: String
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: KalamoraViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                KalamoraViewModel(
                    repository = KalamoraRepository(db),
                    preferences = PreferencesRepository(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val myBooksState by viewModel.myBooks.collectAsStateWithLifecycle()
    val myShayariState by viewModel.myShayari.collectAsStateWithLifecycle()
    val publicBooksState by viewModel.publicBooks.collectAsStateWithLifecycle()
    val publicShayariState by viewModel.publicShayari.collectAsStateWithLifecycle()
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val recentWorks by viewModel.recentWorks.collectAsStateWithLifecycle()
    val activeOwnerChapters by viewModel.activeOwnerChapters.collectAsStateWithLifecycle()
    val activeReaderBook by viewModel.activeReaderBook.collectAsStateWithLifecycle()
    val activeReaderChapters by viewModel.activeReaderChapters.collectAsStateWithLifecycle()
    val activeReaderShayari by viewModel.activeReaderShayari.collectAsStateWithLifecycle()
    val chapterSaveStatus by viewModel.chapterSaveStatus.collectAsStateWithLifecycle()
    val shayariSaveStatus by viewModel.shayariSaveStatus.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var activeRoute by remember { mutableStateOf<KalamoraRoute>(KalamoraRoute.MainTabs) }
    var showCreateChooserSheet by remember { mutableStateOf(false) }
    var showCreateBookDialog by remember { mutableStateOf(false) }
    var editingBookMetadata by remember { mutableStateOf<Book?>(null) }

    val profile = (profileState as? UiState.Success)?.data
    val defaultAuthorName = profile?.displayName?.ifBlank {
        Firebase.auth.currentUser?.displayName ?: "Writer"
    } ?: Firebase.auth.currentUser?.displayName ?: "Writer"

    val myBooksList = (myBooksState as? UiState.Success)?.data.orEmpty()
    val myShayariList = (myShayariState as? UiState.Success)?.data.orEmpty()
    val publicBooksList = (publicBooksState as? UiState.Success)?.data.orEmpty()
    val publicShayariList = (publicShayariState as? UiState.Success)?.data.orEmpty()

    fun openBookStudio(bookId: String) {
        viewModel.selectBookForStudio(bookId)
        activeRoute = KalamoraRoute.BookStudio(bookId)
    }

    fun openBookReader(bookId: String, returnTo: KalamoraRoute) {
        viewModel.selectBookForReader(bookId)
        activeRoute = KalamoraRoute.BookReader(bookId, returnTo)
    }

    fun openShayariReader(shayariId: String, returnTo: KalamoraRoute) {
        viewModel.selectShayariForReader(shayariId)
        activeRoute = KalamoraRoute.ShayariReader(shayariId, returnTo)
    }

    // Secondary full-screen studio & reader routes
    when (val route = activeRoute) {
        is KalamoraRoute.BookStudio -> {
            val book = myBooksList.firstOrNull { it.id == route.bookId }
            if (book != null) {
                BookWritingStudioScreen(
                    book = book,
                    chaptersState = activeOwnerChapters,
                    saveStatus = chapterSaveStatus,
                    onMarkUnsaved = { viewModel.markChapterUnsaved() },
                    onResetSaveStatus = { viewModel.resetChapterSaveStatus() },
                    onSaveChapter = { chId, title, content ->
                        viewModel.saveChapterNow(book.id, chId, title, content)
                    },
                    onAddChapter = { title, onAdded ->
                        viewModel.addChapter(book.id, book.visibility, title, onAdded)
                    },
                    onReorderChapter = { chId, moveUp ->
                        viewModel.reorderChapter(book.id, chId, moveUp)
                    },
                    onDeleteChapter = { chId, onDeleted ->
                        viewModel.deleteChapter(book.id, chId, onDeleted)
                    },
                    onEditBookDetails = { editingBookMetadata = book },
                    onToggleVisibility = { makePublic ->
                        viewModel.setBookVisibility(book.id, makePublic)
                    },
                    onOpenFullBookPreview = {
                        openBookReader(book.id, KalamoraRoute.BookStudio(book.id))
                    },
                    onDeleteBook = {
                        viewModel.deleteBook(book.id) {
                            viewModel.selectBookForStudio(null)
                            activeRoute = KalamoraRoute.MainTabs
                        }
                    },
                    onNavigateBack = {
                        viewModel.selectBookForStudio(null)
                        activeRoute = KalamoraRoute.MainTabs
                    }
                )
            } else {
                LaunchedEffect(Unit) { activeRoute = KalamoraRoute.MainTabs }
            }
        }

        is KalamoraRoute.ShayariStudio -> {
            val existing = route.shayariId?.let { id -> myShayariList.firstOrNull { it.id == id } }
            ShayariStudioScreen(
                existingShayari = existing,
                defaultAuthorName = defaultAuthorName,
                saveStatus = shayariSaveStatus,
                onMarkUnsaved = { viewModel.markShayariUnsaved() },
                onResetSaveStatus = { viewModel.resetShayariSaveStatus() },
                onSaveShayari = { existingId, author, title, content, cat, lang, makePublic ->
                    viewModel.saveShayariNow(existingId, author, title, content, cat, lang, makePublic)
                },
                onDeleteShayari = { id ->
                    viewModel.deleteShayari(id) {
                        activeRoute = KalamoraRoute.MainTabs
                    }
                },
                onNavigateBack = { activeRoute = KalamoraRoute.MainTabs }
            )
        }

        KalamoraRoute.PublicLibrary -> {
            PublicLibraryScreen(
                publicBooksState = publicBooksState,
                publicShayariState = publicShayariState,
                onOpenBookReader = { book ->
                    openBookReader(book.id, KalamoraRoute.PublicLibrary)
                },
                onOpenShayariReader = { shayari ->
                    openShayariReader(shayari.id, KalamoraRoute.PublicLibrary)
                },
                onNavigateBack = { activeRoute = KalamoraRoute.MainTabs }
            )
        }

        is KalamoraRoute.BookReader -> {
            BookReaderScreen(
                bookState = activeReaderBook,
                chaptersState = activeReaderChapters,
                onNavigateBack = {
                    viewModel.selectBookForReader(null)
                    activeRoute = route.returnTo
                }
            )
        }

        is KalamoraRoute.ShayariReader -> {
            ShayariReaderScreen(
                shayariState = activeReaderShayari,
                onNavigateBack = {
                    viewModel.selectShayariForReader(null)
                    activeRoute = route.returnTo
                }
            )
        }

        KalamoraRoute.MainTabs -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 720.dp

                val tabItems = listOf(
                    NavTabSpec(MainTab.HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
                    NavTabSpec(MainTab.MY_BOOKS, strings.navMyBooks, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_books"),
                    NavTabSpec(MainTab.MY_SHAYARI, strings.navMyShayari, Icons.Filled.AutoStories, Icons.Outlined.AutoStories, "nav_shayari"),
                    NavTabSpec(MainTab.PROFILE, strings.navProfile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
                )

                val topTitle = when (currentTab) {
                    MainTab.HOME -> strings.appName
                    MainTab.MY_BOOKS -> strings.navMyBooks
                    MainTab.MY_SHAYARI -> strings.navMyShayari
                    MainTab.PROFILE -> strings.profileTitle
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = topTitle,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { activeRoute = KalamoraRoute.PublicLibrary },
                                    modifier = Modifier.testTag("topbar_public_library_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = strings.publicLibraryTitle
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        if (!useNavRail && currentTab != MainTab.PROFILE) {
                            ExtendedFloatingActionButton(
                                onClick = { showCreateChooserSheet = true },
                                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                text = { Text(strings.createAction) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("create_fab_button")
                            )
                        }
                    },
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar {
                                tabItems.forEach { tab ->
                                    val selected = currentTab == tab.tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab.tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavRail) {
                            NavigationRail(
                                header = {
                                    FloatingActionButton(
                                        onClick = { showCreateChooserSheet = true },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .padding(vertical = 12.dp)
                                            .testTag("rail_create_fab")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = strings.createAction)
                                    }
                                },
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                tabItems.forEach { tab ->
                                    val selected = currentTab == tab.tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { currentTab = tab.tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (currentTab) {
                                MainTab.HOME -> HomeDashboardScreen(
                                    authorDisplayName = defaultAuthorName,
                                    stats = dashboardStats,
                                    recentWorks = recentWorks,
                                    publicBooksCount = publicBooksList.size,
                                    publicShayariCount = publicShayariList.size,
                                    onCreateNewBook = { showCreateBookDialog = true },
                                    onCreateNewShayari = { activeRoute = KalamoraRoute.ShayariStudio(null) },
                                    onOpenBookStudio = { book -> openBookStudio(book.id) },
                                    onOpenShayariStudio = { shayari ->
                                        activeRoute = KalamoraRoute.ShayariStudio(shayari.id)
                                    },
                                    onOpenPublicLibrary = { activeRoute = KalamoraRoute.PublicLibrary }
                                )

                                MainTab.MY_BOOKS -> MyBooksScreen(
                                    booksState = myBooksState,
                                    onCreateNewBook = { showCreateBookDialog = true },
                                    onOpenBookStudio = { book -> openBookStudio(book.id) },
                                    onPreviewBook = { book -> openBookReader(book.id, KalamoraRoute.MainTabs) },
                                    onEditBookDetails = { book -> editingBookMetadata = book },
                                    onToggleBookVisibility = { book, makePublic ->
                                        viewModel.setBookVisibility(book.id, makePublic)
                                    },
                                    onDeleteBook = { book -> viewModel.deleteBook(book.id) }
                                )

                                MainTab.MY_SHAYARI -> MyShayariScreen(
                                    shayariState = myShayariState,
                                    onCreateNewShayari = { activeRoute = KalamoraRoute.ShayariStudio(null) },
                                    onOpenShayariStudio = { shayari ->
                                        activeRoute = KalamoraRoute.ShayariStudio(shayari.id)
                                    },
                                    onPreviewShayari = { shayari ->
                                        openShayariReader(shayari.id, KalamoraRoute.MainTabs)
                                    },
                                    onToggleShayariVisibility = { shayari, makePublic ->
                                        viewModel.setShayariVisibility(shayari.id, makePublic)
                                    },
                                    onDeleteShayari = { shayari -> viewModel.deleteShayari(shayari.id) }
                                )

                                MainTab.PROFILE -> ProfileSettingsScreen(
                                    profileState = profileState,
                                    stats = dashboardStats,
                                    interfaceLanguage = interfaceLanguage,
                                    themeMode = themeMode,
                                    isDeletingAccount = isDeletingAccount,
                                    onSaveProfile = { name, username, bio, photo, lang, dark ->
                                        viewModel.updateProfile(name, username, bio, photo, lang, dark)
                                    },
                                    onChangeInterfaceLanguage = { lang ->
                                        viewModel.setInterfaceLanguage(lang)
                                    },
                                    onChangeThemeMode = { mode ->
                                        viewModel.setThemeMode(mode)
                                    },
                                    onSignOut = {
                                        viewModel.selectBookForStudio(null)
                                        viewModel.selectBookForReader(null)
                                        viewModel.selectShayariForReader(null)
                                        signOutUser(credentialManager, scope)
                                    },
                                    onDeleteAccount = {
                                        viewModel.deleteAccount {
                                            signOutUser(credentialManager, scope)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Chooser Sheet (Book vs Shayari)
    if (showCreateChooserSheet) {
        ModalBottomSheet(onDismissRequest = { showCreateChooserSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.createAction,
                    style = MaterialTheme.typography.headlineSmall
                )
                CreateOptionCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = strings.writeABook,
                    subtitle = strings.writeABookSubtitle,
                    tag = "sheet_create_book",
                    onClick = {
                        showCreateChooserSheet = false
                        showCreateBookDialog = true
                    }
                )
                CreateOptionCard(
                    icon = Icons.Filled.EditNote,
                    title = strings.writeShayari,
                    subtitle = strings.writeShayariSubtitle,
                    tag = "sheet_create_shayari",
                    onClick = {
                        showCreateChooserSheet = false
                        activeRoute = KalamoraRoute.ShayariStudio(null)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Create New Book Modal
    if (showCreateBookDialog) {
        CreateEditBookDialog(
            existingBook = null,
            defaultAuthorName = defaultAuthorName,
            onDismiss = { showCreateBookDialog = false },
            onSubmit = { title, subtitle, author, desc, cat, lang, theme, coverURL, makePublic ->
                viewModel.createBook(
                    title = title,
                    subtitle = subtitle,
                    authorName = author,
                    description = desc,
                    category = cat,
                    language = lang,
                    coverTheme = theme,
                    coverURL = coverURL,
                    makePublic = makePublic
                ) { createdBookId ->
                    showCreateBookDialog = false
                    openBookStudio(createdBookId)
                }
            }
        )
    }

    // Edit Existing Book Metadata Modal
    editingBookMetadata?.let { targetBook ->
        CreateEditBookDialog(
            existingBook = targetBook,
            defaultAuthorName = defaultAuthorName,
            onDismiss = { editingBookMetadata = null },
            onSubmit = { title, subtitle, author, desc, cat, lang, theme, coverURL, _ ->
                viewModel.updateBookMetadata(
                    bookId = targetBook.id,
                    title = title,
                    subtitle = subtitle,
                    authorName = author,
                    description = desc,
                    category = cat,
                    language = lang,
                    coverTheme = theme,
                    coverURL = coverURL
                ) {
                    editingBookMetadata = null
                }
            }
        )
    }
}

private data class NavTabSpec(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
private fun CreateOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
