package com.wencui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wencui.quiz.data.AnswerRecord
import com.wencui.quiz.CategoryStat
import com.wencui.quiz.data.DailyChallenge
import com.wencui.quiz.data.PlayQuestion
import com.wencui.quiz.data.QuizQuestion
import com.wencui.quiz.data.QuizRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 主页面路由。 */
enum class Screen {
    Home, Quiz, Result, WrongBook, Favorites
}

/** 会话模式。 */
enum class SessionMode {
    Normal, Memorize
}

/** 全局 UI 状态。 */
data class QuizUiState(
    val screen: Screen = Screen.Home,
    val questions: List<PlayQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val chosenIndex: Int? = null,
    val records: List<AnswerRecord> = emptyList(),
    val sessionCategory: String? = null,
    val sessionType: String = "random",
    val mode: SessionMode = SessionMode.Normal,
    val mistakeIds: Set<Int> = emptySet(),
    val favoriteIds: Set<Int> = emptySet(),
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val dailyDone: Boolean = false,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    // 连击
    val combo: Int = 0,
    val bestCombo: Int = 0,
    // 分类掌握度
    val categoryStats: Map<String, CategoryStat> = emptyMap(),
    // 题库状态
    val bankVersion: Int = 0,
    val bankUpdating: Boolean = false,
    val bankMessage: String? = null
) {
    val currentQuestion: PlayQuestion?
        get() = questions.getOrNull(currentIndex)

    val isLastQuestion: Boolean
        get() = currentIndex >= questions.lastIndex

    val sessionCorrect: Int
        get() = records.count { it.correct }

    val sessionAccuracy: Float
        get() = if (records.isEmpty()) 0f else sessionCorrect.toFloat() / records.size
}

class QuizViewModel(private val repo: QuizRepository) : ViewModel() {

    var uiState by mutableStateOf(QuizUiState())
        private set

    private val prefs = QuizPrefs(repo.appContext)
    private var saved = prefs.load()

    /** 搜索状态（主页搜索框）。 */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        uiState = uiState.copy(
            totalAnswered = saved.totalAnswered,
            totalCorrect = saved.totalCorrect,
            mistakeIds = saved.mistakeIds.toSet(),
            favoriteIds = saved.favoriteIds.toSet(),
            dailyDone = saved.lastDailyKey == DailyChallenge.todayKey(),
            streak = saved.streak,
            bestStreak = saved.bestStreak,
            bestCombo = saved.bestCombo,
            categoryStats = saved.categoryStats,
            bankVersion = repo.bankVersion
        )
    }

    val totalCount: Int get() = repo.totalCount

    /** 分类列表（动态计算，云端题库更新后自动包含新分类）。 */
    fun categories(): List<String> = repo.categoriesNow()

    fun categoryCount(category: String): Int = repo.questionsByCategory(category).size

    /** 分类掌握度：0.0~1.0；未作答返回 -1 表示无数据。 */
    fun categoryAccuracy(category: String): Float {
        val stat = uiState.categoryStats[category] ?: return -1f
        return stat.accuracy
    }

    // region 搜索

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun searchResults(query: String): List<QuizQuestion> = repo.search(query)

    /** 从搜索结果直接开始该题的专项练习（含同分类其余题）。 */
    fun startFromSearch(question: QuizQuestion) {
        val pool = repo.questionsByCategory(question.category)
            .sortedByDescending { it.id == question.id } // 目标题排最前
        beginSession(pool, category = question.category, type = "search", mode = SessionMode.Normal)
    }

    // endregion

    // region 题库云端更新

    fun updateBank() {
        if (uiState.bankUpdating) return
        uiState = uiState.copy(bankUpdating = true, bankMessage = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val result = com.wencui.quiz.update.BankUpdater.checkAndUpdate(
                repo.bankVersion
            ) { version, jsonText ->
                // 解析并校验云端题库 JSON，通过后应用到仓库
                runCatching {
                    val bankJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val obj = bankJson.parseToJsonElement(jsonText).jsonObjectCompat
                    val questions = bankJson.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(QuizQuestion.serializer()),
                        obj["questions"] ?: return@runCatching false
                    )
                    repo.applyCloudBank(version, questions)
                }.getOrDefault(false)
            }
            uiState = when (result) {
                is com.wencui.quiz.update.BankUpdater.Result.Success ->
                    uiState.copy(
                        bankUpdating = false,
                        bankVersion = repo.bankVersion,
                        bankMessage = "题库已更新至 v${result.newVersion}（${result.count} 题）"
                    )
                is com.wencui.quiz.update.BankUpdater.Result.UpToDate ->
                    uiState.copy(bankUpdating = false, bankMessage = "题库已是最新")
                is com.wencui.quiz.update.BankUpdater.Result.Failure ->
                    uiState.copy(bankUpdating = false, bankMessage = "更新失败：${result.message}")
            }
        }
    }

    // endregion

    // region 导航

    fun goHome() {
        uiState = uiState.copy(screen = Screen.Home)
    }

    fun openWrongBook() {
        uiState = uiState.copy(screen = Screen.WrongBook)
    }

    fun openFavorites() {
        uiState = uiState.copy(screen = Screen.Favorites)
    }

    // endregion

    // region 刷题会话

    fun startRandom(count: Int = DEFAULT_SESSION_SIZE, mode: SessionMode = SessionMode.Normal) {
        beginSession(repo.drawRandom(count), category = null, type = "random", mode = mode)
    }

    fun startCategory(category: String, mode: SessionMode = SessionMode.Normal) {
        beginSession(repo.drawRandom(DEFAULT_SESSION_SIZE, category), category = category, type = "category", mode = mode)
    }

    fun startDaily() {
        beginSession(DailyChallenge.todayQuestions(repo.allQuestions), category = null, type = "daily", mode = SessionMode.Normal)
    }

    fun startMistakes(mode: SessionMode = SessionMode.Normal) {
        val wrong = repo.allByIds(uiState.mistakeIds.toList())
        if (wrong.isNotEmpty()) beginSession(wrong.shuffled(), category = null, type = "mistakes", mode = mode)
    }

    fun startFavorites(mode: SessionMode = SessionMode.Normal) {
        val favs = repo.allByIds(uiState.favoriteIds.toList())
        if (favs.isNotEmpty()) beginSession(favs.shuffled(), category = null, type = "favorites", mode = mode)
    }

    private fun beginSession(questions: List<QuizQuestion>, category: String?, type: String, mode: SessionMode) {
        if (questions.isEmpty()) return
        uiState = uiState.copy(
            screen = Screen.Quiz,
            questions = questions.map { it.shuffledForPlay() },
            currentIndex = 0,
            chosenIndex = null,
            records = emptyList(),
            combo = 0,
            sessionCategory = category,
            sessionType = type,
            mode = mode
        )
    }

    /** 选择选项：判分 + 连击 + 掌握度统计。 */
    fun choose(index: Int) {
        val q = uiState.currentQuestion ?: return
        if (uiState.chosenIndex != null) return

        val correct = index == q.answerIndex
        val newCombo = if (correct) uiState.combo + 1 else 0

        // 分类掌握度更新
        val stat = uiState.categoryStats[q.category] ?: CategoryStat()
        val newStat = stat.copy(
            answered = stat.answered + 1,
            correct = stat.correct + if (correct) 1 else 0
        )

        uiState = uiState.copy(
            chosenIndex = index,
            records = uiState.records + AnswerRecord(q.id, index, correct),
            combo = newCombo,
            bestCombo = maxOf(uiState.bestCombo, newCombo),
            totalAnswered = uiState.totalAnswered + 1,
            totalCorrect = uiState.totalCorrect + if (correct) 1 else 0,
            mistakeIds = if (correct) uiState.mistakeIds - q.id else uiState.mistakeIds + q.id,
            categoryStats = uiState.categoryStats + (q.category to newStat)
        )
        saved = saved.copy(
            bestCombo = maxOf(saved.bestCombo, newCombo),
            categoryStats = uiState.categoryStats
        )
        persistProgress()
    }

    fun toggleFavorite() {
        val q = uiState.currentQuestion ?: return
        val newFavs = if (q.id in uiState.favoriteIds) uiState.favoriteIds - q.id else uiState.favoriteIds + q.id
        uiState = uiState.copy(favoriteIds = newFavs)
        saved = saved.copy(favoriteIds = newFavs.toList())
        persistProgress()
    }

    fun next() {
        if (uiState.chosenIndex == null && uiState.mode == SessionMode.Normal) return
        if (uiState.isLastQuestion) {
            if (uiState.sessionType == "daily") settleDaily()
            uiState = uiState.copy(screen = Screen.Result)
        } else {
            uiState = uiState.copy(currentIndex = uiState.currentIndex + 1, chosenIndex = null)
        }
    }

    fun revealAnswer() {
        val q = uiState.currentQuestion ?: return
        if (uiState.chosenIndex == null) {
            uiState = uiState.copy(chosenIndex = q.answerIndex)
        }
    }

    private fun settleDaily() {
        val today = DailyChallenge.todayKey()
        if (saved.lastDailyKey == today) return
        val yesterday = DailyChallenge.yesterdayKey()
        val newStreak = if (saved.lastDailyKey == yesterday) saved.streak + 1 else 1
        saved = saved.copy(
            lastDailyKey = today,
            streak = newStreak,
            bestStreak = maxOf(saved.bestStreak, newStreak)
        )
        uiState = uiState.copy(
            dailyDone = true,
            streak = newStreak,
            bestStreak = saved.bestStreak
        )
        persistProgress()
    }

    private fun persistProgress() {
        saved = saved.copy(
            totalAnswered = uiState.totalAnswered,
            totalCorrect = uiState.totalCorrect,
            mistakeIds = uiState.mistakeIds.toList(),
            favoriteIds = uiState.favoriteIds.toList()
        )
        prefs.save(saved)
    }

    // endregion

    fun mistakeQuestions(): List<QuizQuestion> = repo.allByIds(uiState.mistakeIds.toList())

    fun favoriteQuestions(): List<QuizQuestion> = repo.allByIds(uiState.favoriteIds.toList())

    fun isFavorite(id: Int): Boolean = id in uiState.favoriteIds


/** JsonElement -> JsonObject 的安全扩展。 */
private val kotlinx.serialization.json.JsonElement.jsonObjectCompat: kotlinx.serialization.json.JsonObject
    get() = this as kotlinx.serialization.json.JsonObject

    companion object {
        const val DEFAULT_SESSION_SIZE = 15
        const val DAILY_SESSION_SIZE = 10

        fun factory(repo: QuizRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QuizViewModel(repo) as T
            }
    }
}