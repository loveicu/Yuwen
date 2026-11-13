package com.wencui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wencui.quiz.data.AnswerRecord
import com.wencui.quiz.data.QuizQuestion
import com.wencui.quiz.data.QuizRepository
import kotlinx.coroutines.launch

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
    val mistakeIds: Set<Int> = emptySet(),
    // 累计统计（持久化）
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0
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

    init {
        val saved = prefs.load()
        uiState = uiState.copy(
            totalAnswered = saved.totalAnswered,
            totalCorrect = saved.totalCorrect,
            mistakeIds = saved.mistakeIds.toSet()
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
        beginSession(repo.drawRandom(count), category = null)
    }

    /** 开始某一分类的练习。 */
    fun startCategory(category: String) {
        beginSession(repo.drawRandom(DEFAULT_SESSION_SIZE, category), category = category)
    }

    /** 重练错题本。 */
    fun startMistakes() {
        val wrong = repo.allByIds(uiState.mistakeIds.toList())
        if (wrong.isNotEmpty()) beginSession(wrong.shuffled(), category = null)
    }

    private fun beginSession(questions: List<QuizQuestion>, category: String?) {
        if (questions.isEmpty()) return
        uiState = uiState.copy(
            screen = Screen.Quiz,
            questions = questions,
            currentIndex = 0,
            chosenIndex = null,
            records = emptyList(),
            sessionCategory = category
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
        prefs.save(uiState.totalAnswered, uiState.totalCorrect, uiState.mistakeIds)
    }

    /** 下一题；最后一题则进入结算页。 */
    fun next() {
        if (uiState.chosenIndex == null) return
        if (uiState.isLastQuestion) {
            uiState = uiState.copy(screen = Screen.Result)
        } else {
            uiState = uiState.copy(currentIndex = uiState.currentIndex + 1, chosenIndex = null)
        }
    }

    // endregion

    /** 错题本列表（按题库顺序）。 */
    fun mistakeQuestions(): List<QuizQuestion> = repo.allByIds(uiState.mistakeIds.toList())

    fun categoryCount(category: String): Int = repo.questionsByCategory(category).size

    fun questionById(id: Int): QuizQuestion? = repo.byId(id)

    companion object {
        const val DEFAULT_SESSION_SIZE = 15

        fun factory(repo: QuizRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QuizViewModel(repo) as T
            }
    }
}