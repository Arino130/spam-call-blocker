package com.blockphonespam.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_calls")
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val matchedPrefix: String,
    val blockedAt: Long = System.currentTimeMillis()
)
