package com.example.std

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Свои иконки приложения (рисуются из векторного пути).
 * Если путь вдруг не разберется - подставится обычная иконка.
 */
object AppIcons {

    private fun build(
        name: String,
        path: String,
        evenOdd: Boolean,
        fallback: ImageVector
    ): ImageVector {
        return runCatching {
            ImageVector.Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).addPath(
                pathData = addPathNodes(path),
                pathFillType = if (evenOdd) {
                    PathFillType.EvenOdd
                } else {
                    PathFillType.NonZero
                },
                fill = SolidColor(Color.Black)
            ).build()
        }.getOrDefault(fallback)
    }

    // Академическая шапочка (студенты)
    val School: ImageVector by lazy {
        build(
            name = "School",
            path = "M5,13.18v4L12,21l7,-3.82v-4L12,17l-7,-3.82z" +
                    "M12,3L1,9l11,6 9,-4.91V17h2V9L12,3z",
            evenOdd = false,
            fallback = Icons.Default.Home
        )
    }

    // Несколько людей (группы)
    val People: ImageVector by lazy {
        build(
            name = "People",
            path = "M16,11c1.66,0 2.99,-1.34 2.99,-3S17.66,5 16,5" +
                    "c-1.66,0 -3,1.34 -3,3s1.34,3 3,3z" +
                    "M8,11c1.66,0 2.99,-1.34 2.99,-3S9.66,5 8,5" +
                    "C6.34,5 5,6.34 5,8s1.34,3 3,3z" +
                    "M8,13c-2.33,0 -7,1.17 -7,3.5V19h14v-2.5" +
                    "c0,-2.33 -4.67,-3.5 -7,-3.5z" +
                    "M16,13c-0.29,0 -0.62,0.02 -0.97,0.05" +
                    " 1.16,0.84 1.97,1.97 1.97,3.45V19h6v-2.5" +
                    "c0,-2.33 -4.67,-3.5 -7,-3.5z",
            evenOdd = false,
            fallback = Icons.Default.Face
        )
    }

    // Бейдж (кураторы)
    val Badge: ImageVector by lazy {
        build(
            name = "Badge",
            path = "M20,7h-5V4c0,-1.1 -0.9,-2 -2,-2h-2" +
                    "c-1.1,0 -2,0.9 -2,2v3H4c-1.1,0 -2,0.9 -2,2v11" +
                    "c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V9" +
                    "c0,-1.1 -0.9,-2 -2,-2z" +
                    "M9,12c0.83,0 1.5,0.67 1.5,1.5S9.83,15 9,15" +
                    "s-1.5,-0.67 -1.5,-1.5S8.17,12 9,12z" +
                    "M12,18H6v-0.75c0,-1 2,-1.5 3,-1.5" +
                    "s3,0.5 3,1.5V18z" +
                    "M13,9h-2V4h2V9z" +
                    "M18,16.5h-4V15h4V16.5z" +
                    "M18,13.5h-4V12h4V13.5z",
            evenOdd = true,
            fallback = Icons.Default.Person
        )
    }
}