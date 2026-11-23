package com.blockphonespam.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrefixRuleDao {

    @Query("SELECT * FROM prefix_rules ORDER BY isDefault DESC, createdAt ASC, prefix ASC")
    fun observeAll(): Flow<List<PrefixRuleEntity>>

    @Query("SELECT * FROM prefix_rules")
    suspend fun getAll(): List<PrefixRuleEntity>

    @Query("SELECT prefix FROM prefix_rules WHERE isEnabled = 1")
    suspend fun getEnabledPrefixes(): List<String>

    @Query("SELECT COUNT(*) FROM prefix_rules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rules: List<PrefixRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: PrefixRuleEntity)

    @Query("UPDATE prefix_rules SET isEnabled = :enabled WHERE prefix = :prefix")
    suspend fun setEnabled(prefix: String, enabled: Boolean)

    @Query("DELETE FROM prefix_rules WHERE prefix = :prefix")
    suspend fun deleteByPrefix(prefix: String)
}
