package com.example.ai

import android.graphics.Bitmap
import com.example.data.local.MemoryEntity
import com.example.model.AiReplyResult
import com.example.model.AppLanguage
import com.example.model.PremiumToolType

interface AiRepository {
    suspend fun generateReply(
        query: String,
        language: AppLanguage,
        memories: List<MemoryEntity>,
        userName: String,
        isOffline: Boolean,
        isPremium: Boolean
    ): AiReplyResult

    suspend fun analyzeImage(
        prompt: String,
        bitmap: Bitmap?,
        language: AppLanguage
    ): AiReplyResult

    suspend fun runPremiumTool(
        tool: PremiumToolType,
        input: String,
        targetLanguage: String,
        language: AppLanguage,
        memories: List<MemoryEntity>,
        userName: String
    ): AiReplyResult
}
