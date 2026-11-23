package com.blockphonespam.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    @Query("SELECT * FROM blocked_calls ORDER BY blockedAt DESC")
    fun observeAll(): Flow<List<BlockedCallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: BlockedCallEntity)

    @Query("DELETE FROM blocked_calls WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_calls")
    suspend fun clearAll()
}
