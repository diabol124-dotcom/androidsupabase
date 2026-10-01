package com.example.std

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var themeSettings by remember {
                mutableStateOf(ThemeStore.load(context))
            }

            AppTheme(themeSettings) {
                MainScreen(
                    themeSettings = themeSettings,
                    onThemeSettingsChange = { themeSettings = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeSettings: ThemeSettings,
    onThemeSettingsChange: (ThemeSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Экраны: login, register, form, settings, groupDetail
    // Вкладки: list, groups, curators, profile
    var screenState by remember { mutableStateOf("login") }
    var settingsBackTo by remember { mutableStateOf("list") }
    var selectedGroupId by remember { mutableStateOf(-1) }
    var currentUser by remember { mutableStateOf("") }

    var students by remember {
        mutableStateOf<List<StudentItem>>(emptyList())
    }
    var groups by remember {
        mutableStateOf<List<GroupItem>>(emptyList())
    }
    var curators by remember {
        mutableStateOf<List<CuratorItem>>(emptyList())
    }
    var curatorsFailed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isThemeBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editingStudent by remember {
        mutableStateOf<StudentItem?>(null)
    }

    val groupMap = remember(groups) {
        groups.associate { it.id to it.name }
    }
    val groupInfos = remember(groups, curators, students) {
        buildGroupInfos(groups, curators, students)
    }

    // Широкий экран (планшет): вместо нижней панели - боковая
    val isWide = LocalConfiguration.current.screenWidthDp >= 720

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                // Все таблицы грузятся параллельно
                coroutineScope {
                    val groupsJob = async {
                        StudentRepository.getAllGroups()
                    }
                    val studentsJob = async {
                        StudentRepository.getAllStudents()
                    }
                    // Кураторы не должны ломать основной экран,
                    // поэтому их ошибка перехватывается отдельно
                    val curatorsJob = async {
                        runCatching { StudentRepository.getAllCurators() }
                    }
                    groups = groupsJob.await()
                    students = studentsJob.await()
                    val result = curatorsJob.await()
                    curators = result.getOrDefault(emptyList())
                    curatorsFailed = result.isFailure
                    result.exceptionOrNull()?.let {
                        Log.e("AppError", "Кураторы: ${it.message}", it)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppError", "Ошибка загрузки: ${e.message}", e)
                errorMessage = "Нет сети или ошибка базы данных. " +
                        "Проверьте RLS в Supabase."
            } finally {
                isLoading = false
            }
        }
    }

    fun handleDeleteStudent(id: Int) {
        scope.launch {
            try {
                StudentRepository.deleteStudent(id)
                // Убираем из списка локально, без лишнего запроса
                students = students.filterNot { it.id == id }
                Toast.makeText(
                    context, "Студент удален", Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Ошибка удаления: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun handleSaveStudent(studentInsert: StudentInsert, newImageUri: Uri?) {
        if (isSaving) return
        isSaving = true
        scope.launch {
            try {
                // Новая картинка: сжимаем, грузим в Storage, берем URL
                var avatarUrl = studentInsert.avatar_url
                if (newImageUri != null) {
                    val bytes = prepareAvatarBytes(context, newImageUri)
                    avatarUrl = StudentRepository.uploadAvatar(bytes)
                }

                val finalData = studentInsert.copy(
                    birth_date = studentInsert.birth_date
                        ?.trim()?.ifBlank { null },
                    avatar_url = avatarUrl?.trim()?.ifBlank { null }
                )

                if (editingStudent == null) {
                    StudentRepository.insertStudent(finalData)
                    Toast.makeText(
                        context, "Студент добавлен", Toast.LENGTH_SHORT
                    ).show()
                } else {
                    StudentRepository.updateStudent(
                        editingStudent!!.id, finalData
                    )
                    Toast.makeText(
                        context, "Данные изменены", Toast.LENGTH_SHORT
                    ).show()
                }

                students = StudentRepository.getAllStudents()
                editingStudent = null
                screenState = "list"
            } catch (e: Exception) {
                Log.e("AppError", "Ошибка сохранения: ${e.message}", e)
                Toast.makeText(
                    context,
                    "Ошибка сохранения: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isSaving = false
            }
        }
    }

    fun handleThemeMode(mode: ThemeMode) {
        onThemeSettingsChange(
            ThemeStore.saveMode(context, themeSettings, mode)
        )
    }

    fun handlePickBackground(uri: Uri) {
        if (isThemeBusy) return
        isThemeBusy = true
        scope.launch {
            try {
                onThemeSettingsChange(
                    ThemeStore.saveCustomImage(context, themeSettings, uri)
                )
            } catch (e: Exception) {
                Log.e("AppError", "Ошибка темы: ${e.message}", e)
                Toast.makeText(
                    context,
                    "Не удалось установить фон: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isThemeBusy = false
            }
        }
    }

    fun handleLogout() {
        currentUser = ""
        students = emptyList()
        groups = emptyList()
        curators = emptyList()
        curatorsFailed = false
        editingStudent = null
        screenState = "login"
    }

    // Системная кнопка "назад"
    val backEnabled = screenState == "form" ||
            screenState == "settings" ||
            screenState == "groupDetail" ||
            (isMainTab(screenState) && screenState != "list")

    BackHandler(enabled = backEnabled) {
        when (screenState) {
            "form" -> if (!isSaving) {
                editingStudent = null
                screenState = "list"
            }
            "settings" -> screenState = settingsBackTo
            "groupDetail" -> screenState = "curators"
            else -> screenState = "list"
        }
    }

    if (errorMessage != null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { loadData() }) {
                Text("Повторить загрузку")
            }
        }
        return
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    when {
        screenState == "login" -> LoginScreen(
            onNavigateToRegister = { screenState = "register" },
            onLoginSuccess = { username ->
                currentUser = username
                screenState = "list"
                loadData()
            }
        )

        screenState == "register" -> RegisterScreen(
            onNavigateToLogin = { screenState = "login" }
        )

        screenState == "settings" -> SettingsScreen(
            settings = themeSettings,
            isBusy = isThemeBusy,
            onModeChange = { handleThemeMode(it) },
            onPickBackground = { handlePickBackground(it) },
            onBack = { screenState = settingsBackTo }
        )

        screenState == "form" -> AddStudentForm(
            editingStudent = editingStudent,
            groupMap = groupMap,
            isSaving = isSaving,
            onCancel = {
                editingStudent = null
                screenState = "list"
            },
            onAdd = { insertData, imageUri ->
                handleSaveStudent(insertData, imageUri)
            }
        )

        screenState == "groupDetail" -> {
            val info = groupInfos.firstOrNull { it.id == selectedGroupId }
            if (info != null) {
                GroupDetailScreen(
                    info = info,
                    onBack = { screenState = "curators" }
                )
            } else {
                LaunchedEffect(Unit) { screenState = "curators" }
            }
        }

        isMainTab(screenState) -> {
            // Содержимое выбранной вкладки
            val tabContent: @Composable () -> Unit = {
                when (screenState) {
                    "list" -> StudentList(
                        students = students,
                        groupMap = groupMap,
                        onEditStudent = { student ->
                            editingStudent = student
                            screenState = "form"
                        },
                        onDeleteStudent = { id ->
                            handleDeleteStudent(id)
                        },
                        onOpenSettings = {
                            settingsBackTo = "list"
                            screenState = "settings"
                        }
                    )
                    "groups" -> GroupsScreen(
                        infos = groupInfos,
                        students = students
                    )
                    "curators" -> CuratorsScreen(
                        curators = curators,
                        infos = groupInfos,
                        loadFailed = curatorsFailed,
                        onOpenGroup = { id ->
                            selectedGroupId = id
                            screenState = "groupDetail"
                        }
                    )
                    "profile" -> ProfileScreen(
                        username = currentUser,
                        studentsCount = students.size,
                        groupsCount = groups.size,
                        curatorsCount = curators.size,
                        onOpenSettings = {
                            settingsBackTo = "profile"
                            screenState = "settings"
                        },
                        onLogout = { handleLogout() }
                    )
                }
            }

            val fab: @Composable () -> Unit = {
                if (screenState == "list") {
                    FloatingActionButton(onClick = {
                        editingStudent = null
                        screenState = "form"
                    }) {
                        Text(
                            "+",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isWide) {
                // Планшет: боковая панель слева
                Row(modifier = Modifier.fillMaxSize()) {
                    AppNavigationRail(
                        current = screenState,
                        onSelect = { screenState = it }
                    )
                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent,
                        floatingActionButton = fab
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) { tabContent() }
                    }
                }
            } else {
                // Телефон: нижняя панель
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        AppNavigationBar(
                            current = screenState,
                            onSelect = { screenState = it }
                        )
                    },
                    floatingActionButton = fab
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) { tabContent() }
                }
            }
        }
    }
}