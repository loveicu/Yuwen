package com.wencui.quiz.data

import kotlinx.serialization.Serializable

/**
 * 语文文化常识题目模型。
 *
 * 题库 JSON 中 [explanations] 可为空（向后兼容旧题库）：
 * 有值时每项对应一个选项的详细解释，答题后逐项展示。
 *
 * 运行时通过 [shuffledForPlay] 实现每次开卷选项乱序，
 * 题库文件本身保持稳定顺序（避免 JSON 频繁变更）。
 */
@Serializable
data class QuizQuestion(
    val id: Int,
    val category: String,
    val stem: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String = "",
    /** 每个选项的详细解释（与 options 等长，可空）。 */
    val explanations: List<String> = emptyList()
) {
    val answerText: String get() = options.getOrElse(answerIndex) { "" }

    /**
     * 生成一次「作答视图」：选项随机乱序，并映射出乱序后的正确答案位置。
     * 每次调用产生新的排列 → 每次开卷、每次重练都不同。
     */
    fun shuffledForPlay(): PlayQuestion {
        val order = options.indices.shuffled()
        val newOptions = order.map { options[it] }
        val newAnswer = order.indexOf(answerIndex)
        val newExplanations = if (explanations.size == options.size) {
            order.map { explanations[it] }
        } else {
            explanations
        }
        return PlayQuestion(
            source = this,
            options = newOptions,
            answerIndex = newAnswer,
            explanations = newExplanations
        )
    }
}

/** 一次作答会话中的题目视图（选项已乱序）。 */
data class PlayQuestion(
    val source: QuizQuestion,
    val options: List<String>,
    val answerIndex: Int,
    val explanations: List<String>
) {
    val id: Int get() = source.id
    val category: String get() = source.category
    val stem: String get() = source.stem
    val defaultExplanation: String get() = source.explanation
}

/** 一次刷题会话中的作答记录，用于统计与错题本。 */
@Serializable
data class AnswerRecord(
    val questionId: Int,
    val chosenIndex: Int,
    val correct: Boolean
)