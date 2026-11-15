package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedPlanBlock(
    val title: String,
    val timeSlot: String,
    val durationMinutes: Long,
    val category: String
)

class AiRepository {

    private val defaultModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are JARVIS, a brilliant, concise, futuristic personal AI assistant. " +
                        "Provide accurate, structured, actionable answers with clean Markdown formatting " +
                        "(headings, bold highlights, bullet points, or code blocks when helpful). " +
                        "Maintain a calm, sharp, helpful tone."
                )
            }
        )
    }

    private val dailyPlannerSchema = Schema.obj(
        properties = mapOf(
            "blocks" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Short actionable block title"),
                        "timeSlot" to Schema.string(description = "Time label like 09:00 AM - 10:30 AM"),
                        "durationMinutes" to Schema.integer(description = "Duration in minutes (15 to 240)"),
                        "category" to Schema.string(description = "Category like Deep Work, Health, Admin, Strategy")
                    )
                ),
                description = "4 to 6 ordered schedule blocks for the day"
            )
        )
    )

    private val plannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dailyPlannerSchema
            }
        )
    }

    fun streamChat(
        history: List<Pair<String, String>>,
        prompt: String
    ): Flow<String> = flow {
        val chatContents = history.takeLast(14).map { (role, msg) ->
            content(if (role == "model") "model" else "user") { text(msg) }
        }
        val chat = defaultModel.startChat(history = chatContents)
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("JARVIS returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Streaming chat timed out", e)
            throw IllegalStateException("Neural link timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming chat failed", e)
            throw e
        }
    }

    fun streamModulePrompt(prompt: String): Flow<String> = flow {
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                defaultModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("JARVIS returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Module stream timed out", e)
            throw IllegalStateException("Request timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Module stream failed", e)
            throw e
        }
    }

    suspend fun generateText(prompt: String): String {
        return try {
            val response = withTimeout(60_000L) {
                defaultModel.generateContent(prompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("JARVIS returned an empty response.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Unary generation timed out", e)
            throw IllegalStateException("Request timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unary generation failed", e)
            throw e
        }
    }

    suspend fun generateDailyPlan(goalsPrompt: String): List<GeneratedPlanBlock> {
        return try {
            val response = withTimeout(60_000L) {
                plannerModel.generateContent(
                    "Build a realistic, productive, balanced daily time-blocked schedule based on: $goalsPrompt"
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty schedule response.")
            val root = JSONObject(rawJson)
            val blocksArray = root.getJSONArray("blocks")
            (0 until blocksArray.length()).map { idx ->
                val obj = blocksArray.getJSONObject(idx)
                GeneratedPlanBlock(
                    title = obj.optString("title", "Focus Block").take(150),
                    timeSlot = obj.optString("timeSlot", "09:00 AM").take(38),
                    durationMinutes = obj.optLong("durationMinutes", 60L).coerceIn(15L, 480L),
                    category = obj.optString("category", "Deep Work").take(38)
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Planner generation timed out", e)
            throw IllegalStateException("Planner generation timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Planner generation failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "AiRepository"
    }
}
