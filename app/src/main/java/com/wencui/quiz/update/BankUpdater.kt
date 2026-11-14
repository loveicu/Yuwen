package com.wencui.quiz.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** 云端题库清单：Release assets 中的 bank_meta.json。 */
@Serializable
data class BankMeta(
    val version: Int = 0,
    val fileName: String = "bank_override.json",
    val questionCount: Int = 0,
    val notes: String = ""
)

/**
 * 统一网络出口：所有请求先直连、失败自动切 gh-proxy.org 代理。
 * api.github.com / github.com / raw 等全部覆盖。
 */
internal object Net {

    const val PROXY = "https://gh-proxy.org/"

    fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 15000
            setRequestProperty("User-Agent", "WenCuiQuiz-App")
            setRequestProperty("Accept", "application/vnd.github+json")
        }

    /** 带代理兜底的 GET 文本：直连失败自动重试代理。 */
    fun getText(url: String): String {
        val candidates = listOf(url, PROXY + url)
        var lastErr: Exception? = null
        for (u in candidates) {
            try {
                val conn = open(u)
                if (conn.responseCode == 200) {
                    return conn.inputStream.bufferedReader().use { it.readText() }
                }
                lastErr = IllegalStateException("HTTP ${conn.responseCode}")
            } catch (e: Exception) {
                lastErr = e
            }
        }
        throw lastErr ?: IllegalStateException("下载失败")
    }
}

/**
 * 题库云端更新器：
 * 1. 拉 Release assets 里的 bank_meta.json 比对版本
 * 2. 版本更新则下载 bank_override.json 交由 QuizRepository 校验落盘
 * 全部请求经 [Net] 走「直连 → gh-proxy 兜底」双通道。
 */
object BankUpdater {

    private const val RELEASE_API =
        "https://api.github.com/repos/loveicu/Yuwen/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    /** 更新结果。 */
    sealed interface Result {
        data object UpToDate : Result
        data class Success(val newVersion: Int, val count: Int) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun checkAndUpdate(currentVersion: Int, apply: (version: Int, json: String) -> Boolean): Result =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                // 1. Release 资产列表（带代理兜底）
                val releaseText = Net.getText(RELEASE_API)
                val release = json.parseToJsonElement(releaseText)
                val assets = release.jsonObject["assets"]?.jsonArray ?: emptyList()
                val assetUrl: (name: String) -> String? = { name ->
                    assets.firstOrNull { it.jsonObject["name"]?.jsonPrimitive?.content == name }
                        ?.jsonObject["browser_download_url"]?.jsonPrimitive?.content
                }

                // 2. 元数据比对
                val metaUrl = assetUrl("bank_meta.json") ?: return@runCatching Result.UpToDate
                val meta = json.decodeFromString(BankMeta.serializer(), Net.getText(metaUrl))
                if (meta.version <= currentVersion) return@runCatching Result.UpToDate

                // 3. 下载题库正文并应用
                val bankUrl = assetUrl(meta.fileName) ?: return@runCatching Result.Failure("找不到 ${meta.fileName}")
                val bankJson = Net.getText(bankUrl)
                if (apply(meta.version, bankJson)) {
                    Result.Success(meta.version, meta.questionCount)
                } else {
                    Result.Failure("题库数据校验失败")
                }
            }.getOrElse { Result.Failure(it.message ?: "网络错误") }
        }

    // ---- kotlinx.json 便捷扩展 ----

    private val kotlinx.serialization.json.JsonElement.jsonObject: Map<String, kotlinx.serialization.json.JsonElement>
        get() = this as kotlinx.serialization.json.JsonObject

    private val kotlinx.serialization.json.JsonElement.jsonArray: List<kotlinx.serialization.json.JsonElement>
        get() = (this as kotlinx.serialization.json.JsonArray).toList()

    private val kotlinx.serialization.json.JsonElement.jsonPrimitive: kotlinx.serialization.json.JsonPrimitive
        get() = this as kotlinx.serialization.json.JsonPrimitive
}