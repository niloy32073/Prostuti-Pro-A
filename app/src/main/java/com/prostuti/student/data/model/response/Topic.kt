package com.prostuti.student.data.model.response

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "topic",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["id"]),
        Index(value = ["chapter_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class Topic(
    @ColumnInfo(name = "id")
    @SerialName("id")
    val id: String,

    @ColumnInfo(name = "chapter_id")
    @SerialName("chapter_id")
    val chapterId: String,

    @ColumnInfo(name = "name_bn")
    @SerialName("name_bn")
    val nameBn: String,

    @ColumnInfo(name = "name_en")
    @SerialName("name_en")
    val nameEn: String,

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
