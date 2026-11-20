package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiRepository
import com.example.ai.PlaceholderAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.UserSettingsRepository
import com.example.device.PhoneActionExecutor
import com.example.device.VoiceController
import com.example.model.AiReplyResult
import com.example.model.AppLanguage
import com.example.model.DeviceCommand
import com.example.model.MoodTone
import com.example.model.OfflinePackState
import com.example.model.OfflinePackVariant
import com.example.model.PendingPhoneConfirmation
import com.example.model.PlanTier
import com.example.model.PremiumToolType
import com.example.model.SathiVoice
import com.example.model.UserSettings
import com.example.model.VoicePhase
import com.example.model.WebSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SathiUiState(
    val settings: UserSettings = UserSettings(),
    val messages: List<ConversationMessageEntity> = emptyList(),
    val memories: List<MemoryEntity> = emptyList(),
    val voicePhase: VoicePhase = VoicePhase.IDLE,
    val partialTranscript: String = "",
    val rmsLevel: Float = 0f,
    val currentMood: MoodTone = MoodTone.WARM,
    val isOnline: Boolean = true,
    val isFlashlightOn: Boolean = false,
    val pendingConfirmation: PendingPhoneConfirmation? = null,
    val statusBannerMessage: String? = null,
    val latestSources: List<WebSource> = emptyList(),
    val visionResult: AiReplyResult? = null,
    val premiumToolResult: AiReplyResult? = null,
    val codeFeedback: String? = null,
    val isProcessingAi: Boolean = false
)

class SathiViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).sathiDao()
    private val settingsRepo = UserSettingsRepository(application)
    private val aiRepository: AiRepository = PlaceholderAiRepository()
    private val voiceController = VoiceController(application)
    private val phoneActionExecutor = PhoneActionExecutor(application)

    private val localTransient = MutableStateFlow(TransientUi())
    private var offlineDownloadJob: Job? = null

    private data class TransientUi(
        val isThinking: Boolean = false,
        val currentMood: MoodTone = MoodTone.WARM,
        val isOnline: Boolean = true,
        val isFlashlightOn: Boolean = false,
        val pendingConfirmation: PendingPhoneConfirmation? = null,
        val statusBannerMessage: String? = null,
        val latestSources: List<WebSource> = emptyList(),
        val visionResult: AiReplyResult? = null,
        val premiumToolResult: AiReplyResult? = null,
        val codeFeedback: String? = null
    )

    val uiState: StateFlow<SathiUiState> = combine(
        settingsRepo.settingsFlow,
        dao.observeMessages(),
        dao.observeMemories(),
        voiceController.isListening,
        voiceController.isSpeaking,
        voiceController.partialTranscript,
        voiceController.rmsLevel,
        localTransient
    ) { values ->
        val settings = values[0] as UserSettings
        @Suppress("UNCHECKED_CAST")
        val messages = values[1] as List<ConversationMessageEntity>
        @Suppress("UNCHECKED_CAST")
        val memories = values[2] as List<MemoryEntity>
        val isListening = values[3] as Boolean
        val isSpeaking = values[4] as Boolean
        val partial = values[5] as String
        val rms = values[6] as Float
        val transient = values[7] as TransientUi

        val phase = when {
            isListening -> VoicePhase.LISTENING
            transient.isThinking -> VoicePhase.THINKING
            isSpeaking -> VoicePhase.SPEAKING
            else -> VoicePhase.IDLE
        }

        SathiUiState(
            settings = settings,
            messages = messages,
            memories = memories,
            voicePhase = phase,
            partialTranscript = partial,
            rmsLevel = rms,
            currentMood = transient.currentMood,
            isOnline = phoneActionExecutor.isOnline() && !settings.offlineModeForced,
            isFlashlightOn = transient.isFlashlightOn,
            pendingConfirmation = transient.pendingConfirmation,
            statusBannerMessage = transient.statusBannerMessage,
            latestSources = transient.latestSources,
            visionResult = transient.visionResult,
            premiumToolResult = transient.premiumToolResult,
            codeFeedback = transient.codeFeedback,
            isProcessingAi = transient.isThinking
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SathiUiState()
    )

    init {
        viewModelScope.launch {
            localTransient.update {
                it.copy(isOnline = phoneActionExecutor.isOnline())
            }
        }
    }

    fun onOrbAction() {
        val current = uiState.value
        when (current.voicePhase) {
            VoicePhase.SPEAKING -> {
                // True barge-in interruption: stop TTS immediately and start listening
                startVoiceListening()
            }
            VoicePhase.LISTENING -> {
                voiceController.stopListening()
            }
            VoicePhase.THINKING -> {
                // Ignore tap while thinking
            }
            VoicePhase.IDLE -> {
                startVoiceListening()
            }
        }
    }

    fun startVoiceListening() {
        val lang = uiState.value.settings.language
        voiceController.startListening(
            language = lang,
            onFinalResult = { spokenText ->
                sendUserQuery(spokenText, speakReply = true)
            },
            onErrorMessage = { err ->
                localTransient.update { it.copy(statusBannerMessage = err) }
            }
        )
    }

    fun stopSpeaking() {
        voiceController.stopSpeaking()
    }

    fun sendUserQuery(rawText: String, speakReply: Boolean = true) {
        val text = rawText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            val current = uiState.value
            val lang = current.settings.language
            val effectiveOffline = !phoneActionExecutor.isOnline() || current.settings.offlineModeForced

            dao.insertMessage(
                ConversationMessageEntity(
                    role = "user",
                    text = text
                )
            )
            localTransient.update {
                it.copy(
                    isThinking = true,
                    statusBannerMessage = null,
                    latestSources = emptyList()
                )
            }

            // Detect and remember user name automatically if they introduce themselves
            extractNameIfMentioned(text)?.let { detectedName ->
                settingsRepo.setUserName(detectedName)
                dao.insertMemory(
                    MemoryEntity(
                        category = if (lang == AppLanguage.BENGALI) "পরিচয়" else "Identity",
                        title = if (lang == AppLanguage.BENGALI) "আপনার নাম" else "Your Name",
                        content = detectedName,
                        isPinned = true
                    )
                )
            }

            val isPremium = current.settings.planTier != PlanTier.FREE
            val result = aiRepository.generateReply(
                query = text,
                language = lang,
                memories = current.memories,
                userName = current.settings.userName,
                isOffline = effectiveOffline,
                isPremium = isPremium
            )

            var actionStatus: String? = null
            result.deviceCommand?.let { cmd ->
                when (cmd) {
                    is DeviceCommand.RequestCall -> {
                        localTransient.update {
                            it.copy(pendingConfirmation = PendingPhoneConfirmation.Call(cmd.target))
                        }
                    }
                    is DeviceCommand.RequestSms -> {
                        localTransient.update {
                            it.copy(pendingConfirmation = PendingPhoneConfirmation.Sms(cmd.target, cmd.message))
                        }
                    }
                    else -> {
                        actionStatus = phoneActionExecutor.executeNonConfirmedCommand(cmd, lang)
                    }
                }
            }

            val firstSource = result.sources.firstOrNull()
            dao.insertMessage(
                ConversationMessageEntity(
                    role = "sathi",
                    text = result.text,
                    mood = result.mood.name,
                    sourceTitle = firstSource?.title,
                    sourceUrl = firstSource?.url,
                    isPlaceholderAi = result.isPlaceholderResponse,
                    actionStatus = actionStatus
                )
            )

            localTransient.update {
                it.copy(
                    isThinking = false,
                    currentMood = result.mood,
                    isFlashlightOn = phoneActionExecutor.isTorchOn,
                    latestSources = result.sources
                )
            }

            if (speakReply) {
                voiceController.speak(
                    text = result.text,
                    language = lang,
                    voice = current.settings.voice,
                    mood = result.mood
                )
            }
        }
    }

    fun triggerPhoneAction(command: DeviceCommand) {
        val lang = uiState.value.settings.language
        val isBn = lang == AppLanguage.BENGALI
        when (command) {
            is DeviceCommand.RequestCall -> {
                localTransient.update {
                    it.copy(pendingConfirmation = PendingPhoneConfirmation.Call(command.target))
                }
            }
            is DeviceCommand.RequestSms -> {
                localTransient.update {
                    it.copy(pendingConfirmation = PendingPhoneConfirmation.Sms(command.target, command.message))
                }
            }
            else -> {
                val feedback = phoneActionExecutor.executeNonConfirmedCommand(command, lang)
                localTransient.update {
                    it.copy(
                        isFlashlightOn = phoneActionExecutor.isTorchOn,
                        statusBannerMessage = feedback.ifBlank {
                            if (isBn) "কাজটি সম্পন্ন হয়েছে।" else "Action executed."
                        }
                    )
                }
            }
        }
    }

    fun confirmPhoneAction(editedTarget: String, editedBody: String) {
        val pending = localTransient.value.pendingConfirmation ?: return
        val isBn = uiState.value.settings.language == AppLanguage.BENGALI
        val resultMsg = when (pending) {
            is PendingPhoneConfirmation.Call -> phoneActionExecutor.launchConfirmedCall(editedTarget, isBn)
            is PendingPhoneConfirmation.Sms -> phoneActionExecutor.launchConfirmedSms(editedTarget, editedBody, isBn)
        }
        localTransient.update {
            it.copy(
                pendingConfirmation = null,
                statusBannerMessage = resultMsg
            )
        }
    }

    fun dismissPhoneConfirmation() {
        localTransient.update { it.copy(pendingConfirmation = null) }
    }

    fun analyzeImage(prompt: String, bitmap: Bitmap?) {
        val current = uiState.value
        val lang = current.settings.language
        if (current.settings.planTier == PlanTier.FREE) {
            localTransient.update {
                it.copy(
                    statusBannerMessage = if (lang == AppLanguage.BENGALI) {
                        "ক্যামেরা বোঝাপড়া প্রিমিয়াম ফিচার। সেটিংস থেকে প্রিমিয়াম কোড ব্যবহার করুন।"
                    } else {
                        "Camera understanding is a Premium feature. Activate a Premium code in Settings."
                    }
                )
            }
            return
        }

        viewModelScope.launch {
            localTransient.update { it.copy(isThinking = true) }
            val result = aiRepository.analyzeImage(prompt, bitmap, lang)
            localTransient.update {
                it.copy(
                    isThinking = false,
                    visionResult = result
                )
            }
        }
    }

    fun runPremiumTool(tool: PremiumToolType, input: String, targetLanguage: String = "English") {
        val current = uiState.value
        val lang = current.settings.language
        if (current.settings.planTier == PlanTier.FREE) {
            localTransient.update {
                it.copy(
                    statusBannerMessage = if (lang == AppLanguage.BENGALI) {
                        "এই ফিচারটি প্রিমিয়াম প্ল্যানের জন্য।"
                    } else {
                        "This tool requires a Premium plan."
                    }
                )
            }
            return
        }

        viewModelScope.launch {
            localTransient.update { it.copy(isThinking = true) }
            val result = aiRepository.runPremiumTool(
                tool = tool,
                input = input,
                targetLanguage = targetLanguage,
                language = lang,
                memories = current.memories,
                userName = current.settings.userName
            )
            localTransient.update {
                it.copy(
                    isThinking = false,
                    premiumToolResult = result
                )
            }
        }
    }

    fun addMemory(category: String, title: String, content: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            dao.insertMemory(
                MemoryEntity(
                    category = category.trim().ifBlank { "Note" },
                    title = title.trim(),
                    content = content.trim()
                )
            )
        }
    }

    fun updateMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            dao.updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun togglePinMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            dao.updateMemory(memory.copy(isPinned = !memory.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            dao.deleteMemoryById(id)
        }
    }

    fun deleteAllMyData() {
        viewModelScope.launch {
            dao.deleteAllMemories()
            dao.clearMessages()
            settingsRepo.clearAllUserPreferences()
            val isBn = uiState.value.settings.language == AppLanguage.BENGALI
            localTransient.update {
                it.copy(
                    visionResult = null,
                    premiumToolResult = null,
                    latestSources = emptyList(),
                    statusBannerMessage = if (isBn) {
                        "আপনার সব স্মৃতি ও কথোপকথন মুছে ফেলা হয়েছে।"
                    } else {
                        "All your saved memories and conversation history have been deleted."
                    }
                )
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepo.setLanguage(language)
        }
    }

    fun setVoice(voice: SathiVoice) {
        viewModelScope.launch {
            settingsRepo.setVoice(voice)
        }
    }

    fun previewVoice(voice: SathiVoice) {
        voiceController.previewVoice(voice, uiState.value.settings.language)
    }

    fun redeemDemoCode(rawCode: String) {
        val code = rawCode.trim().uppercase()
        val settings = uiState.value.settings
        val isBn = settings.language == AppLanguage.BENGALI
        val now = System.currentTimeMillis()

        if (now < settings.codeLockoutUntilMillis) {
            val remainingSec = ((settings.codeLockoutUntilMillis - now) / 1000L).coerceAtLeast(1L)
            localTransient.update {
                it.copy(
                    codeFeedback = if (isBn) {
                        "অনেকবার ভুল কোড দেওয়া হয়েছে। অনুগ্রহ করে $remainingSec সেকেন্ড পর আবার চেষ্টা করুন।"
                    } else {
                        "Too many incorrect attempts. Please wait $remainingSec seconds before retrying."
                    }
                )
            }
            return
        }

        viewModelScope.launch {
            val matchedTier: Pair<PlanTier, String>? = when (code) {
                "SATHI-PRO-1M" -> PlanTier.PREMIUM to (if (isBn) "১ মাসের ডেমো কোড" else "1-Month Demo Code")
                "SATHI-PRO-6M" -> PlanTier.PREMIUM to (if (isBn) "৬ মাসের ডেমো কোড" else "6-Month Demo Code")
                "SATHI-PRO-1Y" -> PlanTier.PREMIUM to (if (isBn) "১ বছরের ডেমো কোড" else "1-Year Demo Code")
                "SATHI-OWNER-DEV" -> PlanTier.OWNER to (if (isBn) "ওনার ডেমো কোড" else "Owner Demo Code")
                else -> null
            }

            if (matchedTier != null) {
                settingsRepo.setPlanTier(matchedTier.first, matchedTier.second)
                localTransient.update {
                    it.copy(
                        codeFeedback = if (isBn) {
                            "সফল! ${matchedTier.second} সক্রিয় হয়েছে।"
                        } else {
                            "Success! ${matchedTier.second} activated."
                        }
                    )
                }
            } else {
                val attempts = settings.failedCodeAttempts + 1
                val lockout = if (attempts >= 5) now + 60_000L else 0L
                settingsRepo.recordFailedCodeAttempt(attempts, lockout)
                localTransient.update {
                    it.copy(
                        codeFeedback = if (isBn) {
                            if (lockout > 0L) "৫ বার ভুল কোড। ৬০ সেকেন্ডের জন্য লক করা হয়েছে।"
                            else "কোডটি সঠিক নয় (${5 - attempts} বার বাকি)।"
                        } else {
                            if (lockout > 0L) "5 failed attempts. Locked for 60 seconds."
                            else "Invalid code (${5 - attempts} attempts left)."
                        }
                    )
                }
            }
        }
    }

    fun resetPlanToFree() {
        viewModelScope.launch {
            settingsRepo.setPlanTier(PlanTier.FREE, "")
            val isBn = uiState.value.settings.language == AppLanguage.BENGALI
            localTransient.update {
                it.copy(codeFeedback = if (isBn) "ফ্রি প্ল্যানে ফিরে গেছেন।" else "Switched back to Free plan.")
            }
        }
    }

    fun setOfflineWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            settingsRepo.setOfflineWifiOnly(wifiOnly)
        }
    }

    fun setOfflineModeForced(forced: Boolean) {
        viewModelScope.launch {
            settingsRepo.setOfflineModeForced(forced)
        }
    }

    fun startOrResumeOfflineDownload(variant: OfflinePackVariant) {
        val settings = uiState.value.settings
        val isBn = settings.language == AppLanguage.BENGALI

        if (settings.offlineWifiOnly && !phoneActionExecutor.isWifiConnected()) {
            localTransient.update {
                it.copy(
                    statusBannerMessage = if (isBn) {
                        "ওয়াই-ফাই (Wi-Fi) সংযোগ নেই। 'শুধুমাত্র Wi-Fi' চালু থাকায় ডাউনলোড থামানো হয়েছে।"
                    } else {
                        "No Wi-Fi detected. Download paused because 'Wi-Fi only' is enabled."
                    }
                )
            }
            return
        }

        offlineDownloadJob?.cancel()
        offlineDownloadJob = viewModelScope.launch {
            var progress = if (settings.offlinePackState == OfflinePackState.PAUSED) {
                settings.offlineDownloadProgress
            } else {
                0
            }
            settingsRepo.updateOfflinePack(OfflinePackState.DOWNLOADING, progress, variant)
            while (progress < 100) {
                delay(180L)
                progress = (progress + 5).coerceAtMost(100)
                val nextState = if (progress >= 100) OfflinePackState.READY else OfflinePackState.DOWNLOADING
                settingsRepo.updateOfflinePack(nextState, progress, variant)
            }
        }
    }

    fun pauseOfflineDownload() {
        offlineDownloadJob?.cancel()
        viewModelScope.launch {
            val currentProgress = uiState.value.settings.offlineDownloadProgress
            settingsRepo.updateOfflinePack(OfflinePackState.PAUSED, currentProgress)
        }
    }

    fun deleteOfflinePack() {
        offlineDownloadJob?.cancel()
        viewModelScope.launch {
            settingsRepo.updateOfflinePack(OfflinePackState.NOT_DOWNLOADED, 0)
        }
    }

    fun openUrl(url: String) {
        phoneActionExecutor.openUrl(url)
    }

    fun clearStatusBanner() {
        localTransient.update { it.copy(statusBannerMessage = null) }
    }

    private fun extractNameIfMentioned(text: String): String? {
        val patterns = listOf(
            Regex("আমার নাম\\s+([\\p{L} ]{2,30})"),
            Regex("(?i)my name is\\s+([A-Za-z ]{2,30})")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
            if (!match.isNullOrBlank()) return match
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        offlineDownloadJob?.cancel()
        voiceController.release()
    }
}
