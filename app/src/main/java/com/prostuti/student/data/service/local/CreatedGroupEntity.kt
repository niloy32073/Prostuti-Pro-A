package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity

/** A group the student created or joined via code at runtime — on top of [ExamDummyData]'s 4
 * fixed demo groups. `myRole` is stored as [com.prostuti.student.data.model.enums.GroupRole]'s
 * `name`. */
@Entity(tableName = "created_group", primaryKeys = ["id"])
data class CreatedGroupEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name_bn") val nameBn: String,
    @ColumnInfo(name = "my_role") val myRole: String,
    @ColumnInfo(name = "join_code") val joinCode: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long
)
