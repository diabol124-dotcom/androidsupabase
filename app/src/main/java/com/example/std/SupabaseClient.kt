package com.example.std

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseClient {
    private const val URL =
        "https://qxjtsvzabzumbbtetsdi.supabase.co"
    private const val KEY =
        "sb_publishable_EMDNixt664-vkbALidgxDg_gdrVmmsd"

    val instance = createSupabaseClient(
        supabaseUrl = URL,
        supabaseKey = KEY
    ) {
        // База данных
        install(Postgrest)

        // Хранилище файлов (аватарки студентов)
        install(Storage)

        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
        })
    }
}