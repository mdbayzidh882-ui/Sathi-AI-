package com.example.model

enum class AppLanguage(val code: String, val localeTag: String) {
    BENGALI("bn", "bn-BD"),
    ENGLISH("en", "en-US")
}

enum class SathiVoice(val id: String) {
    FEMALE_MISTY("female_misty"),
    MALE_AYAN("male_ayan")
}

enum class MoodTone {
    WARM,
    JOYFUL,
    COMFORTING,
    SERIOUS
}

enum class PlanTier {
    FREE,
    PREMIUM,
    OWNER
}

enum class OfflinePackState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    READY
}

enum class OfflinePackVariant(val sizeLabelBn: String, val sizeLabelEn: String, val totalMb: Int) {
    STANDARD_1_5_GB("১.৫ GB (পূর্ণ প্যাক)", "1.5 GB (Full Pack)", 1536),
    COMPACT_650_MB("৬৫০ MB (পুরনো ফোনের জন্য ছোট প্যাক)", "650 MB (Compact Pack for Older Phones)", 650)
}

enum class VoicePhase {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class PremiumToolType {
    CAMERA_VISION,
    SUMMARIZE,
    TRANSLATE,
    WRITE_MESSAGE,
    DAY_PLAN,
    MORNING_GREETING
}

data class WebSource(
    val title: String,
    val url: String,
    val snippet: String
)

sealed interface DeviceCommand {
    data class OpenYouTube(val query: String) : DeviceCommand
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : DeviceCommand
    data class SetTimer(val seconds: Int, val label: String) : DeviceCommand
    data class CreateReminder(val title: String) : DeviceCommand
    data class ToggleFlashlight(val enable: Boolean?) : DeviceCommand
    data class AdjustVolume(val delta: Int) : DeviceCommand
    data class OpenApp(val appName: String) : DeviceCommand
    data class RequestCall(val target: String) : DeviceCommand
    data class RequestSms(val target: String, val message: String) : DeviceCommand
}

sealed interface PendingPhoneConfirmation {
    data class Call(val target: String) : PendingPhoneConfirmation
    data class Sms(val target: String, val body: String) : PendingPhoneConfirmation
}

data class AiReplyResult(
    val text: String,
    val mood: MoodTone = MoodTone.WARM,
    val sources: List<WebSource> = emptyList(),
    val deviceCommand: DeviceCommand? = null,
    val isPlaceholderResponse: Boolean = true
)

data class UserSettings(
    val language: AppLanguage = AppLanguage.BENGALI,
    val voice: SathiVoice = SathiVoice.FEMALE_MISTY,
    val planTier: PlanTier = PlanTier.FREE,
    val activeCodeLabel: String = "",
    val failedCodeAttempts: Int = 0,
    val codeLockoutUntilMillis: Long = 0L,
    val offlinePackState: OfflinePackState = OfflinePackState.NOT_DOWNLOADED,
    val offlinePackVariant: OfflinePackVariant = OfflinePackVariant.STANDARD_1_5_GB,
    val offlineDownloadProgress: Int = 0,
    val offlineWifiOnly: Boolean = true,
    val offlineModeForced: Boolean = false,
    val userName: String = ""
)
