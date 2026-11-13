package com.wencui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wencui.quiz.data.AnswerRecord
import com.wencui.quiz.data.DailyChallenge
import com.wencui.quiz.data.QuizQuestion
import com.wencui.quiz.data.QuizRepository

/** 主页面路由。 */
enum class Screen {
    Home, Quiz, Result, WrongBook
}

/** 全局 UI 状态。 */
data class QuizUiState(
    val screen: Screen = Screen.Home,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val chosenIndex: Int? = null,
    val records: List<AnswerRecord> = emptyList(),
    val sessionCategory: String? = null,
    /** 会话类型：random / daily / category / mistakes */
    val sessionType: String = "random",
    val mistakeIds: Set<Int> = emptySet(),
    // 累计统计（持久化）
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    // 每日一练
    val dailyDone: Boolean = false,
    val streak: Int = 0,
    val bestStreak: Int = 0
) {
    val currentQuestion: QuizQuestion?
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

    // endregion

    // region 刷题会话

    /** 开始一轮随机练习。 */
    fun startRandom(count: Int = DEFAULT_SESSION_SIZE) {
        beginSession(repo.drawRandom(count), category = null, type = "random")
    }

    /** 开始某一分类的练习。 */
    fun startCategory(category: String) {
        beginSession(repo.drawRandom(DEFAULT_SESSION_SIZE, category), category = category, type = "category")
    }

    /** 每日一练：今天固定 10 题；会话结束时计入打卡。 */
    fun startDaily() {
        beginSession(DailyChallenge.todayQuestions(repo.allQuestions), category = null, type = "daily")
    }

    /** 重练错题本。 */
    fun startMistakes() {
        val wrong = repo.allByIds(uiState.mistakeIds.toList())
        if (wrong.isNotEmpty()) beginSession(wrong.shuffled(), category = null, type = "mistakes")
    }

    private fun beginSession(questions: List<QuizQuestion>, category: String?, type: String) {
        if (questions.isEmpty()) return
        uiState = uiState.copy(
            screen = Screen.Quiz,
            questions = questions,
            currentIndex = 0,
            chosenIndex = null,
            records = emptyList(),
            sessionCategory = category,
            sessionType = type
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

    /** 下一题；最后一题则进入结算页（每日一练此时结算打卡）。 */
    fun next() {
        if (uiState.chosenIndex == null) return
        if (uiState.isLastQuestion) {
            if (uiState.sessionType == "daily") settleDaily()
            uiState = uiState.copy(screen = Screen.Result)
        } else {
            uiState = uiState.copy(currentIndex = uiState.currentIndex + 1, chosenIndex = null)
        }
    }

    /** 结算每日打卡：今天完成则更新连胜。 */
    private fun settleDaily() {
        val today = DailyChallenge.todayKey()
        if (saved.lastDailyKey == today) return // 已打过卡
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
            mistakeIds = uiState.mistakeIds.toList()
        )
        prefs.save(saved)
    }

    // endregion

    /** 错题本列表。 */
    fun mistakeQuestions(): List<QuizQuestion> = repo.allByIds(uiState.mistakeIds.toList())

    fun categoryCount(category: String): Int = repo.questionsByCategory(category).size

    fun questionById(id: Int): QuizQuestion? = repo.byId(id)

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