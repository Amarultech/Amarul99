package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.VoiceAssistantEngine
import com.example.core.configureAppCheck
import com.example.data.repository.AiRepository
import com.example.data.repository.JarvisRepository
import com.example.ui.screens.AccountAndAdminScreen
import com.example.ui.screens.AiStudioModuleScreen
import com.example.ui.screens.AiStudioType
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalculatorModuleScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DailyPlannerModuleScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JarvisModuleId
import com.example.ui.screens.KnowledgeCenterModuleScreen
import com.example.ui.screens.ModulesHubScreen
import com.example.ui.screens.NotesManagerModuleScreen
import com.example.ui.screens.QuickModuleTarget
import com.example.ui.screens.ReminderManagerModuleScreen
import com.example.ui.screens.TodoManagerModuleScreen
import com.example.ui.screens.VoiceHudScreen
import com.example.ui.screens.WeatherModuleScreen
import com.example.ui.screens.attemptAutoSignIn
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.HudSurface
import com.example.ui.theme.JarvisAiTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.JarvisViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_CHAT = "chat"
const val ROUTE_MODULES = "modules"
const val ROUTE_ACCOUNT = "account"
const val ROUTE_VOICE_HUD = "module_voice_hud"
const val ROUTE_PLANNER = "module_planner"
const val ROUTE_TODO = "module_todo"
const val ROUTE_NOTES = "module_notes"
const val ROUTE_REMINDERS = "module_reminders"
const val ROUTE_KNOWLEDGE = "module_knowledge"
const val ROUTE_CALCULATOR = "module_calculator"
const val ROUTE_WEATHER = "module_weather"
const val ROUTE_CONTENT_WRITER = "module_content_writer"
const val ROUTE_YOUTUBE = "module_youtube"
const val ROUTE_IMAGE_PROMPT = "module_image_prompt"
const val ROUTE_VIDEO_SCRIPT = "module_video_script"
const val ROUTE_TRANSLATOR = "module_translator"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private enum class TopNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    COMMAND(ROUTE_DASHBOARD, "Command", Icons.Filled.Dashboard),
    CHAT(ROUTE_CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
    MODULES(ROUTE_MODULES, "Modules", Icons.Filled.Apps),
    ACCOUNT(ROUTE_ACCOUNT, "Account", Icons.Filled.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            JarvisAiTheme {
                AppRootNavigation()
            }
        }
    }
}

@Composable
fun AppRootNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            JarvisAppShell(
                currentUserId = user.uid,
                firebaseUser = user
            )
        }
    }
}

@Composable
fun JarvisAppShell(
    currentUserId: String,
    firebaseUser: FirebaseUser
) {
    val context = LocalContext.current
    val voiceEngine = remember(currentUserId) { VoiceAssistantEngine(context.applicationContext) }
    DisposableEffect(voiceEngine) {
        onDispose { voiceEngine.release() }
    }

    val viewModel: JarvisViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                JarvisViewModel(
                    repository = JarvisRepository(db),
                    aiRepository = AiRepository(),
                    currentUserId = currentUserId,
                    firebaseUser = firebaseUser
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isVoiceListening by voiceEngine.isListening.collectAsStateWithLifecycle()
    val voiceRms by voiceEngine.rmsLevel.collectAsStateWithLifecycle()
    val speechRate = (profile?.speechRate ?: 1.0).toFloat()

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateModule(moduleId: JarvisModuleId) {
        viewModel.resetStudioState()
        when (moduleId) {
            JarvisModuleId.AI_CHAT -> navigateTop(ROUTE_CHAT)
            JarvisModuleId.VOICE_ASSISTANT -> navController.navigate(ROUTE_VOICE_HUD)
            JarvisModuleId.DAILY_PLANNER -> navController.navigate(ROUTE_PLANNER)
            JarvisModuleId.TODO_MANAGER -> navController.navigate(ROUTE_TODO)
            JarvisModuleId.NOTES_MANAGER -> navController.navigate(ROUTE_NOTES)
            JarvisModuleId.AI_CONTENT_WRITER -> navController.navigate(ROUTE_CONTENT_WRITER)
            JarvisModuleId.YOUTUBE_CREATOR -> navController.navigate(ROUTE_YOUTUBE)
            JarvisModuleId.IMAGE_PROMPT_GEN -> navController.navigate(ROUTE_IMAGE_PROMPT)
            JarvisModuleId.VIDEO_SCRIPT_GEN -> navController.navigate(ROUTE_VIDEO_SCRIPT)
            JarvisModuleId.TRANSLATOR -> navController.navigate(ROUTE_TRANSLATOR)
            JarvisModuleId.CALCULATOR -> navController.navigate(ROUTE_CALCULATOR)
            JarvisModuleId.WEATHER -> navController.navigate(ROUTE_WEATHER)
            JarvisModuleId.REMINDERS -> navController.navigate(ROUTE_REMINDERS)
            JarvisModuleId.KNOWLEDGE_CENTER -> navController.navigate(ROUTE_KNOWLEDGE)
            JarvisModuleId.SETTINGS -> navigateTop(ROUTE_ACCOUNT)
        }
    }

    val isTopDestination = currentRoute in TopNavDestination.entries.map { it.route }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianBlack, DeepSpace, ObsidianBlack)))
    ) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                if (!useNavRail && isTopDestination) {
                    NavigationBar(
                        containerColor = HudSurface.copy(alpha = 0.96f),
                        contentColor = TextPrimary
                    ) {
                        TopNavDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTop(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
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
                if (useNavRail && isTopDestination) {
                    NavigationRail(
                        containerColor = HudSurface.copy(alpha = 0.96f),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TopNavDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTop(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DASHBOARD,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        composable(ROUTE_DASHBOARD) {
                            DashboardScreen(
                                viewModel = viewModel,
                                isVoiceListening = isVoiceListening,
                                voiceRms = voiceRms,
                                onQuickPromptSubmit = { prompt ->
                                    viewModel.sendChatMessage(prompt) { reply ->
                                        if (profile?.voiceEnabled == true) {
                                            voiceEngine.speak(reply, speechRate)
                                        }
                                    }
                                    navigateTop(ROUTE_CHAT)
                                },
                                onOpenModule = { target ->
                                    when (target) {
                                        QuickModuleTarget.VOICE_HUD -> navController.navigate(ROUTE_VOICE_HUD)
                                        QuickModuleTarget.AI_CHAT -> navigateTop(ROUTE_CHAT)
                                        QuickModuleTarget.DAILY_PLANNER -> navController.navigate(ROUTE_PLANNER)
                                        QuickModuleTarget.CONTENT_WRITER -> navController.navigate(ROUTE_CONTENT_WRITER)
                                        QuickModuleTarget.TODO_MANAGER -> navController.navigate(ROUTE_TODO)
                                        QuickModuleTarget.WEATHER -> navController.navigate(ROUTE_WEATHER)
                                        QuickModuleTarget.REMINDERS -> navController.navigate(ROUTE_REMINDERS)
                                    }
                                },
                                onOpenConversation = { convId ->
                                    viewModel.selectConversation(convId)
                                    navigateTop(ROUTE_CHAT)
                                }
                            )
                        }

                        composable(ROUTE_CHAT) {
                            ChatScreen(
                                viewModel = viewModel,
                                onSpeakText = { text -> voiceEngine.speak(text, speechRate) }
                            )
                        }

                        composable(ROUTE_MODULES) {
                            ModulesHubScreen(
                                onModuleSelected = { mod -> navigateModule(mod) }
                            )
                        }

                        composable(ROUTE_ACCOUNT) {
                            AccountAndAdminScreen(viewModel = viewModel)
                        }

                        composable(ROUTE_VOICE_HUD) {
                            VoiceHudScreen(
                                viewModel = viewModel,
                                voiceEngine = voiceEngine,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_PLANNER) {
                            DailyPlannerModuleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_TODO) {
                            TodoManagerModuleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_NOTES) {
                            NotesManagerModuleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_REMINDERS) {
                            ReminderManagerModuleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_KNOWLEDGE) {
                            KnowledgeCenterModuleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_CALCULATOR) {
                            CalculatorModuleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_WEATHER) {
                            WeatherModuleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_CONTENT_WRITER) {
                            AiStudioModuleScreen(
                                studioType = AiStudioType.CONTENT_WRITER,
                                viewModel = viewModel,
                                onSpeakText = { voiceEngine.speak(it, speechRate) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_YOUTUBE) {
                            AiStudioModuleScreen(
                                studioType = AiStudioType.YOUTUBE_CREATOR,
                                viewModel = viewModel,
                                onSpeakText = { voiceEngine.speak(it, speechRate) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_IMAGE_PROMPT) {
                            AiStudioModuleScreen(
                                studioType = AiStudioType.IMAGE_PROMPT_GEN,
                                viewModel = viewModel,
                                onSpeakText = { voiceEngine.speak(it, speechRate) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_VIDEO_SCRIPT) {
                            AiStudioModuleScreen(
                                studioType = AiStudioType.VIDEO_SCRIPT_GEN,
                                viewModel = viewModel,
                                onSpeakText = { voiceEngine.speak(it, speechRate) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_TRANSLATOR) {
                            AiStudioModuleScreen(
                                studioType = AiStudioType.TRANSLATOR,
                                viewModel = viewModel,
                                onSpeakText = { voiceEngine.speak(it, speechRate) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
