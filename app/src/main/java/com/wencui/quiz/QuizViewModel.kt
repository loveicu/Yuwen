package com.wencui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wencui.quiz.data.AnswerRecord
import com.wencui.quiz.data.DailyChallenge
import com.wencui.quiz.data.PlayQuestion
import com.wencui.quiz.data.QuizQuestion
import com.wencui.quiz.data.QuizRepository

/** 主页面路由。 */
enum class Screen {
    Home, Quiz, Result, WrongBook, Favorites
}

/** 会话模式：normal 正常刷题 / memorize 背题模式（即时看解释，可反复看） */
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
    // 累计统计（持久化）
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    // 每日一练
    val dailyDone: Boolean = false,
    val streak: Int = 0,
    val bestStreak: Int = 0
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

    init {
        uiState = uiState.copy(
            totalAnswered = saved.totalAnswered,
            totalCorrect = saved.totalCorrect,
            mistakeIds = saved.mistakeIds.toSet(),
            favoriteIds = saved.favoriteIds.toSet(),
            dailyDone = saved.lastDailyKey == DailyChallenge.todayKey(),
            streak = saved.streak,
            bestStreak = saved.bestStreak
        )
    }

    val categories: List<String> get() = repo.categories
    val totalCount: Int get() = repo.totalCount

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

    /** 开始一轮随机练习。 */
    fun startRandom(count: Int = DEFAULT_SESSION_SIZE, mode: SessionMode = SessionMode.Normal) {
        beginSession(repo.drawRandom(count), category = null, type = "random", mode = mode)
    }

    /** 开始某一分类的练习。 */
    fun startCategory(category: String, mode: SessionMode = SessionMode.Normal) {
        beginSession(repo.drawRandom(DEFAULT_SESSION_SIZE, category), category = category, type = "category", mode = mode)
    }

    /** 每日一练：今天固定 10 题；会话结束时计入打卡。 */
    fun startDaily() {
        beginSession(DailyChallenge.todayQuestions(repo.allQuestions), category = null, type = "daily", mode = SessionMode.Normal)
    }

    /** 重练错题本。 */
    fun startMistakes(mode: SessionMode = SessionMode.Normal) {
        val wrong = repo.allByIds(uiState.mistakeIds.toList())
        if (wrong.isNotEmpty()) beginSession(wrong.shuffled(), category = null, type = "mistakes", mode = mode)
    }

    /** 重练收藏夹。 */
    fun startFavorites(mode: SessionMode = SessionMode.Normal) {
        val favs = repo.allByIds(uiState.favoriteIds.toList())
        if (favs.isNotEmpty()) beginSession(favs.shuffled(), category = null, type = "favorites", mode = mode)
    }

    private fun beginSession(questions: List<QuizQuestion>, category: String?, type: String, mode: SessionMode) {
        if (questions.isEmpty()) return
        // 每题生成独立的乱序作答视图
        val playQuestions = questions.map { it.shuffledForPlay() }
        uiState = uiState.copy(
            screen = Screen.Quiz,
            questions = playQuestions,
            currentIndex = 0,
            chosenIndex = null,
            records = emptyList(),
            sessionCategory = category,
            sessionType = type,
            mode = mode
        )
    }

    /** 选择一个选项。首次选择时记分；答错进错题本。 */
    fun choose(index: Int) {
        val q = uiState.currentQuestion ?: return
        if (uiState.chosenIndex != null) return // 已作答，防重复

        val correct = index == q.answerIndex
        uiState = uiState.copy(
            chosenIndex = index,
            records = uiState.records + AnswerRecord(q.id, index, correct),
            totalAnswered = uiState.totalAnswered + 1,
            totalCorrect = uiState.totalCorrect + if (correct) 1 else 0,
            mistakeIds = if (correct) uiState.mistakeIds - q.id else uiState.mistakeIds + q.id
        )
        persistProgress()
    }

    /** 切换当前题收藏状态。 */
    fun toggleFavorite() {
        val q = uiState.currentQuestion ?: return
        val newFavs = if (q.id in uiState.favoriteIds) uiState.favoriteIds - q.id else uiState.favoriteIds + q.id
        uiState = uiState.copy(favoriteIds = newFavs)
        persistProgress()
    }

    /** 下一题；最后一题则进入结算页（每日一练此时结算打卡）。 */
    fun next() {
        if (uiState.chosenIndex == null && uiState.mode == SessionMode.Normal) return
        if (uiState.isLastQuestion) {
            if (uiState.sessionType == "daily") settleDaily()
            uiState = uiState.copy(screen = Screen.Result)
        } else {
            uiState = uiState.copy(currentIndex = uiState.currentIndex + 1, chosenIndex = null)
        }
    }

    /** 背题模式：直接查看答案与全部解释，不判分。 */
    fun revealAnswer() {
        val q = uiState.currentQuestion ?: return
        if (uiState.chosenIndex == null) {
            uiState = uiState.copy(chosenIndex = q.answerIndex)
        }
    }

    /** 结算每日打卡。 */
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

    /** 错题本列表。 */
    fun mistakeQuestions(): List<QuizQuestion> = repo.allByIds(uiState.mistakeIds.toList())

    /** 收藏列表。 */
    fun favoriteQuestions(): List<QuizQuestion> = repo.allByIds(uiState.favoriteIds.toList())

    fun isFavorite(id: Int): Boolean = id in uiState.favoriteIds

    fun categoryCount(category: String): Int = repo.questionsByCategory(category).size

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