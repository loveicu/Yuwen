package com.wencui.quiz.update

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** GitHub Release 信息（只取我们关心的字段）。 */
@Serializable
data class ReleaseInfo(
    val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    val assets: List<Asset> = emptyList()
) {
    @Serializable
    data class Asset(
        val name: String,
        val browserDownloadUrl: String = ""
    )

    val apkAsset: Asset?
        get() = assets.firstOrNull { it.name.endsWith(".apk") }
}

/** 版本比较：1.2.10 > 1.2.9，逐段数值化比较。 */
fun isNewerVersion(remote: String, local: String): Boolean {
    fun parts(v: String) = v.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(remote)
    val b = parts(local)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/**
 * 更新检查器：查询 GitHub Releases 最新版本。
 * 直接走 api.github.com（国内网络对 API 一般可达）。
 */
object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/loveicu/Yuwen/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "WenCuiQuiz-App")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return@runCatching null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(ReleaseInfo.serializer(), text)
        }.getOrNull()
    }
}

/** APK 下载器：优先直连，失败自动切换 gh-proxy.org 加速。 */
object ApkDownloader {

    /** 非挂起函数：已在后台线程调用，直接阻塞式下载。 */
    fun download(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit
    ): File? {
        val candidates = buildList {
            add(url)
            // 直连下载失败概率高时，代理前缀兜底（release 下载对国内不友好）
            add("https://gh-proxy.org/$url")
        }

        for (candidate in candidates) {
            val file = runCatching { downloadOne(context, candidate, onProgress) }.getOrNull()
            if (file != null && file.length() > 100_000) return file
        }
        return null
    }

    private fun downloadOne(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit
    ): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val tmp = File(dir, "update.apk.tmp")
        val out = File(dir, "update.apk")
        tmp.delete(); out.delete()

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 30000
        conn.setRequestProperty("User-Agent", "WenCuiQuiz-App")
        conn.instanceFollowRedirects = true

        val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
        var downloaded = 0L

        conn.inputStream.use { input ->
            tmp.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    output.write(buf, 0, n)
                    downloaded += n
                    if (total > 0) onProgress((downloaded.toDouble() / total).toFloat())
                }
            }
        }

        if (!tmp.renameTo(out)) {
            tmp.copyTo(out, overwrite = true)
            tmp.delete()
        }
        onProgress(1f)
        return out
    }
}