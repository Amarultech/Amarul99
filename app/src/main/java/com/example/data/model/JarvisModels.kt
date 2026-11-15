package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SubscriptionTier(
    val id: String,
    val title: String,
    val priceLabel: String,
    val dailyLimit: Int,
    val tagline: String,
    val features: List<String>
) {
    FREE(
        id = "free",
        title = "Free Core",
        priceLabel = "$0 / mo",
        dailyLimit = 25,
        tagline = "Essential personal companion",
        features = listOf(
            "25 AI neural requests / day",
            "Voice HUD & Text-to-Speech",
            "Tasks, Notes, Reminders & Planner",
            "Live Weather & Scientific Calculator"
        )
    ),
    PRO(
        id = "pro",
        title = "JARVIS Pro",
        priceLabel = "$19 / mo",
        dailyLimit = 250,
        tagline = "Creator & productivity accelerator",
        features = listOf(
            "250 AI neural requests / day",
            "Streaming Creator & Script Studios",
            "AI Daily Auto-Scheduler",
            "Personal Knowledge Q&A"
        )
    ),
    PREMIUM(
        id = "premium",
        title = "JARVIS Enterprise",
        priceLabel = "$49 / mo",
        dailyLimit = 10000,
        tagline = "Unrestricted autonomous companion",
        features = listOf(
            "10,000 AI neural requests / day",
            "Priority Gemini 3.8 Flash streaming",
            "Full Knowledge Center & Multi-thread sync",
            "SaaS Telemetry & Admin Suite"
        )
    );

    companion object {
        fun fromId(id: String): SubscriptionTier =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FREE
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val role: String = "user",
    val plan: String = "free",
    val dailyRequestsUsed: Long = 0L,
    val dailyDateKey: String = "",
    val totalRequests: Long = 0L,
    val voiceEnabled: Boolean = true,
    val speechRate: Double = 1.0,
    val preferredLanguage: String = "English",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val tier: SubscriptionTier get() = SubscriptionTier.fromId(plan)
    val isAdminUser: Boolean get() = role == "admin" || email.equals("hoqueamarul848@gmail.com", ignoreCase = true)
}

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Neural Session",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val role: String = "user",
    val text: String = "",
    val module: String = "chat",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JarvisTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val priority: String = "medium",
    val completed: Boolean = false,
    val dueDateLabel: String = "Today",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JarvisNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val tag: String = "General",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JarvisReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val timeLabel: String = "",
    val category: String = "Personal",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlannerEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val timeSlot: String = "",
    val durationMinutes: Long = 60L,
    val category: String = "Deep Work",
    val dateKey: String = "",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class KnowledgeEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val category: String = "Reference",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UsageRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val module: String = "chat",
    val promptChars: Long = 0L,
    val responseChars: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
