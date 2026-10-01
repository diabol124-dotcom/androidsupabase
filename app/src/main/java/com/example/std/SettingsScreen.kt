package com.example.std

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun SettingsScreen(
    settings: ThemeSettings,
    isBusy: Boolean,
    onModeChange: (ThemeMode) -> Unit,
    onPickBackground: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickBackground(uri)
    }
    fun launchPicker() {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val bgFile = remember(settings) { ThemeStore.bgFile(context, settings) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, enabled = !isBusy) { Text("← Назад") }
            Text("Настройки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Тема оформления", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            val options = listOf(
                ThemeMode.LIGHT to "Светлая",
                ThemeMode.DARK to "Тёмная",
                ThemeMode.CUSTOM to "Своя (фон из галереи)"
            )
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    options.forEach { (mode, title) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = settings.mode == mode,
                                    enabled = !isBusy,
                                    role = Role.RadioButton,
                                    onClick = {
                                        if (mode == ThemeMode.CUSTOM && bgFile == null) {
                                            // Фото еще не выбирали - сразу открываем галерею
                                            launchPicker()
                                        } else {
                                            onModeChange(mode)
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {RadioButton(selected = settings.mode == mode, onClick = null)
                            Text(title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (bgFile != null) {
                Text("Фон своей темы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AsyncImage(
                    model = bgFile,
                    contentDescription = "Фон",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (bgFile != null || settings.mode == ThemeMode.CUSTOM) {
                OutlinedButton(
                    onClick = { launchPicker() },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Изменить фон")
                }
            }
        }
    }
}