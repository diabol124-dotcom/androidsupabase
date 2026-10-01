package com.example.std

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Читает картинку из галереи, уменьшает до maxSize по большей
 * стороне, поворачивает по EXIF и сжимает в JPEG.
 * В Supabase летит ~100 КБ вместо 5 МБ.
 */
suspend fun prepareAvatarBytes(
    context: Context,
    uri: Uri,
    maxSize: Int = 512
): ByteArray = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver

    // 1. Размеры, не загружая картинку в память
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }

    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSize &&
        bounds.outHeight / (sample * 2) >= maxSize
    ) {
        sample *= 2
    }

    // 2. Загружаем уже уменьшенную
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
    }
    var bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: throw IllegalStateException("Не удалось прочитать фото")

    // 3. Поворот по EXIF
    val orientation = try {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees != 0f) {
        val matrix = Matrix().apply { postRotate(degrees) }
        bitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    // 4. Доуменьшаем точно до maxSize
    val longest = maxOf(bitmap.width, bitmap.height)
    if (longest > maxSize) {
        val scale = maxSize.toFloat() / longest
        bitmap = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
}