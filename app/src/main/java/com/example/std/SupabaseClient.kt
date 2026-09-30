package com.example.std

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {
    val instance = createSupabaseClient(
        supabaseUrl = "https://qxjtsvzabzumbbtetsdi.supabase.co",
        supabaseKey = "sb_publishable_EMDNixt664-vkbALidgxDg_gdrVmmsd"
    ) {
        // Установка модуля работы с базой данных Postgrest
        install(Postgrest)

        // ПРАВИЛЬНО: Передаем конфигурацию Json через встроенный KotlinXSerializer библиотеки
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
        })
    }
}
