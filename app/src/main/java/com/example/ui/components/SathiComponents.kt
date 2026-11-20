package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.MoodTone
import com.example.model.PendingPhoneConfirmation
import com.example.model.VoicePhase
import com.example.ui.SathiStrings
import com.example.ui.theme.MoodComforting
import com.example.ui.theme.MoodJoyful
import com.example.ui.theme.MoodSerious
import com.example.ui.theme.MoodWarm

@Composable
fun SathiVoiceOrb(
    phase: VoicePhase,
    mood: MoodTone,
    rmsLevel: Float,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (phase) {
                    VoicePhase.LISTENING -> 650
                    VoicePhase.SPEAKING -> 750
                    VoicePhase.THINKING -> 500
                    VoicePhase.IDLE -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val rmsScale by animateFloatAsState(
        targetValue = 1f + (rmsLevel * 0.25f),
        animationSpec = spring(),
        label = "rms_scale"
    )

    val baseMoodColor by animateColorAsState(
        targetValue = when (mood) {
            MoodTone.JOYFUL -> MoodJoyful
            MoodTone.COMFORTING -> MoodComforting
            MoodTone.SERIOUS -> MoodSerious
            MoodTone.WARM -> MoodWarm
        },
        label = "mood_color"
    )

    val activeRingColor = when (phase) {
        VoicePhase.LISTENING -> MaterialTheme.colorScheme.secondary
        VoicePhase.SPEAKING -> MaterialTheme.colorScheme.tertiary
        VoicePhase.THINKING -> MaterialTheme.colorScheme.primary
        VoicePhase.IDLE -> baseMoodColor
    }

    val orbDescription = stringResource(R.string.cd_sathi_orb)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    role = Role.Button,
                    onClick = onClick
                )
                .semantics { contentDescription = orbDescription }
                .testTag("voice_orb_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val pulseRadius = (radius * 0.88f * breathe * rmsScale).coerceAtMost(radius)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeRingColor.copy(alpha = 0.26f),
                            activeRingColor.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    ),
                    radius = pulseRadius
                )

                drawCircle(
                    color = activeRingColor.copy(alpha = 0.45f),
                    radius = radius * 0.72f * breathe,
                    style = Stroke(width = 3.dp.toPx())
                )

                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            activeRingColor,
                            baseMoodColor.copy(alpha = 0.9f)
                        )
                    ),
                    radius = radius * 0.56f
                )
            }

            Icon(
                imageVector = when (phase) {
                    VoicePhase.SPEAKING -> Icons.Default.Stop
                    VoicePhase.LISTENING -> Icons.Default.GraphicEq
                    VoicePhase.THINKING -> Icons.Default.GraphicEq
                    VoicePhase.IDLE -> Icons.Default.Mic
                },
                contentDescription = stringResource(R.string.cd_mic_toggle),
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = SathiStrings.voicePhaseLabel(phase, language),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FirebasePlaceholderNoticeBanner(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = SathiStrings.firebaseNotice(language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    title: String,
    body: String,
    language: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isBn = language == AppLanguage.BENGALI
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text = body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("permission_allow_button")
            ) {
                Text(if (isBn) "অনুমতি দিন" else "Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "এখন নয়" else "Not now")
            }
        }
    )
}

@Composable
fun PhoneConfirmationDialog(
    confirmation: PendingPhoneConfirmation,
    language: AppLanguage,
    onConfirm: (target: String, message: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isBn = language == AppLanguage.BENGALI
    var target by remember(confirmation) {
        mutableStateOf(
            when (confirmation) {
                is PendingPhoneConfirmation.Call -> confirmation.target
                is PendingPhoneConfirmation.Sms -> confirmation.target
            }
        )
    }
    var smsBody by remember(confirmation) {
        mutableStateOf(
            when (confirmation) {
                is PendingPhoneConfirmation.Call -> ""
                is PendingPhoneConfirmation.Sms -> confirmation.body
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (confirmation is PendingPhoneConfirmation.Call) Icons.Default.Call else Icons.Default.Sms,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = when (confirmation) {
                    is PendingPhoneConfirmation.Call -> if (isBn) "কল নিশ্চিত করুন" else "Confirm Call"
                    is PendingPhoneConfirmation.Sms -> if (isBn) "বার্তা নিশ্চিত করুন" else "Confirm Message"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isBn) {
                        "আপনার অনুমতি ছাড়া সাথী কখনো কল বা মেসেজ পাঠায় না। নিচের তথ্য যাচাই করুন:"
                    } else {
                        "Sathi never places calls or sends messages without your confirmation. Please review:"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text(if (isBn) "নম্বর বা প্রাপক" else "Phone number or recipient") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (confirmation is PendingPhoneConfirmation.Sms) {
                    OutlinedTextField(
                        value = smsBody,
                        onValueChange = { smsBody = it },
                        label = { Text(if (isBn) "বার্তা" else "Message text") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(target, smsBody) },
                modifier = Modifier.testTag("confirm_phone_action_button")
            ) {
                Text(if (isBn) "নিশ্চিত করুন" else "Confirm")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}
