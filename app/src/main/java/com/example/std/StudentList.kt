package com.example.std

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StudentList(
    students: List<StudentItem>,
    groupMap: Map<Int, String>,
    onEditStudent: (StudentItem) -> Unit,
    onDeleteStudent: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    // Телефон - 1 колонка, планшет / ландшафт - 2, 3 и т.д.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Студенты (${students.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Настройки"
                    )
                }
            }
        }

        // key = id: Compose не перерисовывает лишние карточки
        items(students, key = { it.id }) { student ->
            val gName = student.group_id?.let { groupMap[it] }
                ?: "не указана"
            val bDate = student.birth_date ?: "не указан"
            val initials = "${student.first_name.firstOrNull() ?: ""}" +
                    "${student.last_name.firstOrNull() ?: ""}"
            var menuExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!student.avatar_url.isNullOrBlank()) {
                        AsyncImage(
                            model = student.avatar_url,
                            contentDescription = "Аватар",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme
                                        .primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initials.uppercase(),
                                style = MaterialTheme.typography
                                    .titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme
                                    .onPrimaryContainer
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${student.first_name} ${student.last_name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "День рождения: $bDate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme
                                .onSurfaceVariant
                        )
                        Text(
                            "Группа: $gName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme
                                .onSurfaceVariant
                        )
                    }

                    // Троеточие действий
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Меню"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Редактировать") },
                                onClick = {
                                    menuExpanded = false
                                    onEditStudent(student)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Удалить",
                                        color = MaterialTheme
                                            .colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteStudent(student.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}