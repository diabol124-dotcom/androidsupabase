package com.example.std

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screenState by remember { mutableStateOf("login") }
    var students by remember { mutableStateOf<List<StudentItem>>(emptyList()) }
    var groupMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editingStudent by remember { mutableStateOf<StudentItem?>(null) }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val groups = StudentRepository.getAllGroups()
                groupMap = groups.associate { it.id to it.name }
                students = StudentRepository.getAllStudents()
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
                students = StudentRepository.getAllStudents()
                Toast.makeText(context, "Студент удален", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка удаления: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun handleSaveStudent(studentInsert: StudentInsert, typedGroupName: String) {
        scope.launch {
            try {
                var finalGroupId: Int? = null

                if (typedGroupName.isNotBlank()) {
                    val existingId = groupMap.entries.firstOrNull { it.value.equals(typedGroupName, ignoreCase = true) }?.key
                    if (existingId != null) {
                        finalGroupId = existingId
                    } else {
                        val newGroup = StudentRepository.insertGroup(GroupInsert(name = typedGroupName))
                        finalGroupId = newGroup.id
                        val groups = StudentRepository.getAllGroups()
                        groupMap = groups.associate { it.id to it.name }
                    }
                }

                // ИСПРАВЛЕНО: Жестко проверяем поля. Если они пусты — шлем в базу строгий null, а не пустую строку ""
                val finalStudentData = studentInsert.copy(
                    group_id = finalGroupId,
                    birth_date = studentInsert.birth_date?.trim()?.ifBlank { null },
                    avatar_url = studentInsert.avatar_url?.trim()?.ifBlank { null } // Теперь аватарка 100% не обязательна
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
            }
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
        "list", "form" -> {
            Scaffold(
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
                            }
                        )
                        "form" -> AddStudentForm(
                            editingStudent = editingStudent,
                            groupMap = groupMap,
                            onCancel = {
                                editingStudent = null
                                screenState = "list"
                            },
                            onAdd = { insertData, groupStr ->
                                handleSaveStudent(insertData, groupStr)
                            }
                        )
                    }
                }
            }
        }
    }
}
