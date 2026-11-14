package com.wencui.quiz

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 单个分类的掌握度统计。 */
@Serializable
data class CategoryStat(
    val answered: Int = 0,
    val correct: Int = 0
) {
    val accuracy: Float get() = if (answered == 0) 0f else correct.toFloat() / answered
}

/** 持久化的学习数据。 */
@Serializable
data class SavedProgress(
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val mistakeIds: List<Int> = emptyList(),
    val favoriteIds: List<Int> = emptyList(),
    // 每日一练
    val lastDailyKey: String = "",
    val streak: Int = 0,
    val bestStreak: Int = 0,
    // 分类掌握度：category -> stat
    val categoryStats: Map<String, CategoryStat> = emptyMap(),
    // 历史最佳连击
    val bestCombo: Int = 0,
    // 题库云端版本（0 = 未更新过，用内置题库）
    val bankVersion: Int = 0
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

    fun save(data: SavedProgress) {
        prefs.edit().putString(KEY_PROGRESS, json.encodeToString(data)).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_PROGRESS = "progress_json"
    }
}