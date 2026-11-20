package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.VoicePhase
import com.example.ui.SathiStrings
import com.example.ui.SathiViewModel
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.components.PhoneConfirmationDialog
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TalkScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.SathiTheme

const val ROUTE_TALK = "talk"
const val ROUTE_TOOLS = "tools"
const val ROUTE_MEMORY = "memory"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SathiTheme {
                SathiAppRoot()
            }
        }
    }
}

@Composable
fun SathiAppRoot(viewModel: SathiViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = uiState.settings.language
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TALK

    val snackbarHostState = remember { SnackbarHostState() }
    var showMicRationale by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onOrbAction()
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    if (showMicRationale) {
        PermissionRationaleDialog(
            title = SathiStrings.micPermissionTitle(lang),
            body = SathiStrings.micPermissionBody(lang),
            language = lang,
            onConfirm = {
                showMicRationale = false
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = { showMicRationale = false }
        )
    }

    uiState.pendingConfirmation?.let { pending ->
        PhoneConfirmationDialog(
            confirmation = pending,
            language = lang,
            onConfirm = { target, body -> viewModel.confirmPhoneAction(target, body) },
            onDismiss = { viewModel.dismissPhoneConfirmation() }
        )
    }

    val destinations = remember(lang) {
        listOf(
            NavDestinationItem(
                route = ROUTE_TALK,
                label = SathiStrings.tabTalk(lang),
                selectedIcon = Icons.Filled.Mic,
                unselectedIcon = Icons.Outlined.Mic,
                tag = "nav_talk"
            ),
            NavDestinationItem(
                route = ROUTE_TOOLS,
                label = SathiStrings.tabTools(lang),
                selectedIcon = Icons.Filled.Build,
                unselectedIcon = Icons.Outlined.Build,
                tag = "nav_tools"
            ),
            NavDestinationItem(
                route = ROUTE_MEMORY,
                label = SathiStrings.tabMemory(lang),
                selectedIcon = Icons.Filled.Psychology,
                unselectedIcon = Icons.Outlined.Psychology,
                tag = "nav_memory"
            ),
            NavDestinationItem(
                route = ROUTE_SETTINGS,
                label = SathiStrings.tabSettings(lang),
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                tag = "nav_settings"
            )
        )
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        destinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        destinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_TALK
                    ) {
                        composable(ROUTE_TALK) {
                            TalkScreen(
                                uiState = uiState,
                                onOrbClick = {
                                    if (uiState.voicePhase == VoicePhase.SPEAKING ||
                                        uiState.voicePhase == VoicePhase.IDLE
                                    ) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            viewModel.onOrbAction()
                                        } else {
                                            if (uiState.voicePhase == VoicePhase.SPEAKING) {
                                                viewModel.stopSpeaking()
                                            }
                                            showMicRationale = true
                                        }
                                    } else {
                                        viewModel.onOrbAction()
                                    }
                                },
                                onSendText = { viewModel.sendUserQuery(it, speakReply = true) },
                                onOpenUrl = { viewModel.openUrl(it) },
                                onNavigateToVision = { navigateTo(ROUTE_TOOLS) }
                            )
                        }

                        composable(ROUTE_TOOLS) {
                            ToolsScreen(
                                uiState = uiState,
                                onExecuteCommand = { viewModel.triggerPhoneAction(it) },
                                onAnalyzeImage = { prompt, bmp -> viewModel.analyzeImage(prompt, bmp) },
                                onRunPremiumTool = { tool, input, target ->
                                    viewModel.runPremiumTool(tool, input, target)
                                },
                                onGoToUpgrade = { navigateTo(ROUTE_SETTINGS) }
                            )
                        }

                        composable(ROUTE_MEMORY) {
                            MemoryScreen(
                                uiState = uiState,
                                onAddMemory = { cat, title, content ->
                                    viewModel.addMemory(cat, title, content)
                                },
                                onUpdateMemory = { viewModel.updateMemory(it) },
                                onTogglePin = { viewModel.togglePinMemory(it) },
                                onDeleteMemory = { viewModel.deleteMemory(it) },
                                onDeleteAllData = { viewModel.deleteAllMyData() }
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                onSetLanguage = { viewModel.setLanguage(it) },
                                onSetVoice = { viewModel.setVoice(it) },
                                onPreviewVoice = { viewModel.previewVoice(it) },
                                onRedeemCode = { viewModel.redeemDemoCode(it) },
                                onResetPlan = { viewModel.resetPlanToFree() },
                                onStartOfflineDownload = { viewModel.startOrResumeOfflineDownload(it) },
                                onPauseOfflineDownload = { viewModel.pauseOfflineDownload() },
                                onDeleteOfflinePack = { viewModel.deleteOfflinePack() },
                                onSetWifiOnly = { viewModel.setOfflineWifiOnly(it) },
                                onSetOfflineForced = { viewModel.setOfflineModeForced(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
