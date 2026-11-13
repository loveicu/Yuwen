package com.wencui.quiz.data

import java.security.MessageDigest

/**
 * 每日一练：基于日期生成确定性随机抽题。
 * 同一天所有用户抽到同一套题，跨天自动换卷。
 */
object DailyChallenge {

    /** 用日期字符串生成 64 位种子。 */
    private fun seedOf(dateKey: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(dateKey.toByteArray())
        return ((digest[0].toLong() and 0xff) shl 56) or
                ((digest[1].toLong() and 0xff) shl 48) or
                ((digest[2].toLong() and 0xff) shl 40) or
                ((digest[3].toLong() and 0xff) shl 32) or
                ((digest[4].toLong() and 0xff) shl 24) or
                ((digest[5].toLong() and 0xff) shl 16) or
                ((digest[6].toLong() and 0xff) shl 8) or
                (digest[7].toLong() and 0xff)
    }

    /** 确定性洗牌（Fisher-Yates with seeded LCG）。 */
    private fun seededShuffle(list: List<QuizQuestion>, seed: Long): List<QuizQuestion> {
        val result = list.toMutableList()
        var state = seed
        fun next(): Long {
            state = state * 6364136223846793005L + 1442695040888963407L
            return state ushr 33
        }
        for (i in result.size - 1 downTo 1) {
            val j = (next() % (i + 1)).toInt()
            result[i] = result[j].also { result[j] = result[i] }
        }
        return result
    }

    /** 取今天的日期 key（本地时区）。 */
    fun todayKey(): String {
        val cal = java.util.Calendar.getInstance()
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    /** 生成今日题目（每天固定 n 题）。 */
    fun todayQuestions(all: List<QuizQuestion>, count: Int = 10): List<QuizQuestion> =
        seededShuffle(all, seedOf(todayKey())).take(count)

    /** 昨天是否已完成（供连续打卡判断）。 */
    fun yesterdayKey(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }
}