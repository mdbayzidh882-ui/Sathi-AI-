package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversation_messages")
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String, // "user" or "sathi"
    val text: String,
    val mood: String = "WARM",
    val sourceTitle: String? = null,
    val sourceUrl: String? = null,
    val isPlaceholderAi: Boolean = false,
    val actionStatus: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
