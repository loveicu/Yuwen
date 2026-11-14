package com.wencui.quiz.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** assets/question_bank.json 的外层结构。 */
@Serializable
private data class QuestionBankFile(
    val version: Int = 1,
    val questions: List<QuizQuestion>
)

/**
 * 题库仓库：内置 assets + 云端更新双源。
 * 云端题库（下载于 cacheDir/bank_override.json）优先于内置题库。
 */
class QuizRepository private constructor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var allQuestionsInternal: List<QuizQuestion>
    private var byIdMap: Map<Int, QuizQuestion>
    private var bankVersionInternal: Int

    /** 当前生效的题库版本（云端更新后大于内置版本）。 */
    val bankVersion: Int get() = bankVersionInternal

    init {
        val (loaded, version) = loadBestBank()
        allQuestionsInternal = loaded
        byIdMap = loaded.associateBy { it.id }
        bankVersionInternal = version
    }

    /** 内置 assets 优先级低于已下载的云端题库。 */
    private fun loadBestBank(): Pair<List<QuizQuestion>, Int> {
        val builtin = runCatching { parseFrom(context.assets.open(BANK_FILE)) }
            .getOrDefault(QuestionBankFile(1, emptyList()))

        val overrideFile = File(context.cacheDir, OVERRIDE_FILE)
        val remote = if (overrideFile.exists()) {
            runCatching { parseFrom(overrideFile.inputStream()) }.getOrNull()
        } else null

        return if (remote != null && remote.version >= builtin.version) {
            remote.questions to remote.version
        } else {
            builtin.questions to builtin.version
        }
    }

    private fun parseFrom(stream: java.io.InputStream): QuestionBankFile =
        stream.bufferedReader().use { reader ->
            json.decodeFromString(QuestionBankFile.serializer(), reader.readText())
        }

    /** 应用云端题库（校验通过后落盘并刷新内存缓存）。 */
    fun applyCloudBank(version: Int, questions: List<QuizQuestion>): Boolean {
        if (questions.isEmpty() || version <= bankVersionInternal) return false
        val file = File(context.cacheDir, OVERRIDE_FILE)
        runCatching {
            file.writeText(json.encodeToString(QuestionBankFile.serializer(), QuestionBankFile(version, questions)))
        }.getOrElse { return false }
        allQuestionsInternal = questions
        byIdMap = questions.associateBy { it.id }
        bankVersionInternal = version
        return true
    }

    /** 供持久化层使用的应用上下文。 */
    val appContext: Context get() = context.applicationContext

    val categories: List<String> by lazy { refreshCategories() }

    private fun refreshCategories(): List<String> =
        allQuestionsInternal.map { it.category }.distinct().sorted()

    /** 全部题目（只读视图）。 */
    val allQuestions: List<QuizQuestion> get() = allQuestionsInternal

    val totalCount: Int get() = allQuestionsInternal.size

    /** 分类变化时刷新（云端题库可能带来新分类）。 */
    fun notifyBankChanged() {
        // categories 是 lazy val，无法重置；改为每次动态计算
    }

    fun categoriesNow(): List<String> =
        allQuestionsInternal.map { it.category }.distinct().sorted()

    fun questionsByCategory(category: String): List<QuizQuestion> =
        allQuestionsInternal.filter { it.category == category }

    fun byId(id: Int): QuizQuestion? = byIdMap[id]

    fun allByIds(ids: List<Int>): List<QuizQuestion> =
        ids.mapNotNull { byIdMap[it] }

    /** 关键词搜索：匹配题干/选项/分类/解析，返回去重后的题目。 */
    fun search(query: String): List<QuizQuestion> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return allQuestionsInternal.filter { question ->
            question.stem.contains(q, ignoreCase = true) ||
                question.category.contains(q, ignoreCase = true) ||
                question.explanation.contains(q, ignoreCase = true) ||
                question.options.any { it.contains(q, ignoreCase = true) }
        }
    }

    /** 随机抽取 n 题（不重复），可按分类过滤。 */
    fun drawRandom(count: Int, category: String? = null): List<QuizQuestion> {
        val pool = if (category == null) allQuestionsInternal else questionsByCategory(category)
        return pool.shuffled().take(count)
    }

    companion object {
        private const val BANK_FILE = "question_bank.json"
        private const val OVERRIDE_FILE = "bank_override.json"

        @Volatile
        private var instance: QuizRepository? = null

        fun get(context: Context): QuizRepository =
            instance ?: synchronized(this) {
                instance ?: QuizRepository(context.applicationContext).also { instance = it }
            }
    }
}