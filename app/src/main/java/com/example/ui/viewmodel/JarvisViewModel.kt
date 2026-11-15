package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.JarvisNote
import com.example.data.model.JarvisReminder
import com.example.data.model.JarvisTask
import com.example.data.model.KnowledgeEntry
import com.example.data.model.PlannerEvent
import com.example.data.model.SubscriptionTier
import com.example.data.model.UsageRecord
import com.example.data.model.UserProfile
import com.example.data.remote.LiveWeatherSnapshot
import com.example.data.remote.WeatherService
import com.example.data.repository.AiRepository
import com.example.data.repository.JarvisRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AsyncUiState<out T> {
    data object Idle : AsyncUiState<Nothing>
    data object Loading : AsyncUiState<Nothing>
    data class Streaming(val text: String) : AsyncUiState<Nothing>
    data class Success<T>(val data: T) : AsyncUiState<T>
    data class Error(val message: String) : AsyncUiState<Nothing>
}

class JarvisViewModel(
    private val repository: JarvisRepository,
    private val aiRepository: AiRepository,
    private val currentUserId: String,
    firebaseUser: FirebaseUser?
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e -> Log.w(TAG, "observeUserProfile error", unwrapFirestore(e)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val conversations: StateFlow<List<Conversation>> = repository.observeConversations(currentUserId)
        .catch { e -> Log.w(TAG, "observeConversations error", unwrapFirestore(e)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeMessages(currentUserId, convId)
                    .catch { e ->
                        Log.w(TAG, "observeMessages error", unwrapFirestore(e))
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val tasks: StateFlow<List<JarvisTask>> = repository.observeTasks(currentUserId)
        .catch { e -> Log.w(TAG, "observeTasks error", unwrapFirestore(e)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notes: StateFlow<List<JarvisNote>> = repository.observeNotes(currentUserId)
        .catch { e -> Log.w(TAG, "observeNotes error", unwrapFirestore(e)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reminders: StateFlow<List<JarvisReminder>> = repository.observeReminders(currentUserId)
        .catch { e -> Log.w(TAG, "observeReminders error", unwrapFirestore(e)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val plannerEvents: StateFlow<List<PlannerEvent>> = repository.observePlannerEvents(currentUserId)
        .catch { e -> Log.w(TAG, "observePlannerEvents error", unwrapFirestore(e)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val knowledgeEntries: StateFlow<List<KnowledgeEntry>> = repository.observeKnowledgeEntries(currentUserId)
        .catch { e -> Log.w(TAG, "observeKnowledgeEntries error", unwrapFirestore(e)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val usageHistory: StateFlow<List<UsageRecord>> = repository.observeUsageHistory(currentUserId)
        .catch { e -> Log.w(TAG, "observeUsageHistory error", unwrapFirestore(e)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _chatStreamingText = MutableStateFlow<String?>(null)
    val chatStreamingText: StateFlow<String?> = _chatStreamingText.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _voiceReplyState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val voiceReplyState: StateFlow<AsyncUiState<String>> = _voiceReplyState.asStateFlow()

    private val _studioState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val studioState: StateFlow<AsyncUiState<String>> = _studioState.asStateFlow()

    private val _plannerAiLoading = MutableStateFlow(false)
    val plannerAiLoading: StateFlow<Boolean> = _plannerAiLoading.asStateFlow()

    private val _plannerError = MutableStateFlow<String?>(null)
    val plannerError: StateFlow<String?> = _plannerError.asStateFlow()

    private val _weatherState = MutableStateFlow<AsyncUiState<LiveWeatherSnapshot>>(AsyncUiState.Loading)
    val weatherState: StateFlow<AsyncUiState<LiveWeatherSnapshot>> = _weatherState.asStateFlow()

    private val _adminUsersState = MutableStateFlow<AsyncUiState<List<UserProfile>>>(AsyncUiState.Idle)
    val adminUsersState: StateFlow<AsyncUiState<List<UserProfile>>> = _adminUsersState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        if (firebaseUser != null) {
            viewModelScope.launch {
                repository.ensureUserProfile(firebaseUser)
            }
        }
        viewModelScope.launch {
            conversations.collect { list ->
                if (_activeConversationId.value == null && list.isNotEmpty()) {
                    _activeConversationId.value = list.first().id
                }
            }
        }
        loadWeather("San Francisco")
    }

    private fun unwrapFirestore(error: Throwable): Throwable {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull() ?: error
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearChatError() {
        _chatError.value = null
    }

    fun resetStudioState() {
        _studioState.value = AsyncUiState.Idle
    }

    fun selectConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        _chatError.value = null
    }

    fun createNewConversation(title: String = "New Neural Session") {
        viewModelScope.launch {
            repository.createConversation(currentUserId, title).onSuccess { newId ->
                _activeConversationId.value = newId
                _chatError.value = null
            }.onFailure { err ->
                _chatError.value = err.localizedMessage ?: "Failed to create conversation."
            }
        }
    }

    private fun remainingDailyQuota(): Int {
        val profile = userProfile.value ?: return SubscriptionTier.FREE.dailyLimit
        val limit = profile.tier.dailyLimit
        val usedToday = if (profile.dailyDateKey == repository.currentDateKey()) {
            profile.dailyRequestsUsed.toInt()
        } else {
            0
        }
        return (limit - usedToday).coerceAtLeast(0)
    }

    fun sendChatMessage(promptText: String, onResponseReady: ((String) -> Unit)? = null) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank() || _isChatGenerating.value) return

        if (remainingDailyQuota() <= 0) {
            _chatError.value = "Daily quota reached for ${userProfile.value?.tier?.title ?: "Free Core"}. Upgrade plan in Account to continue."
            return
        }

        viewModelScope.launch {
            _chatError.value = null
            _isChatGenerating.value = true
            _chatStreamingText.value = ""

            try {
                val convId = _activeConversationId.value ?: run {
                    val created = repository.createConversation(
                        currentUserId,
                        trimmed.take(40)
                    ).getOrThrow()
                    _activeConversationId.value = created
                    created
                }

                val isFirstMessage = messages.value.isEmpty()
                val historyPairs = messages.value.map { it.role to it.text }

                repository.addMessageToConversation(
                    userId = currentUserId,
                    conversationId = convId,
                    role = "user",
                    text = trimmed,
                    module = "chat",
                    updateTitleIfFirst = if (isFirstMessage) trimmed.take(48) else null
                ).getOrThrow()

                val finalReply = StringBuilder()
                aiRepository.streamChat(historyPairs, trimmed).collect { accumulated ->
                    finalReply.clear()
                    finalReply.append(accumulated)
                    _chatStreamingText.value = accumulated
                }

                val replyText = finalReply.toString().trim()
                if (replyText.isNotEmpty()) {
                    repository.addMessageToConversation(
                        userId = currentUserId,
                        conversationId = convId,
                        role = "model",
                        text = replyText,
                        module = "chat"
                    )
                    repository.recordAiUsage(
                        userId = currentUserId,
                        profile = userProfile.value,
                        module = "chat",
                        promptChars = trimmed.length,
                        responseChars = replyText.length
                    )
                    onResponseReady?.invoke(replyText)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat error", e)
                _chatError.value = e.localizedMessage ?: "Failed to reach JARVIS neural core."
            } finally {
                _chatStreamingText.value = null
                _isChatGenerating.value = false
            }
        }
    }

    fun regenerateLastResponse(onResponseReady: ((String) -> Unit)? = null) {
        val currentMsgs = messages.value
        val convId = _activeConversationId.value ?: return
        val lastUserMsg = currentMsgs.lastOrNull { it.role == "user" } ?: return
        val lastMsg = currentMsgs.lastOrNull()

        viewModelScope.launch {
            if (lastMsg != null && lastMsg.role == "model") {
                repository.deleteMessage(currentUserId, convId, lastMsg.id)
            }
            _chatError.value = null
            _isChatGenerating.value = true
            _chatStreamingText.value = ""
            try {
                val historyWithoutLast = currentMsgs
                    .dropLast(if (lastMsg?.role == "model") 2 else 1)
                    .map { it.role to it.text }
                val finalReply = StringBuilder()
                aiRepository.streamChat(historyWithoutLast, lastUserMsg.text).collect { accumulated ->
                    finalReply.clear()
                    finalReply.append(accumulated)
                    _chatStreamingText.value = accumulated
                }
                val replyText = finalReply.toString().trim()
                if (replyText.isNotEmpty()) {
                    repository.addMessageToConversation(
                        userId = currentUserId,
                        conversationId = convId,
                        role = "model",
                        text = replyText,
                        module = "chat"
                    )
                    repository.recordAiUsage(
                        userId = currentUserId,
                        profile = userProfile.value,
                        module = "chat_regen",
                        promptChars = lastUserMsg.text.length,
                        responseChars = replyText.length
                    )
                    onResponseReady?.invoke(replyText)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _chatError.value = e.localizedMessage ?: "Regeneration failed."
            } finally {
                _chatStreamingText.value = null
                _isChatGenerating.value = false
            }
        }
    }

    fun clearActiveConversation() {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.clearConversation(currentUserId, convId)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(currentUserId, conversationId)
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
        }
    }

    fun processVoiceCommand(spokenText: String, onSpeakBack: (String) -> Unit) {
        val clean = spokenText.trim()
        if (clean.isBlank()) return
        if (remainingDailyQuota() <= 0) {
            _voiceReplyState.value = AsyncUiState.Error("Daily quota reached. Upgrade your plan in Account.")
            return
        }

        viewModelScope.launch {
            _voiceReplyState.value = AsyncUiState.Loading
            try {
                val pendingTasks = tasks.value.count { !it.completed }
                val prompt = """
                    The user spoke this voice command to JARVIS: "$clean"
                    Context: The user currently has $pendingTasks pending tasks and ${reminders.value.count { !it.completed }} active reminders.
                    Respond concisely in 2-4 crisp spoken sentences suitable for Text-to-Speech (no heavy markdown tables).
                """.trimIndent()
                val reply = aiRepository.generateText(prompt)
                _voiceReplyState.value = AsyncUiState.Success(reply)
                repository.recordAiUsage(
                    userId = currentUserId,
                    profile = userProfile.value,
                    module = "voice",
                    promptChars = clean.length,
                    responseChars = reply.length
                )
                onSpeakBack(reply)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _voiceReplyState.value = AsyncUiState.Error(e.localizedMessage ?: "Voice AI request failed.")
            }
        }
    }

    fun runStudioPrompt(moduleName: String, prompt: String) {
        if (prompt.isBlank()) return
        if (remainingDailyQuota() <= 0) {
            _studioState.value = AsyncUiState.Error("Daily quota reached for your current plan.")
            return
        }

        viewModelScope.launch {
            _studioState.value = AsyncUiState.Loading
            val accumulated = StringBuilder()
            try {
                aiRepository.streamModulePrompt(prompt).collect { text ->
                    accumulated.clear()
                    accumulated.append(text)
                    _studioState.value = AsyncUiState.Streaming(text)
                }
                val result = accumulated.toString().trim()
                if (result.isNotEmpty()) {
                    _studioState.value = AsyncUiState.Success(result)
                    repository.recordAiUsage(
                        userId = currentUserId,
                        profile = userProfile.value,
                        module = moduleName,
                        promptChars = prompt.length,
                        responseChars = result.length
                    )
                } else {
                    _studioState.value = AsyncUiState.Error("Empty response received.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _studioState.value = AsyncUiState.Error(e.localizedMessage ?: "AI generation failed.")
            }
        }
    }

    fun generateAiDailyPlan(focusGoals: String) {
        if (focusGoals.isBlank() || _plannerAiLoading.value) return
        if (remainingDailyQuota() <= 0) {
            _plannerError.value = "Daily quota reached for your current plan."
            return
        }
        viewModelScope.launch {
            _plannerError.value = null
            _plannerAiLoading.value = true
            try {
                val blocks = aiRepository.generateDailyPlan(focusGoals)
                val today = repository.currentDateKey()
                blocks.forEach { block ->
                    repository.addPlannerEvent(
                        userId = currentUserId,
                        title = block.title,
                        timeSlot = block.timeSlot,
                        durationMinutes = block.durationMinutes,
                        category = block.category,
                        dateKey = today
                    )
                }
                repository.recordAiUsage(
                    userId = currentUserId,
                    profile = userProfile.value,
                    module = "planner",
                    promptChars = focusGoals.length,
                    responseChars = blocks.sumOf { it.title.length }
                )
                _statusBanner.value = "Generated ${blocks.size} AI schedule blocks."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _plannerError.value = e.localizedMessage ?: "Failed to generate AI schedule."
            } finally {
                _plannerAiLoading.value = false
            }
        }
    }

    fun askKnowledgeCenter(question: String) {
        val entries = knowledgeEntries.value
        val contextBlock = if (entries.isEmpty()) {
            "No personal knowledge documents saved yet."
        } else {
            entries.take(15).joinToString("\n\n") { entry ->
                "Title: ${entry.title} [${entry.category}]\nSummary: ${entry.summary}\nContent: ${entry.content.take(800)}"
            }
        }
        val prompt = """
            Answer the user's question primarily using their Personal Knowledge Base below.
            If the answer is not covered in their saved entries, clearly say so and then provide general guidance.

            === PERSONAL KNOWLEDGE BASE ===
            $contextBlock

            === QUESTION ===
            $question
        """.trimIndent()
        runStudioPrompt("knowledge_qa", prompt)
    }

    // --- Productivity CRUD ---

    fun addTask(title: String, description: String, priority: String, dueDate: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTask(currentUserId, title.trim(), description.trim(), priority, dueDate.ifBlank { "Today" })
        }
    }

    fun toggleTask(task: JarvisTask) {
        viewModelScope.launch { repository.toggleTask(currentUserId, task) }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { repository.deleteTask(currentUserId, taskId) }
    }

    fun addNote(title: String, content: String, tag: String) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.addNote(
                currentUserId,
                title.trim().ifBlank { "Quick Note" },
                content.trim(),
                tag.trim().ifBlank { "General" }
            )
            _statusBanner.value = "Saved to Notes."
        }
    }

    fun togglePinNote(note: JarvisNote) {
        viewModelScope.launch { repository.togglePinNote(currentUserId, note) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repository.deleteNote(currentUserId, noteId) }
    }

    fun addReminder(title: String, timeLabel: String, category: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addReminder(
                currentUserId,
                title.trim(),
                timeLabel.trim().ifBlank { "Today 5:00 PM" },
                category.trim().ifBlank { "Personal" }
            )
        }
    }

    fun toggleReminder(reminder: JarvisReminder) {
        viewModelScope.launch { repository.toggleReminder(currentUserId, reminder) }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch { repository.deleteReminder(currentUserId, reminderId) }
    }

    fun addPlannerEvent(title: String, timeSlot: String, durationMinutes: Long, category: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addPlannerEvent(
                userId = currentUserId,
                title = title.trim(),
                timeSlot = timeSlot.trim().ifBlank { "09:00 AM" },
                durationMinutes = durationMinutes,
                category = category.trim().ifBlank { "Focus" }
            )
        }
    }

    fun togglePlannerEvent(event: PlannerEvent) {
        viewModelScope.launch { repository.togglePlannerEvent(currentUserId, event) }
    }

    fun deletePlannerEvent(eventId: String) {
        viewModelScope.launch { repository.deletePlannerEvent(currentUserId, eventId) }
    }

    fun addKnowledgeEntry(title: String, summary: String, content: String, category: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            repository.addKnowledgeEntry(
                currentUserId,
                title.trim(),
                summary.trim().ifBlank { content.trim().take(120) },
                content.trim(),
                category.trim().ifBlank { "Reference" }
            )
        }
    }

    fun deleteKnowledgeEntry(entryId: String) {
        viewModelScope.launch { repository.deleteKnowledgeEntry(currentUserId, entryId) }
    }

    // --- Weather ---

    fun loadWeather(city: String) {
        viewModelScope.launch {
            _weatherState.value = AsyncUiState.Loading
            try {
                val snapshot = WeatherService.fetchWeatherForCity(city)
                _weatherState.value = AsyncUiState.Success(snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _weatherState.value = AsyncUiState.Error(e.localizedMessage ?: "Failed to load live weather.")
            }
        }
    }

    // --- Settings, Subscription & Admin ---

    fun updatePlan(planId: String) {
        viewModelScope.launch {
            repository.updateUserPlan(currentUserId, planId)
                .onSuccess { _statusBanner.value = "Switched to ${SubscriptionTier.fromId(planId).title}" }
        }
    }

    fun updatePreferences(voiceEnabled: Boolean, speechRate: Double, preferredLanguage: String) {
        viewModelScope.launch {
            repository.updateUserPreferences(currentUserId, voiceEnabled, speechRate, preferredLanguage)
                .onSuccess { _statusBanner.value = "Preferences synced." }
        }
    }

    fun loadAdminUsers() {
        viewModelScope.launch {
            _adminUsersState.value = AsyncUiState.Loading
            repository.adminFetchAllUsers()
                .onSuccess { users -> _adminUsersState.value = AsyncUiState.Success(users) }
                .onFailure { err -> _adminUsersState.value = AsyncUiState.Error(err.localizedMessage ?: "Admin authorization required.") }
        }
    }

    fun adminSetUserPlan(targetUserId: String, planId: String) {
        viewModelScope.launch {
            repository.updateUserPlan(targetUserId, planId)
                .onSuccess { loadAdminUsers() }
        }
    }

    private companion object {
        const val TAG = "JarvisVM"
    }
}
