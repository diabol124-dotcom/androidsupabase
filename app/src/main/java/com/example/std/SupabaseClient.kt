package com.example.std

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseClient {
    val instance = createSupabaseClient(
        supabaseUrl = "https://qxjtsvzabzumbbtetsdi.supabase.co",
        supabaseKey = "sb_publishable_EMDNixt664-vkbALidgxDg_gdrVmmsd"
    ) {
        // Установка модуля работы с базой данных Postgrest
        install(Postgrest)

        // Хранилище файлов (для аватарок)
        install(Storage)

        // ПРАВИЛЬНО: Передаем конфигурацию Json через встроенный KotlinXSerializer библиотеки
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
        })
    }
}