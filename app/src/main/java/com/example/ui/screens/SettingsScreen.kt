package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.OfflinePackState
import com.example.model.OfflinePackVariant
import com.example.model.PlanTier
import com.example.model.SathiVoice
import com.example.ui.SathiStrings
import com.example.ui.SathiUiState

@Composable
fun SettingsScreen(
    uiState: SathiUiState,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetVoice: (SathiVoice) -> Unit,
    onPreviewVoice: (SathiVoice) -> Unit,
    onRedeemCode: (String) -> Unit,
    onResetPlan: () -> Unit,
    onStartOfflineDownload: (OfflinePackVariant) -> Unit,
    onPauseOfflineDownload: () -> Unit,
    onDeleteOfflinePack: () -> Unit,
    onSetWifiOnly: (Boolean) -> Unit,
    onSetOfflineForced: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val isBn = settings.language == AppLanguage.BENGALI

    var codeInput by remember { mutableStateOf("") }
    var selectedVariant by remember(settings.offlinePackVariant) {
        mutableStateOf(settings.offlinePackVariant)
    }
    var showOfflineConsentDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    if (showOfflineConsentDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineConsentDialog = false },
            title = {
                Text(if (isBn) "অফলাইন প্যাক ডাউনলোড নিশ্চিত করুন" else "Confirm Offline Pack Download")
            },
            text = {
                val sizeStr = if (isBn) selectedVariant.sizeLabelBn else selectedVariant.sizeLabelEn
                Text(
                    if (isBn) {
                        "আপনি কি $sizeStr অফলাইন প্যাক ডাউনলোড করতে চান? আপনার অনুমতি ছাড়া কোনো ফাইল ডাউনলোড হয় না। (ডেমো সিমুলেশন — পূর্ণ অফলাইন LLM প্যাকের জন্য পরবর্তীতে স্থানীয় মডেল যুক্ত হবে।)"
                    } else {
                        "Download $sizeStr for offline chat and voices? Nothing downloads without your agreement. (Local progress preview without consuming 1.5 GB mobile data.)"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOfflineConsentDialog = false
                        onStartOfflineDownload(selectedVariant)
                    }
                ) {
                    Text(if (isBn) "ডাউনলোড শুরু করুন" else "Agree & Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfflineConsentDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(if (isBn) "গোপনীয়তা নীতি (Privacy Policy)" else "Privacy Policy") },
            text = {
                Text(
                    if (isBn) {
                        "• সাথী আপনার অনুমতি ছাড়া মাইক্রোফোন বা ক্যামেরা ব্যবহার করে না।\n" +
                            "• কল বা মেসেজ পাঠানোর আগে সবসময় আপনার নিশ্চিতকরণ নেয়।\n" +
                            "• অ্যাপের ভেতর কোনো গোপন API কী সংরক্ষণ করা হয় না।\n" +
                            "• আপনার স্মৃতি ও কথোপকথন কেবল আপনার ডিভাইসেই সংরক্ষিত থাকে এবং যেকোনো সময় 'সব ডেটা মুছুন' বাটনে মুছে ফেলা যায়।\n" +
                            "• নির্মাতা: মোহাম্মদ বাইজিদ হাসান।"
                    } else {
                        "• Sathi only accesses microphone or camera after your explicit permission.\n" +
                            "• Calls and SMS always require your confirmation.\n" +
                            "• Zero secret API keys are stored inside the APK.\n" +
                            "• Memories and chat logs stay local on your device and can be wiped anytime.\n" +
                            "• Creator: Mohammad Bayzid Hasan."
                    }
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text(if (isBn) "বুঝেছি" else "Close")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = if (isBn) "সেটিংস ও অ্যাকাউন্ট" else "Settings & Preferences",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // 1. Language Switch
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "অ্যাপ ও ভয়েসের ভাষা" else "App & Voice Language",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = settings.language == AppLanguage.BENGALI,
                                onClick = { onSetLanguage(AppLanguage.BENGALI) },
                                label = { Text("বাংলা (ডিফল্ট)") },
                                modifier = Modifier.testTag("lang_bengali")
                            )
                            FilterChip(
                                selected = settings.language == AppLanguage.ENGLISH,
                                onClick = { onSetLanguage(AppLanguage.ENGLISH) },
                                label = { Text("English") },
                                modifier = Modifier.testTag("lang_english")
                            )
                        }
                    }
                }
            }

            // 2. Voice Selection & Sample Preview
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "সাথীর কণ্ঠ নির্বাচন (সিন্থেটিক)" else "Sathi Voice Choice (Synthetic)",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        VoiceChoiceRow(
                            selected = settings.voice == SathiVoice.FEMALE_MISTY,
                            title = if (isBn) "মিষ্টি — মিষ্টি তরুণী কণ্ঠ" else "Misty — Sweet Young Female",
                            subtitle = if (isBn) "আন্তরিক, নরম ও আবেগপূর্ণ কৃত্রিম কণ্ঠ" else "Warm, expressive synthetic female voice",
                            previewTag = "preview_female_voice",
                            previewLabel = if (isBn) "নমুনা শুনুন" else "Preview",
                            onSelect = { onSetVoice(SathiVoice.FEMALE_MISTY) },
                            onPreview = { onPreviewVoice(SathiVoice.FEMALE_MISTY) }
                        )

                        VoiceChoiceRow(
                            selected = settings.voice == SathiVoice.MALE_AYAN,
                            title = if (isBn) "অয়ন — মিষ্টি তরুণ কণ্ঠ" else "Ayan — Sweet Young Male",
                            subtitle = if (isBn) "শান্ত, ভদ্র ও বন্ধুত্বপূর্ণ কৃত্রিম কণ্ঠ" else "Calm, respectful synthetic male voice",
                            previewTag = "preview_male_voice",
                            previewLabel = if (isBn) "নমুনা শুনুন" else "Preview",
                            onSelect = { onSetVoice(SathiVoice.MALE_AYAN) },
                            onPreview = { onPreviewVoice(SathiVoice.MALE_AYAN) }
                        )
                    }
                }
            }

            // 3. Plan & Code Entry
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "প্ল্যান ও প্রিমিয়াম কোড" else "Plan & Activation Code",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${if (isBn) "বর্তমান প্ল্যান:" else "Active Plan:"} ${SathiStrings.planLabel(settings.planTier, settings.language)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Text(
                            text = if (isBn) {
                                "সার্ভার-সাইড কোড যাচাই ও ওয়েব অ্যাডমিন প্যানেলের জন্য Firebase প্রয়োজন। স্থানীয়ভাবে প্রিমিয়াম ফিচার পরীক্ষা করতে ডেমো কোড ব্যবহার করুন: SATHI-PRO-1M, SATHI-PRO-6M, SATHI-PRO-1Y, অথবা SATHI-OWNER-DEV।"
                            } else {
                                "Server-side code checks and the Admin web dashboard require Firebase. To test Premium tools locally right now, try demo codes: SATHI-PRO-1M, SATHI-PRO-6M, SATHI-PRO-1Y, or SATHI-OWNER-DEV."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            label = { Text(if (isBn) "কোড লিখুন" else "Enter activation code") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("promo_code_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = codeInput.isNotBlank(),
                                onClick = {
                                    onRedeemCode(codeInput)
                                    codeInput = ""
                                },
                                modifier = Modifier.testTag("redeem_code_button")
                            ) {
                                Text(if (isBn) "কোড যাচাই করুন" else "Redeem Code")
                            }
                            if (settings.planTier != PlanTier.FREE) {
                                OutlinedButton(onClick = onResetPlan) {
                                    Text(if (isBn) "ফ্রি-তে ফিরুন" else "Reset to Free")
                                }
                            }
                        }

                        uiState.codeFeedback?.let { feedback ->
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 4. Offline Pack Manager
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "অফলাইন মোড প্যাক" else "Offline Mode Pack",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isBn) "শুধুমাত্র Wi-Fi দিয়ে ডাউনলোড" else "Download on Wi-Fi only",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = settings.offlineWifiOnly,
                                onCheckedChange = onSetWifiOnly
                            )
                        }

                        OfflinePackVariant.entries.forEach { variant ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedVariant == variant,
                                    onClick = { selectedVariant = variant }
                                )
                                Text(
                                    text = if (isBn) variant.sizeLabelBn else variant.sizeLabelEn,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (settings.offlinePackState == OfflinePackState.DOWNLOADING ||
                            settings.offlinePackState == OfflinePackState.PAUSED
                        ) {
                            LinearProgressIndicator(
                                progress = { settings.offlineDownloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${settings.offlineDownloadProgress}%",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (settings.offlinePackState) {
                                OfflinePackState.NOT_DOWNLOADED -> {
                                    Button(
                                        onClick = { showOfflineConsentDialog = true },
                                        modifier = Modifier.testTag("download_offline_pack_button")
                                    ) {
                                        Text(if (isBn) "প্যাক ডাউনলোড করুন" else "Download Pack")
                                    }
                                }
                                OfflinePackState.DOWNLOADING -> {
                                    OutlinedButton(onClick = onPauseOfflineDownload) {
                                        Icon(Icons.Default.Pause, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isBn) "বিরতি" else "Pause")
                                    }
                                    OutlinedButton(onClick = onDeleteOfflinePack) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isBn) "মুছুন" else "Delete")
                                    }
                                }
                                OfflinePackState.PAUSED -> {
                                    Button(onClick = { onStartOfflineDownload(selectedVariant) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isBn) "পুনরায় শুরু" else "Resume")
                                    }
                                    OutlinedButton(onClick = onDeleteOfflinePack) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isBn) "মুছুন" else "Delete")
                                    }
                                }
                                OfflinePackState.READY -> {
                                    OutlinedButton(onClick = onDeleteOfflinePack) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isBn) "অফলাইন প্যাক মুছুন" else "Delete Pack")
                                    }
                                }
                            }
                        }

                        if (settings.offlinePackState == OfflinePackState.READY) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isBn) "অফলাইন চ্যাট মোড চালু করুন" else "Use Offline Mode now",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Switch(
                                    checked = settings.offlineModeForced,
                                    onCheckedChange = onSetOfflineForced
                                )
                            }
                        }
                    }
                }
            }

            // 5. Privacy Policy & Creator Attribution
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "নিরাপত্তা, গোপনীয়তা ও নির্মাতা" else "Privacy, Security & Creator",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Text(
                            text = if (isBn) {
                                "নির্মাতা: মোহাম্মদ বাইজিদ হাসান\nGitHub Actions APK বিল্ড ওয়ার্কফ্লো প্রজেক্টে যুক্ত করা হয়েছে।"
                            } else {
                                "Creator: Mohammad Bayzid Hasan\nCloud APK build workflow (.github/workflows/android-apk.yml) included."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.testTag("privacy_policy_button")
                        ) {
                            Text(if (isBn) "গোপনীয়তা নীতি পড়ুন" else "Read Privacy Policy")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceChoiceRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    previewTag: String,
    previewLabel: String,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier.testTag(previewTag)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(previewLabel)
            }
        }
    }
}
