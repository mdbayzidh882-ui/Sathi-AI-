package com.example.ai

import android.graphics.Bitmap
import com.example.data.local.MemoryEntity
import com.example.model.AiReplyResult
import com.example.model.AppLanguage
import com.example.model.DeviceCommand
import com.example.model.MoodTone
import com.example.model.PremiumToolType
import com.example.model.WebSource
import kotlinx.coroutines.delay

/**
 * Placeholder implementation used when Firebase AI Logic is not enabled.
 * Provides real device-command parsing, creator identity rules, mood matching,
 * and clearly labeled sample AI responses without embedding any API keys.
 */
class PlaceholderAiRepository : AiRepository {

    override suspend fun generateReply(
        query: String,
        language: AppLanguage,
        memories: List<MemoryEntity>,
        userName: String,
        isOffline: Boolean,
        isPremium: Boolean
    ): AiReplyResult {
        delay(220L)
        val trimmed = query.trim()
        val lower = trimmed.lowercase()
        val isBn = language == AppLanguage.BENGALI
        val greetingName = userName.ifBlank { if (isBn) "বন্ধু" else "friend" }

        // 1. Creator identity rule (always exact)
        if (containsAny(
                lower,
                "কে বানিয়েছে", "কে তৈরি করেছে", "তোমার স্রষ্টা", "who made you",
                "who created you", "your creator", "developer", "বাইজিদ"
            )
        ) {
            return AiReplyResult(
                text = if (isBn) {
                    "আমাকে বানিয়েছেন মোহাম্মদ বাইজিদ হাসান।"
                } else {
                    "I was created by Mohammad Bayzid Hasan. (আমাকে বানিয়েছেন মোহাম্মদ বাইজিদ হাসান)"
                },
                mood = MoodTone.WARM,
                isPlaceholderResponse = false
            )
        }

        // 2. Device action commands (work both online and offline)
        parseDeviceCommand(trimmed, lower, isBn)?.let { (cmd, replyText) ->
            return AiReplyResult(
                text = replyText,
                mood = MoodTone.WARM,
                deviceCommand = cmd,
                isPlaceholderResponse = false
            )
        }

        // 3. Web search queries
        if (containsAny(
                lower,
                "খবর", "news", "আবহাওয়া", "weather", "স্কোর", "score", "cricket",
                "ক্রিকেট", "দাম", "price", "সার্চ", "search", "খোঁজ"
            )
        ) {
            if (isOffline) {
                return AiReplyResult(
                    text = if (isBn) {
                        "আমি এখন অফলাইন মোডে আছি, $greetingName। ওয়েব সার্চ করার জন্য ইন্টারনেট সংযোগ প্রয়োজন। তবে অ্যালার্ম, টাইমার, ফ্ল্যাশলাইট ও কল অফলাইনেও কাজ করবে।"
                    } else {
                        "I am currently in Offline Mode, $greetingName. Web search needs an internet connection, though alarms, timers, flashlight, and calls still work offline."
                    },
                    mood = MoodTone.SERIOUS,
                    isPlaceholderResponse = false
                )
            }
            return AiReplyResult(
                text = if (isBn) {
                    "[নমুনা ওয়েব সার্চ সারসংক্ষেপ] আপনার প্রশ্ন '$trimmed' অনুযায়ী নির্ভরযোগ্য সংবাদ ও তথ্যসূত্র নিচে যুক্ত করা হয়েছে। লাইভ সার্চ ও Gemini AI উত্তরের জন্য Firebase সংযুক্ত করুন।"
                } else {
                    "[Sample Web Search Summary] Trusted reference links for '$trimmed' are attached below. Enable Firebase AI Logic for live Gemini-grounded search summaries."
                },
                mood = MoodTone.SERIOUS,
                sources = buildSampleSources(lower, isBn),
                isPlaceholderResponse = true
            )
        }

        // 4. Mood-matched emotional companion replies
        if (containsAny(
                lower,
                "মন খারাপ", "কষ্ট", "দুঃখ", "কান্না", "একা", "ভালো লাগছে না",
                "ভালো নেই", "অবসাদ", "মন ভেঙে",
                "sad", "lonely", "upset", "depressed", "tired", "stressed", "hurt"
            ) || (lower.contains("মন") && containsAny(lower, "খারাপ", "ভাঙা", "অস্থির", "উদাস"))
        ) {
            return AiReplyResult(
                text = if (isBn) {
                    "[নমুনা সহানুভূতিশীল উত্তর] মন খারাপ করো না, $greetingName। আমি সাথী সবসময় তোমার পাশে আছি। চাইলে একটু গভীর শ্বাস নাও, আর আমাকে খুলে বলো কী হয়েছে—আমি মন দিয়ে শুনছি।"
                } else {
                    "[Sample Comforting Response] Please don't feel alone, $greetingName. I'm right here listening gently. Take a slow breath and tell me what's weighing on your heart."
                },
                mood = MoodTone.COMFORTING,
                isPlaceholderResponse = true
            )
        }

        if (containsAny(
                lower,
                "খুশি", "দারুণ", "অভিনন্দন", "ভালো আছি", "হয়েছে", "জিতেছি",
                "happy", "excited", "great", "awesome", "good news", "wonderful"
            )
        ) {
            return AiReplyResult(
                text = if (isBn) {
                    "[নমুনা আনন্দময় উত্তর] বাহ, শুনে মনটা ভরে গেল $greetingName! তোমার আনন্দের মুহূর্তে আমিও দারুণ খুশি। চলো দিনটা আরও সুন্দর করে তুলি!"
                } else {
                    "[Sample Joyful Response] That is wonderful news, $greetingName! Hearing you happy brightens my whole day too."
                },
                mood = MoodTone.JOYFUL,
                isPlaceholderResponse = true
            )
        }

        // 5. Memory-aware warm chat response
        val memoryHint = memories.firstOrNull()?.let {
            if (isBn) " (আপনার সংরক্ষিত স্মৃতি: ${it.title})" else " (Saved memory: ${it.title})"
        } ?: ""

        if (isOffline) {
            return AiReplyResult(
                text = if (isBn) {
                    "অফলাইন প্যাক সক্রিয় আছে, $greetingName। আমি আপনার কথা শুনছি: \"$trimmed\"$memoryHint। অফলাইনে সাধারণ আলাপ, অ্যালার্ম, টাইমার ও ফ্ল্যাশলাইট ব্যবহার করতে পারেন।"
                } else {
                    "Offline companion mode is active, $greetingName. I heard: \"$trimmed\"$memoryHint. We can chat casually offline, or run alarms, timers, and flashlight."
                },
                mood = MoodTone.WARM,
                isPlaceholderResponse = true
            )
        }

        return AiReplyResult(
            text = if (isBn) {
                "[নমুনা সাথী উত্তর] হ্যালো $greetingName, আমি আপনার কথা শুনেছি: \"$trimmed\"$memoryHint। লাইভ Gemini AI কথোপকথনের জন্য সেটিংস/প্রজেক্টে Firebase সক্রিয় করুন।"
            } else {
                "[Sample Sathi Response] Hi $greetingName, I received: \"$trimmed\"$memoryHint. Enable Firebase to connect Sathi to live Gemini AI."
            },
            mood = MoodTone.WARM,
            isPlaceholderResponse = true
        )
    }

    override suspend fun analyzeImage(
        prompt: String,
        bitmap: Bitmap?,
        language: AppLanguage
    ): AiReplyResult {
        delay(280L)
        val isBn = language == AppLanguage.BENGALI
        val resolutionInfo = if (bitmap != null) "${bitmap.width}x${bitmap.height}px" else "No bitmap"
        return AiReplyResult(
            text = if (isBn) {
                "[নমুনা ক্যামেরা বিশ্লেষণ ($resolutionInfo)] ছবিটি সফলভাবে গ্রহণ করা হয়েছে। প্রশ্ন: \"${prompt.ifBlank { "এই ছবিতে কী আছে?" }}\"। ছবির গভীর বিশ্লেষণ ও ভিশন উত্তরের জন্য Firebase AI Logic প্রয়োজন।"
            } else {
                "[Sample Camera Analysis ($resolutionInfo)] Photo received safely for prompt: \"${prompt.ifBlank { "What is in this image?" }}\". Enable Firebase AI Logic for live Gemini multimodal image understanding."
            },
            mood = MoodTone.SERIOUS,
            isPlaceholderResponse = true
        )
    }

    override suspend fun runPremiumTool(
        tool: PremiumToolType,
        input: String,
        targetLanguage: String,
        language: AppLanguage,
        memories: List<MemoryEntity>,
        userName: String
    ): AiReplyResult {
        delay(250L)
        val isBn = language == AppLanguage.BENGALI
        val name = userName.ifBlank { if (isBn) "বন্ধু" else "Friend" }
        val text = when (tool) {
            PremiumToolType.SUMMARIZE -> if (isBn) {
                "[নমুনা সারসংক্ষেপ] মূল বিষয়বস্তু: \"${input.take(90)}...\"\n• প্রধান পয়েন্ট সংক্ষেপে সাজানো হয়েছে।\n• সম্পূর্ণ AI সারসংক্ষেপের জন্য Firebase AI Logic প্রয়োজন।"
            } else {
                "[Sample Summary] Key takeaway from \"${input.take(90)}...\":\n• Condensed into clear bullet points.\n• Connect Firebase AI Logic for live Gemini summarization."
            }

            PremiumToolType.TRANSLATE -> if (isBn) {
                "[নমুনা অনুবাদ -> $targetLanguage] উৎস লেখা: \"$input\"\nলাইভ নির্ভুল অনুবাদের জন্য Firebase AI Logic সক্রিয় করুন।"
            } else {
                "[Sample Translation -> $targetLanguage] Source: \"$input\"\nEnable Firebase AI Logic for live Gemini neural translation."
            }

            PremiumToolType.WRITE_MESSAGE -> if (isBn) {
                "[নমুনা বার্তা খসড়া]\nপ্রিয়,\nআশা করি ভালো আছেন। \"$input\" বিষয়ে আপনার সাথে যোগাযোগ করছি। সময় করে জানালে কৃতজ্ঞ থাকব।\nধন্যবাদান্তে,\n$name"
            } else {
                "[Sample Message Draft]\nHello,\nI hope you are doing well. I'm reaching out regarding \"$input\". Please let me know when it's convenient to discuss.\nBest regards,\n$name"
            }

            PremiumToolType.DAY_PLAN -> if (isBn) {
                "[নমুনা দিনের পরিকল্পনা - $name]\n• সকাল ৮:০০ — হালকা ব্যায়াম ও সকালের নাশতা\n• সকাল ১০:০০ — মূল কাজ: ${input.ifBlank { "অগ্রাধিকার তালিকা" }}\n• দুপুর ১:৩০ — বিরতি ও দুপুরের খাবার\n• বিকাল ৫:০০ — হাঁটা ও পরিবারের সাথে সময়"
            } else {
                "[Sample Day Plan for $name]\n• 8:00 AM — Morning refresh & breakfast\n• 10:00 AM — Focus block: ${input.ifBlank { "Top priorities" }}\n• 1:30 PM — Lunch & rest\n• 5:00 PM — Walk & evening unwind"
            }

            PremiumToolType.MORNING_GREETING -> if (isBn) {
                "সুপ্রভাত $name! ☀️ আজকের সকালটা শান্তি ও আনন্দে ভরে উঠুক। আপনার সংরক্ষিত ${memories.size}টি স্মৃতি প্রস্তুত আছে—আজ আমি কীভাবে সাহায্য করতে পারি?"
            } else {
                "Good morning, $name! ☀️ May today bring calm focus and warm moments. You have ${memories.size} saved memories ready—how can Sathi help today?"
            }

            PremiumToolType.CAMERA_VISION -> if (isBn) {
                "[নমুনা ক্যামেরা ভিশন] ছবি নির্বাচন করুন বা তুলুন।"
            } else {
                "[Sample Camera Vision] Pick or capture a photo to analyze."
            }
        }
        return AiReplyResult(
            text = text,
            mood = if (tool == PremiumToolType.MORNING_GREETING) MoodTone.JOYFUL else MoodTone.WARM,
            isPlaceholderResponse = true
        )
    }

    private fun parseDeviceCommand(
        original: String,
        lower: String,
        isBn: Boolean
    ): Pair<DeviceCommand, String>? {
        // Flashlight
        if (containsAny(lower, "ফ্ল্যাশ", "টর্চ", "flashlight", "torch")) {
            val enable = when {
                containsAny(lower, "বন্ধ", "অফ", "off", "stop") -> false
                containsAny(lower, "জ্বালাও", "চালু", "অন", "on", "start") -> true
                else -> null
            }
            val msg = if (isBn) {
                when (enable) {
                    true -> "ফ্ল্যাশলাইট চালু করছি।"
                    false -> "ফ্ল্যাশলাইট বন্ধ করছি।"
                    null -> "ফ্ল্যাশলাইটের অবস্থা পরিবর্তন করছি।"
                }
            } else {
                when (enable) {
                    true -> "Turning on your flashlight."
                    false -> "Turning off your flashlight."
                    null -> "Toggling your flashlight."
                }
            }
            return DeviceCommand.ToggleFlashlight(enable) to msg
        }

        // Volume
        if (containsAny(lower, "ভলিউম", "শব্দ", "volume", "sound")) {
            val delta = if (containsAny(lower, "কমাও", "কম", "down", "lower", "decrease")) -2 else 2
            val msg = if (isBn) {
                if (delta > 0) "মিডিয়া ভলিউম বাড়াচ্ছি।" else "মিডিয়া ভলিউম কমাচ্ছি।"
            } else {
                if (delta > 0) "Increasing media volume." else "Lowering media volume."
            }
            return DeviceCommand.AdjustVolume(delta) to msg
        }

        // YouTube
        if (containsAny(lower, "ইউটিউব", "youtube")) {
            val cleaned = original
                .replace(Regex("(?i)youtube|ইউটিউবে|ইউটিউব|চালাও|খোলো|সার্চ করো|play|search|open|on"), "")
                .trim()
                .ifBlank { if (isBn) "বাংলা গান" else "relaxing music" }
            val msg = if (isBn) {
                "ইউটিউবে \"$cleaned\" খুঁজে দিচ্ছি।"
            } else {
                "Opening YouTube for \"$cleaned\"."
            }
            return DeviceCommand.OpenYouTube(cleaned) to msg
        }

        // Timer
        if (containsAny(lower, "টাইমার", "timer")) {
            val minutes = Regex("(\\d+)").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 5
            val seconds = (minutes.coerceIn(1, 120)) * 60
            val msg = if (isBn) {
                "$minutes মিনিটের টাইমার সেট করছি।"
            } else {
                "Setting a $minutes-minute timer."
            }
            return DeviceCommand.SetTimer(seconds, "Sathi Timer") to msg
        }

        // Alarm
        if (containsAny(lower, "অ্যালার্ম", "alarm")) {
            val hour = Regex("(\\d{1,2})").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
            val msg = if (isBn) {
                "$hour:00-এর অ্যালার্ম সেট করার স্ক্রিন খুলছি।"
            } else {
                "Opening alarm setup for $hour:00."
            }
            return DeviceCommand.SetAlarm(hour, 0, "Sathi Alarm") to msg
        }

        // Calendar / Reminder
        if (containsAny(lower, "রিমাইন্ডার", "ক্যালেন্ডার", "মনে করিয়ে", "reminder", "calendar")) {
            val msg = if (isBn) {
                "ক্যালেন্ডার রিমাইন্ডার যোগ করার জন্য প্রস্তুত।"
            } else {
                "Opening calendar reminder for you."
            }
            return DeviceCommand.CreateReminder(original) to msg
        }

        // Call (Always confirmation required)
        if (containsAny(lower, "কল", "ফোন করো", "call")) {
            val digits = Regex("([+\\d][\\d\\s-]{4,})").find(original)?.value?.trim()
            val target = digits ?: original.replace(Regex("(?i)call|কল করো|কল দাও|কল|ফোন করো"), "").trim().ifBlank { "01700000000" }
            val msg = if (isBn) {
                "$target-এ কল করার আগে আপনার নিশ্চিতকরণ প্রয়োজন।"
            } else {
                "Please confirm before I open the dialer for $target."
            }
            return DeviceCommand.RequestCall(target) to msg
        }

        // SMS / Message (Always confirmation required)
        if (containsAny(lower, "মেসেজ", "এসএমএস", "sms", "message", "বার্তা পাঠাও")) {
            val digits = Regex("([+\\d][\\d\\s-]{4,})").find(original)?.value?.trim() ?: ""
            val msg = if (isBn) {
                "বার্তা পাঠানোর আগে আপনার নিশ্চিতকরণ প্রয়োজন।"
            } else {
                "Please confirm your message details before sending."
            }
            return DeviceCommand.RequestSms(digits, original) to msg
        }

        // Open App
        if (containsAny(lower, "অ্যাপ খোলো", "খোলো", "open ")) {
            val appQuery = original.replace(Regex("(?i)open|app|অ্যাপ|খোলো"), "").trim()
            if (appQuery.isNotBlank()) {
                val msg = if (isBn) {
                    "\"$appQuery\" অ্যাপটি খোলার চেষ্টা করছি।"
                } else {
                    "Opening $appQuery."
                }
                return DeviceCommand.OpenApp(appQuery) to msg
            }
        }

        return null
    }

    private fun buildSampleSources(lower: String, isBn: Boolean): List<WebSource> {
        return when {
            containsAny(lower, "আবহাওয়া", "weather") -> listOf(
                WebSource(
                    title = if (isBn) "বাংলাদেশ আবহাওয়া অধিদপ্তর" else "Bangladesh Meteorological Dept.",
                    url = "https://live8.bmd.gov.bd/",
                    snippet = if (isBn) "সরকারি আবহাওয়া পূর্বাভাস ও সতর্কতা" else "Official weather forecast and alerts"
                ),
                WebSource(
                    title = "Open-Meteo Weather",
                    url = "https://open-meteo.com/",
                    snippet = if (isBn) "বর্তমান তাপমাত্রা ও বৃষ্টির পূর্বাভাস" else "Live temperature and rainfall forecast"
                )
            )

            containsAny(lower, "ক্রিকেট", "স্কোর", "cricket", "score") -> listOf(
                WebSource(
                    title = "ESPNcricinfo Live Scores",
                    url = "https://www.espncricinfo.com/live-cricket-score",
                    snippet = if (isBn) "লাইভ ক্রিকেট স্কোর ও বল-বাই-বল আপডেট" else "Live cricket scores and match commentary"
                )
            )

            else -> listOf(
                WebSource(
                    title = if (isBn) "বিবিসি বাংলা" else "BBC News",
                    url = if (isBn) "https://www.bbc.com/bengali" else "https://www.bbc.com/news",
                    snippet = if (isBn) "সর্বশেষ বিশ্ব ও বাংলাদেশের খবর" else "Latest world and regional headlines"
                ),
                WebSource(
                    title = if (isBn) "প্রথম আলো" else "Wikipedia",
                    url = if (isBn) "https://www.prothomalo.com/" else "https://en.wikipedia.org/",
                    snippet = if (isBn) "দৈনিক সংবাদ ও তথ্যসূত্র" else "General knowledge and reference"
                )
            )
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
