package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.example.model.AppLanguage
import com.example.model.DeviceCommand

class PhoneActionExecutor(private val context: Context) {
    private val appContext = context.applicationContext
    var isTorchOn: Boolean = false
        private set

    fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isWifiConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun executeNonConfirmedCommand(
        command: DeviceCommand,
        language: AppLanguage
    ): String {
        val isBn = language == AppLanguage.BENGALI
        return when (command) {
            is DeviceCommand.OpenYouTube -> openYouTube(command.query, isBn)
            is DeviceCommand.SetAlarm -> setAlarm(command.hour, command.minute, command.label, isBn)
            is DeviceCommand.SetTimer -> setTimer(command.seconds, command.label, isBn)
            is DeviceCommand.CreateReminder -> createCalendarReminder(command.title, isBn)
            is DeviceCommand.ToggleFlashlight -> toggleFlashlight(command.enable, isBn)
            is DeviceCommand.AdjustVolume -> adjustMediaVolume(command.delta, isBn)
            is DeviceCommand.OpenApp -> openAppByName(command.appName, isBn)
            is DeviceCommand.RequestCall, is DeviceCommand.RequestSms -> ""
        }
    }

    fun toggleFlashlight(enable: Boolean?, isBn: Boolean): String {
        return try {
            val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                return if (isBn) "এই ডিভাইসে ফ্ল্যাশলাইট হার্ডওয়্যার পাওয়া যায়নি।" else "No flashlight hardware found on this device."
            }
            val nextState = enable ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, nextState)
            isTorchOn = nextState
            if (isBn) {
                if (nextState) "ফ্ল্যাশলাইট চালু হয়েছে।" else "ফ্ল্যাশলাইট বন্ধ হয়েছে।"
            } else {
                if (nextState) "Flashlight turned ON." else "Flashlight turned OFF."
            }
        } catch (e: Exception) {
            if (isBn) "ফ্ল্যাশলাইট নিয়ন্ত্রণ করা যায়নি।" else "Unable to toggle flashlight: ${e.localizedMessage}"
        }
    }

    fun adjustMediaVolume(delta: Int, isBn: Boolean): String {
        return try {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val target = (current + delta).coerceIn(0, max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            val pct = if (max > 0) (target * 100) / max else 0
            if (isBn) "মিডিয়া ভলিউম এখন $pct%।" else "Media volume set to $pct%."
        } catch (e: Exception) {
            if (isBn) "ভলিউম পরিবর্তন করা যায়নি।" else "Could not adjust volume."
        }
    }

    fun openYouTube(query: String, isBn: Boolean): String {
        return try {
            val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            if (isBn) "ইউটিউবে \"$query\" খোলা হয়েছে।" else "Opened YouTube search for \"$query\"."
        } catch (_: Exception) {
            if (isBn) "ইউটিউব বা ব্রাউজার খোলা যায়নি।" else "Could not open YouTube or browser."
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String, isBn: Boolean): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            if (isBn) "অ্যালার্ম অ্যাপ খোলা হয়েছে ($hour:${minute.toString().padStart(2, '0')})।"
            else "Opened Clock to set alarm for $hour:${minute.toString().padStart(2, '0')}."
        } catch (_: Exception) {
            if (isBn) "এই ডিভাইসে অ্যালার্ম অ্যাপ পাওয়া যায়নি।" else "No Clock app available to set alarm."
        }
    }

    fun setTimer(seconds: Int, label: String, isBn: Boolean): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            if (isBn) "${seconds / 60} মিনিটের টাইমার খোলা হয়েছে।" else "Opened timer for ${seconds / 60} min."
        } catch (_: Exception) {
            if (isBn) "এই ডিভাইসে টাইমার অ্যাপ পাওয়া যায়নি।" else "No Clock app available for timer."
        }
    }

    fun createCalendarReminder(title: String, isBn: Boolean): String {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title.take(80))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            if (isBn) "ক্যালেন্ডার রিমাইন্ডার খোলা হয়েছে।" else "Opened Calendar to save reminder."
        } catch (_: Exception) {
            if (isBn) "ক্যালেন্ডার অ্যাপ পাওয়া যায়নি।" else "No Calendar app available."
        }
    }

    fun openAppByName(name: String, isBn: Boolean): String {
        val lower = name.lowercase()
        val packageCandidates = when {
            lower.contains("youtube") || lower.contains("ইউটিউব") -> listOf("com.google.android.youtube")
            lower.contains("chrome") || lower.contains("ক্রোম") -> listOf("com.android.chrome")
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> listOf("com.whatsapp")
            lower.contains("maps") || lower.contains("ম্যাপ") -> listOf("com.google.android.apps.maps")
            lower.contains("clock") || lower.contains("ঘড়ি") -> listOf("com.google.android.deskclock")
            lower.contains("settings") || lower.contains("সেটিংস") -> listOf("com.android.settings")
            else -> emptyList()
        }

        val pm: PackageManager = appContext.packageManager
        for (pkg in packageCandidates) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(launch)
                return if (isBn) "$name খোলা হয়েছে।" else "Opened $name."
            }
        }

        if (lower.contains("settings") || lower.contains("সেটিংস")) {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            return if (isBn) "ফোনের সেটিংস খোলা হয়েছে।" else "Opened device Settings."
        }

        return if (isBn) "\"$name\" অ্যাপটি এই ডিভাইসে ইনস্টল করা নেই।" else "App \"$name\" was not found on this device."
    }

    fun launchConfirmedCall(target: String, isBn: Boolean): String {
        return try {
            val cleaned = target.filter { it.isDigit() || it == '+' }.ifBlank { target }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleaned)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            if (isBn) "নিশ্চিত করার পর ডায়ালার খোলা হয়েছে ($cleaned)।" else "Opened dialer for $cleaned."
        } catch (_: Exception) {
            if (isBn) "ফোন ডায়ালার খোলা যায়নি।" else "Unable to open phone dialer."
        }
    }

    fun launchConfirmedSms(target: String, message: String, isBn: Boolean): String {
        return try {
            val cleaned = target.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(cleaned)}")).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            if (isBn) "নিশ্চিত করার পর মেসেজ অ্যাপ খোলা হয়েছে।" else "Opened Messages app with your draft."
        } catch (_: Exception) {
            if (isBn) "মেসেজ অ্যাপ খোলা যায়নি।" else "Unable to open Messages app."
        }
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
