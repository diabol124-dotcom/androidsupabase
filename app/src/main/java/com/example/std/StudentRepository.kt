package com.example.std

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class StudentItem(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val group_id: Int? = null,
    val birth_date: String? = null,
    val avatar_url: String? = null
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
    val birth_date: String? = null,
    val avatar_url: String? = null
)

@Serializable
data class GroupInsert(val name: String)

@Serializable
data class GroupResponse(val id: Int, val name: String)

object StudentRepository {

    suspend fun getAllStudents(): List<StudentItem> = withContext(Dispatchers.IO) {
        SupabaseClient.instance.from("students").select().decodeList()
    }

    suspend fun getAllGroups(): List<GroupItem> = withContext(Dispatchers.IO) {
        SupabaseClient.instance.from("groups").select().decodeList()
    }

    // ИСПРАВЛЕНО: Запрашиваем возврат созданной строки (select), чтобы не было ошибки EOF
    suspend fun insertStudent(student: StudentInsert) = withContext(Dispatchers.IO) {
        SupabaseClient.instance.from("students").insert(student) {
            select()
        }
    }

    suspend fun insertGroup(group: GroupInsert): GroupResponse = withContext(Dispatchers.IO) {
        SupabaseClient.instance.from("groups").insert(group) {
            select()
        }.decodeSingle()
    }

    suspend fun deleteStudent(studentId: Int) = withContext(Dispatchers.IO) {
        SupabaseClient.instance.from("students").delete {
            filter {
                eq("id", studentId)
            }
        }
    }

    // ИСПРАВЛЕНО: Также добавили select(), чтобы метод обновления не падал из-за EOF
    suspend fun updateStudent(studentId: Int, student: StudentInsert) = withContext(Dispatchers.IO) {
        SupabaseClient.instance.from("students").update(student) {
            filter {
                eq("id", studentId)
            }
            select()
        }
    }
}
