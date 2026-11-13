package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prostuti.student.data.model.enums.ReportResourceType
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportedResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportedResourceEntity)

    @Query("SELECT resource_id FROM reported_resource WHERE resource_type = :type")
    fun observeIdsByType(type: ReportResourceType): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM reported_resource WHERE resource_type = :type AND resource_id = :resourceId)")
    suspend fun exists(type: ReportResourceType, resourceId: String): Boolean

    @Query("DELETE FROM reported_resource")
    suspend fun deleteAll()
}
