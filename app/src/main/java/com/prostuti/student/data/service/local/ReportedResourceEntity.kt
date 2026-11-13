package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.prostuti.student.data.model.enums.ReportResourceType

/**
 * Things the signed-in user has reported. A report can't be taken back (moderators handle it),
 * so a row is only written after the server accepts the report and is never removed except on
 * logout. The server has no "my reports" list, so this table is the only record on the device.
 */
@Entity(tableName = "reported_resource", primaryKeys = ["resource_type", "resource_id"])
data class ReportedResourceEntity(
    @ColumnInfo(name = "resource_type") val resourceType: ReportResourceType,
    @ColumnInfo(name = "resource_id") val resourceId: String,
    @ColumnInfo(name = "report_id") val reportId: String,
    @ColumnInfo(name = "reason") val reason: String,
    @ColumnInfo(name = "reported_at") val reportedAt: Long
)
