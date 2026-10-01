package com.example.std

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ThemeMode { LIGHT, DARK, CUSTOM }

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.LIGHT,
    val customBgFile: String? = null,   // имя файла в filesDir
    val customDark: Boolean = false,    // темное ли фото (определяется автоматически)
    val customAccent: Int = 0           // средний цвет фото
)

object ThemeStore {
    private const val PREFS = "settings_prefs"
    private const val K_MODE = "theme_mode"
    private const val K_FILE = "custom_bg_file"
    private const val K_DARK = "custom_dark"
    private const val K_ACCENT = "custom_accent"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun bgFile(context: Context, settings: ThemeSettings): File? =
        settings.customBgFile?.let { File(context.filesDir, it) }?.takeIf { it.exists() }

    fun load(context: Context): ThemeSettings {
        val p = prefs(context)
        val mode = runCatching { ThemeMode.valueOf(p.getString(K_MODE, null) ?: "LIGHT") }
            .getOrDefault(ThemeMode.LIGHT)
        val settings = ThemeSettings(
            mode = mode,
            customBgFile = p.getString(K_FILE, null),
            customDark = p.getBoolean(K_DARK, false),
            customAccent = p.getInt(K_ACCENT, 0)
        )
        // Защита: если выбрана "своя" тема, а файла уже нет - откатываемся на светлую
        return if (mode == ThemeMode.CUSTOM && bgFile(context, settings) == null) {
            settings.copy(mode = ThemeMode.LIGHT)
        } else settings
    }

    fun saveMode(context: Context, current: ThemeSettings, mode: ThemeMode): ThemeSettings {
        prefs(context).edit().putString(K_MODE, mode.name).apply()
        return current.copy(mode = mode)
    }

    /** Сжимает выбранное фото, сохраняет в память приложения, определяет цвета и включает тему CUSTOM. */
    suspend fun saveCustomImage(context: Context, current: ThemeSettings, uri: android.net.Uri): ThemeSettings =
        withContext(Dispatchers.IO) {
            val bytes = prepareAvatarBytes(context, uri, 1600)
            val newName = "custom_bg_${System.currentTimeMillis()}.jpg"
            File(context.filesDir, newName).writeBytes(bytes)
            current.customBgFile?.let { File(context.filesDir, it).delete() }

            // Средний цвет фото
            val small = BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = 8 }
            )
            val avg = if (small != null) {
                Bitmap.createScaledBitmap(small, 1, 1, true).getPixel(0, 0)
            } else android.graphics.Color.GRAY
            val dark = ColorUtils.calculateLuminance(avg) < 0.5

            prefs(context).edit()
                .putString(K_MODE, ThemeMode.CUSTOM.name)
                .putString(K_FILE, newName)
                .putBoolean(K_DARK, dark)
                .putInt(K_ACCENT, avg)
                .apply()

            ThemeSettings(ThemeMode.CUSTOM, newName, dark, avg)
        }

    fun colorScheme(settings: ThemeSettings): ColorScheme = when (settings.mode) {
        ThemeMode.LIGHT -> lightColorScheme()
        ThemeMode.DARK -> darkColorScheme()
        ThemeMode.CUSTOM -> {
            val avg = settings.customAccent
            val dark = settings.customDark
            val white = android.graphics.Color.WHITE
            val black = android.graphics.Color.BLACK
            val primaryInt = if (dark) ColorUtils.blendARGB(avg, white, 0.45f) else ColorUtils.blendARGB(avg, black, 0.35f)
            val primary = Color(primaryInt)
            val onPrimary = if (ColorUtils.calculateLuminance(primaryInt) > 0.5) Color.Black else Color.White
            val container = Color(
                if (dark) ColorUtils.blendARGB(primaryInt, black, 0.55f) else ColorUtils.blendARGB(primaryInt, white, 0.75f)
            )
            val onContainer = if (dark) Color.White else Color.Black
            if (dark) {
                darkColorScheme(primary = primary, onPrimary = onPrimary, primaryContainer = container, onPrimaryContainer = onContainer)
            } else {
                lightColorScheme(primary = primary, onPrimary = onPrimary, primaryContainer = container, onPrimaryContainer = onContainer)
            }
        }
    }
}

/** Тема приложения + фон (для CUSTOM - фото с затемнением/осветлением для читаемости). */
@Composable
fun AppTheme(settings: ThemeSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = remember(settings) { ThemeStore.colorScheme(settings) }
    val bgFile = remember(settings) {
        if (settings.mode == ThemeMode.CUSTOM) ThemeStore.bgFile(context, settings) else null
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
            if (bgFile != null) {
                AsyncImage(
                    model = bgFile,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(colorScheme.background.copy(alpha = 0.6f)))
            }
            Surface(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                color = Color.Transparent,
                contentColor = colorScheme.onBackground
            ) {
                content()
            }
        }
    }
}