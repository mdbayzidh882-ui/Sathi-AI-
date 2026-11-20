package com.example.ui

import com.example.model.AppLanguage
import com.example.model.MoodTone
import com.example.model.PlanTier
import com.example.model.VoicePhase

object SathiStrings {
    fun tabTalk(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) "কথা" else "Talk"
    fun tabTools(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) "টুলস" else "Tools"
    fun tabMemory(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) "স্মৃতি" else "Memory"
    fun tabSettings(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) "সেটিংস" else "Settings"

    fun appTitle(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) "সাথী" else "Sathi"
    fun appSubtitle(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) {
        "আপনার ব্যক্তিগত ভয়েস ও দৈনন্দিন সাথী"
    } else {
        "Your personal Bengali & English AI companion"
    }

    fun firebaseNotice(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) {
        "প্লেসহোল্ডার এআই মোড: লাইভ জেমিনি এআই ও ক্লাউড সিঙ্ক চালু করতে ফায়ারবেস (Firebase) সংযুক্ত করুন।"
    } else {
        "Placeholder AI Mode: Sample responses active. Enable Firebase to connect live Gemini AI & cloud sync."
    }

    fun voicePhaseLabel(phase: VoicePhase, lang: AppLanguage): String {
        val isBn = lang == AppLanguage.BENGALI
        return when (phase) {
            VoicePhase.IDLE -> if (isBn) "কথা বলতে ট্যাপ করুন" else "Tap orb to talk"
            VoicePhase.LISTENING -> if (isBn) "শুনছি... কথা বলুন" else "Listening... speak now"
            VoicePhase.THINKING -> if (isBn) "ভাবছি..." else "Thinking..."
            VoicePhase.SPEAKING -> if (isBn) "কথা বলছি — থামাতে ট্যাপ করুন" else "Speaking — tap to interrupt"
        }
    }

    fun moodLabel(mood: MoodTone, lang: AppLanguage): String {
        val isBn = lang == AppLanguage.BENGALI
        return when (mood) {
            MoodTone.WARM -> if (isBn) "আন্তরিক" else "Warm"
            MoodTone.JOYFUL -> if (isBn) "আনন্দিত" else "Joyful"
            MoodTone.COMFORTING -> if (isBn) "সহানুভূতিশীল" else "Comforting"
            MoodTone.SERIOUS -> if (isBn) "মনোযোগী" else "Focused"
        }
    }

    fun planLabel(tier: PlanTier, lang: AppLanguage): String {
        val isBn = lang == AppLanguage.BENGALI
        return when (tier) {
            PlanTier.FREE -> if (isBn) "ফ্রি প্ল্যান" else "Free Plan"
            PlanTier.PREMIUM -> if (isBn) "প্রিমিয়াম প্ল্যান" else "Premium Plan"
            PlanTier.OWNER -> if (isBn) "ওনার আনলিমিটেড" else "Owner Unlimited"
        }
    }

    fun micPermissionTitle(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) {
        "মাইক্রোফোন অনুমতি প্রয়োজন"
    } else {
        "Microphone Permission Needed"
    }

    fun micPermissionBody(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) {
        "সাথীর সাথে সরাসরি কথা বলার জন্য মাইক্রোফোন ব্যবহারের অনুমতি দিন। আপনার অনুমতি ছাড়া অডিও সংরক্ষণ করা হয় না।"
    } else {
        "Allow microphone access so Sathi can listen to your voice in Bengali or English. Audio is never stored without your consent."
    }

    fun cameraPermissionTitle(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) {
        "ক্যামেরা অনুমতি প্রয়োজন"
    } else {
        "Camera Permission Needed"
    }

    fun cameraPermissionBody(lang: AppLanguage) = if (lang == AppLanguage.BENGALI) {
        "ছবি তুলে বিশ্লেষণ করার জন্য ক্যামেরার অনুমতি প্রয়োজন। চাইলে গ্যালারি থেকেও ছবি বেছে নিতে পারেন।"
    } else {
        "Allow camera access to snap a photo for understanding, or pick a photo directly from your gallery."
    }
}
