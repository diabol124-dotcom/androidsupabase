package com.example.std

/** Группа вместе с ее кураторами и студентами. */
data class GroupInfo(
    val id: Int,
    val name: String,
    val curators: List<CuratorItem>,
    val students: List<StudentItem>
)

/**
 * Связь куратор <-> группа работает в обе стороны:
 * groups.curator_id -> curators.id
 * или curators.group_id -> groups.id
 */
fun buildGroupInfos(
    groups: List<GroupItem>,
    curators: List<CuratorItem>,
    students: List<StudentItem>
): List<GroupInfo> {
    return groups.sortedBy { it.name }.map { group ->
        val linked = curators.filter { c ->
            c.id == group.curator_id || c.group_id == group.id
        }
        GroupInfo(
            id = group.id,
            name = group.name,
            curators = linked,
            students = students.filter { it.group_id == group.id }
        )
    }
}

fun groupsOfCurator(
    curator: CuratorItem,
    infos: List<GroupInfo>
): List<GroupInfo> {
    return infos.filter { info ->
        info.curators.any { it.id == curator.id }
    }
}

fun GroupInfo.curatorText(): String {
    if (curators.isEmpty()) return "не назначен"
    return curators.joinToString(", ") { it.displayName }
}

fun studentsCountText(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    val word = when {
        mod10 == 1 && mod100 != 11 -> "студент"
        mod10 in 2..4 && mod100 !in 12..14 -> "студента"
        else -> "студентов"
    }
    return "$n $word"
}