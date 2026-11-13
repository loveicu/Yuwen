package com.wencui.quiz.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** assets/question_bank.json 的外层结构。 */
@Serializable
private data class QuestionBankFile(
    val version: Int = 1,
    val questions: List<QuizQuestion>
)

/**
 * 题库仓库：负责从 assets 加载题库，并提供分类、随机组卷与错题重练能力。
 * 全部内存缓存，题目规模小（几百题），无需数据库。
 */
class QuizRepository private constructor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val allQuestions: List<QuizQuestion>
    private val byIdMap: Map<Int, QuizQuestion>

    init {
        val text = context.assets.open(BANK_FILE).bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString(QuestionBankFile.serializer(), text)
        allQuestions = parsed.questions
        byIdMap = allQuestions.associateBy { it.id }
    }

    /** 供持久化层使用的应用上下文。 */
    val appContext: Context get() = context.applicationContext

    val categories: List<String> by lazy {
        allQuestions.map { it.category }.distinct().sorted()
    }

    val totalCount: Int get() = allQuestions.size

    fun questionsByCategory(category: String): List<QuizQuestion> =
        allQuestions.filter { it.category == category }

    fun byId(id: Int): QuizQuestion? = byIdMap[id]

    fun allByIds(ids: List<Int>): List<QuizQuestion> =
        ids.mapNotNull { byIdMap[it] }

    /** 随机抽取 n 题（不重复），可按分类过滤。 */
    fun drawRandom(count: Int, category: String? = null): List<QuizQuestion> {
        val pool = if (category == null) allQuestions else questionsByCategory(category)
        return pool.shuffled().take(count)
    }

    companion object {
        private const val BANK_FILE = "question_bank.json"

        @Volatile
        private var instance: QuizRepository? = null

        fun get(context: Context): QuizRepository =
            instance ?: synchronized(this) {
                instance ?: QuizRepository(context.applicationContext).also { instance = it }
            }
    }
}