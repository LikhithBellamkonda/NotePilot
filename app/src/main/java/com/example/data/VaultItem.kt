package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contentType: String, // "REEL", "WHATSAPP", "SHORT", "OTHER"
    val title: String,
    val contentOrUrl: String,
    val notes: String = "",
    val tags: String = "", // Comma-separated tags
    val timestamp: Long = System.currentTimeMillis(),
    val isHighlighted: Boolean = false,
    val hasDeadline: Boolean = false,
    val deadlineText: String? = null,
    val deadlineConfirmStatus: String = "PENDING" // "PENDING", "CONFIRMED", "DECLINED"
)
