package com.example.std

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StudentItem(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val group_id: Int? = null,
    val birth_date: String? = null,
    val avatar_url: String? = null
)

// curator_id - если в таблице groups есть такая колонка
@Serializable
data class GroupItem(
    val id: Int,
    val name: String,
    val curator_id: Int? = null
)

// Все поля кроме id необязательные: подойдет любая схема таблицы
@Serializable
data class CuratorItem(
    val id: Int,
    val first_name: String? = null,
    val last_name: String? = null,
    val middle_name: String? = null,
    val patronymic: String? = null,
    val name: String? = null,
    val full_name: String? = null,
    val fio: String? = null,
    val group_id: Int? = null
) {
    val displayName: String
        get() {
            val parts = listOfNotNull(
                last_name,
                first_name,
                middle_name ?: patronymic
            ).filter { it.isNotBlank() }
            if (parts.isNotEmpty()) return parts.joinToString(" ")
            val single = listOfNotNull(full_name, fio, name)
                .firstOrNull { it.isNotBlank() }
            return single ?: "Куратор №$id"
        }
}

@Serializable
data class StudentInsert(
    val first_name: String,
    val last_name: String,
    val group_id: Int? = null,
    val birth_date: String? = null,
    val avatar_url: String? = null
)

@Serializable
data class GroupInsert(val name: String)

@Serializable
data class GroupResponse(val id: Int, val name: String)

object StudentRepository {

    private const val AVATAR_BUCKET = "avatars"

    suspend fun getAllStudents(): List<StudentItem> =
        withContext(Dispatchers.IO) {
            SupabaseClient.instance.from("students").select()
                .decodeList()
        }

    suspend fun getAllGroups(): List<GroupItem> =
        withContext(Dispatchers.IO) {
            SupabaseClient.instance.from("groups").select()
                .decodeList()
        }

    suspend fun getAllCurators(): List<CuratorItem> =
        withContext(Dispatchers.IO) {
            SupabaseClient.instance.from("curators").select()
                .decodeList()
        }

    // select() - чтобы сервер вернул строку и не было ошибки EOF
    suspend fun insertStudent(student: StudentInsert) =
        withContext(Dispatchers.IO) {
            SupabaseClient.instance.from("students")
                .insert(student) {
                    select()
                }
        }

    suspend fun insertGroup(group: GroupInsert): GroupResponse =
        withContext(Dispatchers.IO) {
            SupabaseClient.instance.from("groups")
                .insert(group) {
                    select()
                }.decodeSingle()
        }

    suspend fun deleteStudent(studentId: Int) =
        withContext(Dispatchers.IO) {
            SupabaseClient.instance.from("students").delete {
                filter {
                    eq("id", studentId)
                }
            }
        }

    suspend fun updateStudent(studentId: Int, student: StudentInsert) =
        withContext(Dispatchers.IO) {
            SupabaseClient.instance.from("students")
                .update(student) {
                    filter {
                        eq("id", studentId)
                    }
                    select()
                }
        }

    /**
     * Грузит картинку в Storage (бакет "avatars")
     * и возвращает публичную ссылку для students.avatar_url.
     */
    suspend fun uploadAvatar(bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val path = "${UUID.randomUUID()}.jpg"
            val bucket = SupabaseClient.instance.storage
                .from(AVATAR_BUCKET)
            bucket.upload(path, bytes) {
                upsert = false
                contentType = ContentType.Image.JPEG
            }
            bucket.publicUrl(path)
        }
}