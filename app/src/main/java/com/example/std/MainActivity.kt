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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var themeSettings by remember { mutableStateOf(ThemeStore.load(context)) }

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

    var screenState by remember { mutableStateOf("login") }
    var students by remember { mutableStateOf<List<StudentItem>>(emptyList()) }
    var groupMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isThemeBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editingStudent by remember { mutableStateOf<StudentItem?>(null) }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                // Группы и студенты грузятся ПАРАЛЛЕЛЬНО (раньше - по очереди)
                coroutineScope {
                    val groupsDeferred = async { StudentRepository.getAllGroups() }
                    val studentsDeferred = async { StudentRepository.getAllStudents() }
                    groupMap = groupsDeferred.await().associate { it.id to it.name }
                    students = studentsDeferred.await()
                }
            } catch (e: Exception) {
                Log.e("AppError", "Ошибка загрузки: ${e.message}", e)
                errorMessage = "Нет сети или ошибка базы данных. Проверьте RLS в Supabase."
            } finally {
                isLoading = false
            }
        }
    }

    fun handleDeleteStudent(id: Int) {
        scope.launch {
            try {
                StudentRepository.deleteStudent(id)
                // Убираем из списка локально - без лишнего запроса к серверу
                students = students.filterNot { it.id == id }
                Toast.makeText(context, "Студент удален", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка удаления: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun handleSaveStudent(studentInsert: StudentInsert, newImageUri: Uri?) {
        if (isSaving) return
        isSaving = true
        scope.launch {
            try {
                // Если выбрали новую картинку - сжимаем, грузим в Storage и берем URL
                var avatarUrl = studentInsert.avatar_url
                if (newImageUri != null) {
                    val bytes = prepareAvatarBytes(context, newImageUri)
                    avatarUrl = StudentRepository.uploadAvatar(bytes)
                }

                val finalStudentData = studentInsert.copy(
                    birth_date = studentInsert.birth_date?.trim()?.ifBlank { null },
                    avatar_url = avatarUrl?.trim()?.ifBlank { null }
                )

                if (editingStudent == null) {
                    StudentRepository.insertStudent(finalStudentData)
                    Toast.makeText(context, "Студент добавлен", Toast.LENGTH_SHORT).show()
                } else {
                    StudentRepository.updateStudent(editingStudent!!.id, finalStudentData)
                    Toast.makeText(context, "Данные изменены", Toast.LENGTH_SHORT).show()
                }

                students = StudentRepository.getAllStudents()
                editingStudent = null
                screenState = "list"
            } catch (e: Exception) {
                Log.e("AppError", "Ошибка сохранения: ${e.message}", e)
                Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
            }
        }
    }

    fun handleThemeMode(mode: ThemeMode) {
        onThemeSettingsChange(ThemeStore.saveMode(context, themeSettings, mode))
    }

    fun handlePickBackground(uri: Uri) {
        if (isThemeBusy) return
        isThemeBusy = true
        scope.launch {
            try {
                onThemeSettingsChange(ThemeStore.saveCustomImage(context, themeSettings, uri))
            } catch (e: Exception) {
                Log.e("AppError", "Ошибка темы: ${e.message}", e)
                Toast.makeText(context, "Не удалось установить фон: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isThemeBusy = false
            }
        }
    }

    // Системная кнопка "назад" на формах и в настройках
    BackHandler(enabled = screenState == "form" || screenState == "settings") {
        if (screenState == "form") {
            if (!isSaving) {
                editingStudent = null
                screenState = "list"
            }
        } else {
            screenState = "list"
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    when (screenState) {
        "login" -> LoginScreen(
            onNavigateToRegister = { screenState = "register" },
            onLoginSuccess = {
                screenState = "list"
                loadData()
            }
        )
        "register" -> RegisterScreen(
            onNavigateToLogin = { screenState = "login" }
        )
        "settings" -> SettingsScreen(
            settings = themeSettings,
            isBusy = isThemeBusy,
            onModeChange = { handleThemeMode(it) },
            onPickBackground = { handlePickBackground(it) },
            onBack = { screenState = "list" }
        )
        "list", "form" -> {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                floatingActionButton = {
                    if (screenState == "list") {
                        FloatingActionButton(onClick = {
                            editingStudent = null
                            screenState = "form"
                        }) {
                            Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                            onOpenSettings = { screenState = "settings" }
                        )
                        "form" -> AddStudentForm(
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
                    }
                }
            }
        }
    }
}