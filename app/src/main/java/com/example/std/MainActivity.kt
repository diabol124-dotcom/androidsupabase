package com.example.std

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private const val TAG = "SupabaseDebug"

val supabase = createSupabaseClient(
    supabaseUrl = "https://qxjtsvzabzumbbtetsdi.supabase.co",
    supabaseKey = "sb_publishable_EMDNixt664-vkbALidgxDg_gdrVmmsd"
) {
    install(Postgrest)
}

@Serializable
data class StudentItem(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val group_id: Int? = null,
    val birth_date: String? = null
)

@Serializable
data class GroupItem(
    val id: Int,
    val name: String
)

@Serializable
data class StudentInsert(
    val first_name: String,
    val last_name: String,
    val group_id: Int? = null,
    val birth_date: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudentScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var students by remember { mutableStateOf<List<StudentItem>>(emptyList()) }
    var groupMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Поля формы
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var isInserting by remember { mutableStateOf(false) }

    fun loadStudents() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val result = supabase.from("students").select().decodeList<StudentItem>()
                    students = result
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка загрузки: ${e.message}", e)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val groups = supabase.from("groups").select().decodeList<GroupItem>()
                groupMap = groups.associate { it.id to it.name }

                val studs = supabase.from("students").select().decodeList<StudentItem>()
                students = studs
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый студент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Имя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Фамилия") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text("День рождения (ГГГГ-ММ-ДД)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Группа") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (firstName.isBlank() || lastName.isBlank()) {
                            Toast.makeText(context, "Имя и фамилия обязательны", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val targetGroupId = groupMap.entries
                            .find { it.value.equals(groupName.trim(), ignoreCase = true) }?.key

                        if (groupName.isNotBlank() && targetGroupId == null) {
                            Toast.makeText(
                                context,
                                "Группа не найдена. Доступные: ${groupMap.values.joinToString()}",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        isInserting = true
                        val newStudent = StudentInsert(
                            first_name = firstName.trim(),
                            last_name = lastName.trim(),
                            group_id = targetGroupId,
                            birth_date = birthDate.trim().ifBlank { null }
                        )

                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    supabase.from("students").insert(newStudent)
                                    loadStudents()
                                    firstName = ""
                                    lastName = ""
                                    birthDate = ""
                                    groupName = ""
                                    showAddDialog = false
                                } catch (e: Exception) {
                                    Log.e(TAG, "Ошибка вставки: ${e.message}", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } finally {
                                    isInserting = false
                                }
                            }
                        }
                    },
                    enabled = !isInserting
                ) {
                    if (isInserting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Добавить")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Студенты (${students.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = { showAddDialog = true }) {
                            Text("+ Добавить")
                        }
                    }
                }

                items(students) { student ->
                    val gName = student.group_id?.let { groupMap[it] } ?: "не указана"
                    val bDate = student.birth_date ?: "не указан"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray)
                            .padding(12.dp)
                    ) {
                        Text(
                            "${student.first_name} ${student.last_name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("День рождения: $bDate", style = MaterialTheme.typography.bodyMedium)
                        Text("Группа: $gName", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
