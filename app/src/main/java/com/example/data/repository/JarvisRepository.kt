package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.JarvisNote
import com.example.data.model.JarvisReminder
import com.example.data.model.JarvisTask
import com.example.data.model.KnowledgeEntry
import com.example.data.model.PlannerEvent
import com.example.data.model.UsageRecord
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun currentDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    private fun <T> Flow<T>.handleSnapshotErrors(
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw (firestoreError ?: error)
    }

    // --- User Profile & SaaS Plan ---

    suspend fun ensureUserProfile(user: FirebaseUser): Result<UserProfile> {
        val docRef = userDoc(user.uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(userId = user.uid)
                Result.success(existing)
            } else {
                val cleanName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Commander").take(100)
                val cleanEmail = (user.email?.takeIf { it.isNotBlank() } ?: "user@jarvis.ai").take(150)
                val initialRole = if (cleanEmail.equals("hoqueamarul848@gmail.com", ignoreCase = true) && user.isEmailVerified) {
                    "admin"
                } else {
                    "user"
                }
                val payload = mutableMapOf<String, Any>(
                    "userId" to user.uid,
                    "displayName" to cleanName,
                    "email" to cleanEmail,
                    "role" to initialRole,
                    "plan" to "free",
                    "dailyRequestsUsed" to 0L,
                    "dailyDateKey" to currentDateKey(),
                    "totalRequests" to 0L,
                    "voiceEnabled" to true,
                    "speechRate" to 1.0,
                    "preferredLanguage" to "English",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                user.photoUrl?.toString()?.take(500)?.takeIf { it.isNotBlank() }?.let {
                    payload["photoUrl"] = it
                }
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = user.uid,
                    userId = user.uid,
                    displayName = cleanName,
                    email = cleanEmail,
                    role = initialRole
                )
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = userDoc(userId)
        return docRef.snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .handleSnapshotErrors(OperationType.GET, docRef.path)
    }

    suspend fun updateUserPlan(userId: String, planId: String): Result<Unit> {
        val docRef = userDoc(userId)
        return try {
            docRef.update(
                mapOf(
                    "plan" to planId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        userId: String,
        voiceEnabled: Boolean,
        speechRate: Double,
        preferredLanguage: String
    ): Result<Unit> {
        val docRef = userDoc(userId)
        return try {
            docRef.update(
                mapOf(
                    "voiceEnabled" to voiceEnabled,
                    "speechRate" to speechRate.coerceIn(0.5, 2.5),
                    "preferredLanguage" to preferredLanguage.take(40).ifBlank { "English" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordAiUsage(
        userId: String,
        profile: UserProfile?,
        module: String,
        promptChars: Int,
        responseChars: Int
    ): Result<Unit> {
        val docRef = userDoc(userId)
        val usageRef = docRef.collection("usage").document()
        val today = currentDateKey()
        return try {
            val nextDaily = if (profile?.dailyDateKey == today) {
                (profile.dailyRequestsUsed + 1L)
            } else {
                1L
            }
            val nextTotal = (profile?.totalRequests ?: 0L) + 1L
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "dailyRequestsUsed" to nextDaily,
                    "dailyDateKey" to today,
                    "totalRequests" to nextTotal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                usageRef,
                mapOf(
                    "userId" to userId,
                    "module" to module.take(40).ifBlank { "chat" },
                    "promptChars" to promptChars.coerceAtLeast(0),
                    "responseChars" to responseChars.coerceAtLeast(0),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, usageRef.path)
            Result.failure(e)
        }
    }

    fun observeUsageHistory(userId: String): Flow<List<UsageRecord>> {
        val colRef = userDoc(userId).collection("usage")
        return colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    UsageRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleSnapshotErrors(OperationType.LIST, colRef.path)
    }

    // --- Admin Queries ---

    suspend fun adminFetchAllUsers(): Result<List<UserProfile>> {
        return try {
            val snap = db.collection("users").get().await()
            val users = snap.toObjects(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(users)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users")
            Result.failure(e)
        }
    }

    // --- Conversations & Messages ---

    fun observeConversations(userId: String): Flow<List<Conversation>> {
        val colRef = userDoc(userId).collection("conversations")
        return colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Conversation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleSnapshotErrors(OperationType.LIST, colRef.path)
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> {
        val colRef = userDoc(userId).collection("conversations")
            .document(conversationId).collection("messages")
        return colRef.orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleSnapshotErrors(OperationType.LIST, colRef.path)
    }

    suspend fun createConversation(userId: String, title: String = "Neural Session"): Result<String> {
        val colRef = userDoc(userId).collection("conversations")
        val docRef = colRef.document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to title.take(120).ifBlank { "Neural Session" },
                    "lastMessagePreview" to "Ready for commands",
                    "messageCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addMessageToConversation(
        userId: String,
        conversationId: String,
        role: String,
        text: String,
        module: String = "chat",
        updateTitleIfFirst: String? = null
    ): Result<String> {
        val convRef = userDoc(userId).collection("conversations").document(conversationId)
        val msgRef = convRef.collection("messages").document()
        return try {
            val cleanText = text.take(30000).ifBlank { " " }
            val preview = cleanText.replace("\n", " ").take(120)
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "userId" to userId,
                    "conversationId" to conversationId,
                    "role" to role,
                    "text" to cleanText,
                    "module" to module.take(40).ifBlank { "chat" },
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            val convUpdate = mutableMapOf<String, Any>(
                "lastMessagePreview" to preview,
                "messageCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!updateTitleIfFirst.isNullOrBlank()) {
                convUpdate["title"] = updateTitleIfFirst.take(120)
            }
            batch.update(convRef, convUpdate)
            batch.commit().await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(userId: String, conversationId: String, messageId: String): Result<Unit> {
        val msgRef = userDoc(userId).collection("conversations")
            .document(conversationId).collection("messages").document(messageId)
        return try {
            msgRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearConversation(userId: String, conversationId: String): Result<Unit> {
        val convRef = userDoc(userId).collection("conversations").document(conversationId)
        return try {
            val messages = convRef.collection("messages").get().await()
            val batch = db.batch()
            messages.documents.forEach { batch.delete(it.reference) }
            batch.update(
                convRef,
                mapOf(
                    "lastMessagePreview" to "Session cleared",
                    "messageCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, convRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(userId: String, conversationId: String): Result<Unit> {
        val convRef = userDoc(userId).collection("conversations").document(conversationId)
        return try {
            val messages = convRef.collection("messages").get().await()
            val batch = db.batch()
            messages.documents.forEach { batch.delete(it.reference) }
            batch.delete(convRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, convRef.path)
            Result.failure(e)
        }
    }

    // --- Tasks ---

    fun observeTasks(userId: String): Flow<List<JarvisTask>> {
        val colRef = userDoc(userId).collection("tasks")
        return colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    JarvisTask::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleSnapshotErrors(OperationType.LIST, colRef.path)
    }

    suspend fun addTask(
        userId: String,
        title: String,
        description: String = "",
        priority: String = "medium",
        dueDateLabel: String = "Today"
    ): Result<String> {
        val docRef = userDoc(userId).collection("tasks").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to title.take(160).ifBlank { "Untitled Task" },
                    "description" to description.take(1000),
                    "priority" to if (priority in listOf("low", "medium", "high")) priority else "medium",
                    "completed" to false,
                    "dueDateLabel" to dueDateLabel.take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleTask(userId: String, task: JarvisTask): Result<Unit> {
        val docRef = userDoc(userId).collection("tasks").document(task.id)
        return try {
            docRef.update(
                mapOf(
                    "completed" to !task.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("tasks").document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Notes ---

    fun observeNotes(userId: String): Flow<List<JarvisNote>> {
        val colRef = userDoc(userId).collection("notes")
        return colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    JarvisNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleSnapshotErrors(OperationType.LIST, colRef.path)
    }

    suspend fun addNote(
        userId: String,
        title: String,
        content: String,
        tag: String = "General"
    ): Result<String> {
        val docRef = userDoc(userId).collection("notes").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to title.take(160).ifBlank { "Quick Note" },
                    "content" to content.take(15000),
                    "tag" to tag.take(40).ifBlank { "General" },
                    "pinned" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePinNote(userId: String, note: JarvisNote): Result<Unit> {
        val docRef = userDoc(userId).collection("notes").document(note.id)
        return try {
            docRef.update(
                mapOf(
                    "pinned" to !note.pinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("notes").document(noteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Reminders ---

    fun observeReminders(userId: String): Flow<List<JarvisReminder>> {
        val colRef = userDoc(userId).collection("reminders")
        return colRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    JarvisReminder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleSnapshotErrors(OperationType.LIST, colRef.path)
    }

    suspend fun addReminder(
        userId: String,
        title: String,
        timeLabel: String,
        category: String = "Personal"
    ): Result<String> {
        val docRef = userDoc(userId).collection("reminders").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to title.take(160).ifBlank { "Reminder" },
                    "timeLabel" to timeLabel.take(60).ifBlank { "Today 5:00 PM" },
                    "category" to category.take(40).ifBlank { "Personal" },
                    "completed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleReminder(userId: String, reminder: JarvisReminder): Result<Unit> {
        val docRef = userDoc(userId).collection("reminders").document(reminder.id)
        return try {
            docRef.update(
                mapOf(
                    "completed" to !reminder.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(userId: String, reminderId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("reminders").document(reminderId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Daily Planner ---

    fun observePlannerEvents(userId: String): Flow<List<PlannerEvent>> {
        val colRef = userDoc(userId).collection("planner")
        return colRef.orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    PlannerEvent::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleSnapshotErrors(OperationType.LIST, colRef.path)
    }

    suspend fun addPlannerEvent(
        userId: String,
        title: String,
        timeSlot: String,
        durationMinutes: Long = 60L,
        category: String = "Deep Work",
        dateKey: String = currentDateKey()
    ): Result<String> {
        val docRef = userDoc(userId).collection("planner").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to title.take(160).ifBlank { "Focus Block" },
                    "timeSlot" to timeSlot.take(40).ifBlank { "09:00 AM" },
                    "durationMinutes" to durationMinutes.coerceIn(5L, 1440L),
                    "category" to category.take(40).ifBlank { "Deep Work" },
                    "dateKey" to dateKey.take(20),
                    "completed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePlannerEvent(userId: String, event: PlannerEvent): Result<Unit> {
        val docRef = userDoc(userId).collection("planner").document(event.id)
        return try {
            docRef.update(
                mapOf(
                    "completed" to !event.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePlannerEvent(userId: String, eventId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("planner").document(eventId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Personal Knowledge Center ---

    fun observeKnowledgeEntries(userId: String): Flow<List<KnowledgeEntry>> {
        val colRef = userDoc(userId).collection("knowledge")
        return colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    KnowledgeEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .handleSnapshotErrors(OperationType.LIST, colRef.path)
    }

    suspend fun addKnowledgeEntry(
        userId: String,
        title: String,
        summary: String,
        content: String,
        category: String = "Reference"
    ): Result<String> {
        val docRef = userDoc(userId).collection("knowledge").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to title.take(160).ifBlank { "Knowledge Doc" },
                    "summary" to summary.take(500),
                    "content" to content.take(15000).ifBlank { title },
                    "category" to category.take(50).ifBlank { "Reference" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteKnowledgeEntry(userId: String, entryId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("knowledge").document(entryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
