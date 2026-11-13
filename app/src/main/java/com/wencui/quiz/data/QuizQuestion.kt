package com.wencui.quiz.data

import kotlinx.serialization.Serializable

/**
 * 语文文化常识题目模型。
 *
 * 题库以 JSON 形式打包在 assets 中，结构：
 * ```json
 * {
 *   "id": 1,
 *   "category": "科举制度",
 *   "stem": "\"连中三元\"中的三元指的是？",
 *   "options": ["乡试、会试、殿试的第一名", "..."],
 *   "answerIndex": 0,
 *   "explanation": "解元、会元、状元分别指三级考试的第一名。"
 * }
 * ```
 */
@Serializable
data class QuizQuestion(
    val id: Int,
    val category: String,
    val stem: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String = ""
) {
    val answerText: String get() = options.getOrElse(answerIndex) { "" }
}

/** 一次刷题会话中的作答记录，用于统计与错题本。 */
@Serializable
data class AnswerRecord(
    val questionId: Int,
    val chosenIndex: Int,
    val correct: Boolean
)
