package com.dylandos.iptv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An Xtream IPTV provider account. [isActive] marks the account whose channels
 * are currently presented. Passwords are stored Base64-obfuscated (not hashed) —
 * they are needed for stream URL generation and forwarded through TLS.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val epgUrl: String? = null,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
