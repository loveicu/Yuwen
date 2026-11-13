package com.wencui.quiz

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 持久化的学习数据：累计答题数、正确数、错题 ID 列表。 */
@Serializable
data class SavedProgress(
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val mistakeIds: List<Int> = emptyList()
)

/** 基于 SharedPreferences + JSON 的轻量持久化。 */
class QuizPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("wencui_progress", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): SavedProgress {
        val text = prefs.getString(KEY_PROGRESS, null) ?: return SavedProgress()
        return runCatching { json.decodeFromString(SavedProgress.serializer(), text) }
            .getOrDefault(SavedProgress())
    }

    fun save(totalAnswered: Int, totalCorrect: Int, mistakeIds: Set<Int>) {
        val data = SavedProgress(
            totalAnswered = totalAnswered,
            totalCorrect = totalCorrect,
            mistakeIds = mistakeIds.toList()
        )
        prefs.edit().putString(KEY_PROGRESS, json.encodeToString(data)).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_PROGRESS = "progress_json"
    }
}