package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.AppLanguage
import com.example.model.DeviceCommand
import com.example.model.PlanTier
import com.example.model.PremiumToolType
import com.example.ui.SathiStrings
import com.example.ui.SathiUiState
import com.example.ui.components.FirebasePlaceholderNoticeBanner
import com.example.ui.components.PermissionRationaleDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(
    uiState: SathiUiState,
    onExecuteCommand: (DeviceCommand) -> Unit,
    onAnalyzeImage: (String, Bitmap?) -> Unit,
    onRunPremiumTool: (PremiumToolType, String, String) -> Unit,
    onGoToUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = uiState.settings.language
    val isBn = lang == AppLanguage.BENGALI
    val isPremium = uiState.settings.planTier != PlanTier.FREE

    var youtubeQuery by remember { mutableStateOf("") }
    var callOrSmsTarget by remember { mutableStateOf("") }
    var smsText by remember { mutableStateOf("") }
    var appToOpen by remember { mutableStateOf("YouTube") }

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var visionPrompt by remember { mutableStateOf("") }
    var showCameraRationale by remember { mutableStateOf(false) }

    var selectedTool by remember { mutableStateOf(PremiumToolType.SUMMARIZE) }
    var premiumToolInput by remember { mutableStateOf("") }
    var translateTarget by remember { mutableStateOf("English") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bmp
            } catch (_: Exception) {
            }
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPreviewLauncher.launch(null)
        }
    }

    if (showCameraRationale) {
        PermissionRationaleDialog(
            title = SathiStrings.cameraPermissionTitle(lang),
            body = SathiStrings.cameraPermissionBody(lang),
            language = lang,
            onConfirm = {
                showCameraRationale = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = { showCameraRationale = false }
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
                    text = if (isBn) "ফোন অ্যাকশন ও প্রিমিয়াম টুলস" else "Phone Actions & Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. Free Phone Actions
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isBn) "ফ্রি ফোন কন্ট্রোল (অফলাইনেও চলে)" else "Free Phone Controls (Works Offline)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onExecuteCommand(DeviceCommand.ToggleFlashlight(null)) },
                                modifier = Modifier.testTag("tool_flashlight")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isBn) {
                                        if (uiState.isFlashlightOn) "ফ্ল্যাশলাইট বন্ধ" else "ফ্ল্যাশলাইট চালু"
                                    } else {
                                        if (uiState.isFlashlightOn) "Flashlight Off" else "Flashlight On"
                                    }
                                )
                            }

                            FilledTonalButton(
                                onClick = { onExecuteCommand(DeviceCommand.AdjustVolume(2)) },
                                modifier = Modifier.testTag("tool_volume_up")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "ভলিউম +" else "Volume +")
                            }

                            FilledTonalButton(
                                onClick = { onExecuteCommand(DeviceCommand.AdjustVolume(-2)) },
                                modifier = Modifier.testTag("tool_volume_down")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "ভলিউম -" else "Volume -")
                            }

                            FilledTonalButton(
                                onClick = { onExecuteCommand(DeviceCommand.SetAlarm(7, 0, "Sathi Morning")) },
                                modifier = Modifier.testTag("tool_alarm")
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "৭টার অ্যালার্ম" else "7:00 Alarm")
                            }

                            FilledTonalButton(
                                onClick = { onExecuteCommand(DeviceCommand.SetTimer(300, "Sathi 5m Timer")) },
                                modifier = Modifier.testTag("tool_timer")
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "৫ মিনিট টাইমার" else "5m Timer")
                            }

                            FilledTonalButton(
                                onClick = {
                                    onExecuteCommand(
                                        DeviceCommand.CreateReminder(
                                            if (isBn) "সাথী রিমাইন্ডার" else "Sathi Reminder"
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("tool_calendar")
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "ক্যালেন্ডার" else "Calendar")
                            }
                        }

                        // YouTube Search & Open
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = youtubeQuery,
                                onValueChange = { youtubeQuery = it },
                                label = { Text(if (isBn) "ইউটিউব ভিডিও/চ্যানেল খুঁজুন" else "Search YouTube video/channel") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    onExecuteCommand(
                                        DeviceCommand.OpenYouTube(
                                            youtubeQuery.ifBlank { if (isBn) "বাংলা গান" else "Bengali songs" }
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("tool_youtube_go")
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBn) "চালান" else "Play")
                            }
                        }

                        // Open Installed App
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = appToOpen,
                                onValueChange = { appToOpen = it },
                                label = { Text(if (isBn) "অ্যাপের নাম (যেমন Chrome, Settings)" else "App name (Chrome, Settings, Maps)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { onExecuteCommand(DeviceCommand.OpenApp(appToOpen)) }
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBn) "খুলুন" else "Open")
                            }
                        }

                        // Call / SMS with mandatory confirmation
                        OutlinedTextField(
                            value = callOrSmsTarget,
                            onValueChange = { callOrSmsTarget = it },
                            label = { Text(if (isBn) "কল বা মেসেজের নম্বর" else "Phone number for Call or SMS") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = smsText,
                            onValueChange = { smsText = it },
                            label = { Text(if (isBn) "মেসেজের লেখা (ঐচ্ছিক)" else "Message text (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onExecuteCommand(
                                        DeviceCommand.RequestCall(callOrSmsTarget.ifBlank { "01700000000" })
                                    )
                                },
                                modifier = Modifier.testTag("tool_call_confirm")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "কল (নিশ্চিত করে)" else "Call (Confirm)")
                            }
                            OutlinedButton(
                                onClick = {
                                    onExecuteCommand(
                                        DeviceCommand.RequestSms(
                                            callOrSmsTarget.ifBlank { "01700000000" },
                                            smsText.ifBlank { if (isBn) "হ্যালো, কেমন আছেন?" else "Hi, how are you?" }
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("tool_sms_confirm")
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "মেসেজ (নিশ্চিত করে)" else "SMS (Confirm)")
                            }
                        }
                    }
                }
            }

            // 2. Premium Gate Banner if Free
            if (!isPremium) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "প্রিমিয়াম ফিচার আনলক করুন" else "Unlock Premium Features",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = if (isBn) {
                                    "ক্যামেরা বোঝাপড়া, সারসংক্ষেপ, অনুবাদ, মেসেজ লেখা, দিনের পরিকল্পনা এবং সকালের শুভেচ্ছা প্রিমিয়াম প্ল্যানের জন্য।"
                                } else {
                                    "Camera understanding, summaries, translation, message drafting, day planning, and morning greeting are unlocked with Premium."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Button(
                                onClick = onGoToUpgrade,
                                modifier = Modifier.testTag("upgrade_to_premium_button")
                            ) {
                                Text(if (isBn) "সেটিংসে কোড দিন" else "Enter Code in Settings")
                            }
                        }
                    }
                }
            }

            // 3. Camera & Photo Understanding (Premium)
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isBn) "ক্যামেরা ও ছবি বোঝাপড়া (প্রিমিয়াম)" else "Camera & Photo Understanding (Premium)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FirebasePlaceholderNoticeBanner(language = lang)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                enabled = isPremium,
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("pick_photo_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "গ্যালারি" else "Gallery")
                            }

                            FilledTonalButton(
                                enabled = isPremium,
                                onClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        cameraPreviewLauncher.launch(null)
                                    } else {
                                        showCameraRationale = true
                                    }
                                },
                                modifier = Modifier.testTag("take_photo_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "ক্যামেরা" else "Camera")
                            }
                        }

                        selectedBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = if (isBn) "নির্বাচিত ছবি" else "Selected image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        OutlinedTextField(
                            value = visionPrompt,
                            onValueChange = { visionPrompt = it },
                            enabled = isPremium,
                            label = { Text(if (isBn) "ছবি সম্পর্কে প্রশ্ন লিখুন" else "Ask a question about this photo") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            enabled = isPremium && !uiState.isProcessingAi,
                            onClick = { onAnalyzeImage(visionPrompt, selectedBitmap) },
                            modifier = Modifier.testTag("analyze_photo_button")
                        ) {
                            Text(if (isBn) "ছবি বিশ্লেষণ করুন" else "Analyze Photo")
                        }

                        uiState.visionResult?.let { res ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = res.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Premium Writing, Translation & Day Planning Studio
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isBn) "প্রিমিয়াম সহকারী স্টুডিও" else "Premium Assistant Studio",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tools = listOf(
                                PremiumToolType.SUMMARIZE to (if (isBn) "সারসংক্ষেপ" else "Summarize"),
                                PremiumToolType.TRANSLATE to (if (isBn) "অনুবাদ" else "Translate"),
                                PremiumToolType.WRITE_MESSAGE to (if (isBn) "মেসেজ লেখা" else "Write Message"),
                                PremiumToolType.DAY_PLAN to (if (isBn) "দিনের পরিকল্পনা" else "Day Plan"),
                                PremiumToolType.MORNING_GREETING to (if (isBn) "সকালের শুভেচ্ছা" else "Morning Greeting")
                            )
                            tools.forEach { (type, label) ->
                                FilterChip(
                                    selected = selectedTool == type,
                                    onClick = { selectedTool = type },
                                    label = { Text(label) }
                                )
                            }
                        }

                        if (selectedTool == PremiumToolType.TRANSLATE) {
                            OutlinedTextField(
                                value = translateTarget,
                                onValueChange = { translateTarget = it },
                                enabled = isPremium,
                                label = { Text(if (isBn) "কোন ভাষায় অনুবাদ করবেন?" else "Target language") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (selectedTool != PremiumToolType.MORNING_GREETING) {
                            OutlinedTextField(
                                value = premiumToolInput,
                                onValueChange = { premiumToolInput = it },
                                enabled = isPremium,
                                label = { Text(if (isBn) "এখানে লেখা বা বিষয় দিন" else "Enter text or topic") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            enabled = isPremium && !uiState.isProcessingAi,
                            onClick = { onRunPremiumTool(selectedTool, premiumToolInput, translateTarget) },
                            modifier = Modifier.testTag("run_premium_tool_button")
                        ) {
                            Text(if (isBn) "তৈরি করুন" else "Generate")
                        }

                        uiState.premiumToolResult?.let { res ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = res.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
