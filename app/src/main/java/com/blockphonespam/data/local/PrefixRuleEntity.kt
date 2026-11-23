package com.blockphonespam.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prefix_rules")
data class PrefixRuleEntity(
    @PrimaryKey
    val prefix: String,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
