package com.prostuti.student.data.service.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: CurrentUserEntity)

    @Query("SELECT * FROM current_user LIMIT 1")
    suspend fun get(): CurrentUserEntity?

    @Query("SELECT * FROM current_user LIMIT 1")
    fun observe(): Flow<CurrentUserEntity?>

    @Query("DELETE FROM current_user")
    suspend fun deleteAll()

    /** Keeps the table at one row, even when a different user signs in. */
    @Transaction
    suspend fun replace(user: CurrentUserEntity) {
        deleteAll()
        insert(user)
    }
}
