package com.prostuti.student.data.model.response

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "chapter",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["id"]),
        Index(value = ["subject_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class Chapter(
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: String,

    @ColumnInfo(name = "subject_id")
    @SerialName("subject_id")
    val subjectId: String,

    @ColumnInfo(name = "chapter_name_bn")
    @SerialName("chapter_name_bn")
    val chapterNameBn: String,

    @ColumnInfo(name = "chapter_name_en")
    @SerialName("chapter_name_en")
    val chapterNameEn: String,

    @ColumnInfo(name = "order")
    @SerialName("order")
    val order: Int,

    @ColumnInfo(name = "created_at")
    @SerialName("created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    @SerialName("updated_at")
    val updatedAt: String? = null,

    @ColumnInfo(name = "created_by")
    @SerialName("created_by")
    val createdBy: String,

    @ColumnInfo(name = "updated_by")
    @SerialName("updated_by")
    val updatedBy: String? = null
)
