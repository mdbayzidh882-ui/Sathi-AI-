package com.example

import com.example.ai.PlaceholderAiRepository
import com.example.model.AppLanguage
import com.example.model.DeviceCommand
import com.example.model.MoodTone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val repo = PlaceholderAiRepository()

    @Test
    fun `creator question returns Bayzid Hasan attribution`() = runTest {
        val result = repo.generateReply(
            query = "তোমাকে কে বানিয়েছে?",
            language = AppLanguage.BENGALI,
            memories = emptyList(),
            userName = "",
            isOffline = false,
            isPremium = false
        )
        assertEquals("আমাকে বানিয়েছেন মোহাম্মদ বাইজিদ হাসান।", result.text)
    }

    @Test
    fun `sad input selects comforting mood`() = runTest {
        val result = repo.generateReply(
            query = "আজ মনটা খুব খারাপ",
            language = AppLanguage.BENGALI,
            memories = emptyList(),
            userName = "বাইজিদ",
            isOffline = false,
            isPremium = false
        )
        assertEquals(MoodTone.COMFORTING, result.mood)
    }

    @Test
    fun `flashlight voice query parses ToggleFlashlight command`() = runTest {
        val result = repo.generateReply(
            query = "ফ্ল্যাশলাইট জ্বালাও",
            language = AppLanguage.BENGALI,
            memories = emptyList(),
            userName = "",
            isOffline = true,
            isPremium = false
        )
        assertTrue(result.deviceCommand is DeviceCommand.ToggleFlashlight)
    }
}
