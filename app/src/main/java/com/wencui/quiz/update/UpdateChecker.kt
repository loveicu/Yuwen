package com.wencui.quiz.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** GitHub Release 信息（只取我们关心的字段）。 */
@Serializable
data class ReleaseInfo(
    // GitHub API 返回 snake_case，必须用 @SerialName 映射，
    // 否则反序列化后恒为默认值（历史上 tagName 恒为 "" 的隐藏根因之一）
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    val assets: List<Asset> = emptyList()
) {
    @Serializable
    data class Asset(
        val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String = ""
    )

    val apkAsset: Asset?
        get() = assets.firstOrNull { it.name.endsWith(".apk") }

    /**
     * 解析出的真实版本号（如 "1.8.1"）。
     * tag 恒为 "latest"，真实版本号在 APK 文件名（中文前缀被 GitHub
     * 清洗，如 "-v1.8.1.apk"）或 Release 显示名（"最新构建 v1.8.1"）里，
     * 逐级回退；均解析不出返回 null（调用方退回用 tagName 比较）。
     */
    val version: String?
        get() {
            for (source in listOfNotNull(apkAsset?.name, name)) {
                val m = VERSION_REGEX.find(source) ?: continue
                return m.groupValues[1]
            }
            return null
        }

    companion object {
        /** 匹配 "v1.8.1" / "1.8.1"；要求含点分段，避免误匹配纯数字的 artifact ID。 */
        private val VERSION_REGEX = Regex("""v?(\d+(?:\.\d+)+)""")
    }
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
 * API 请求经 [Net] 双通道（直连 → gh-proxy 兜底）。
 */
object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/loveicu/Yuwen/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }

    /** 挂起函数：在协程中调用（如 LaunchedEffect）。 */
    suspend fun check(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val text = Net.getText(API_URL)
            json.decodeFromString(ReleaseInfo.serializer(), text)
        }.getOrNull()
    }
}

/** APK 下载器：直连失败自动切换 gh-proxy.org 加速。 */
object ApkDownloader {

    /** 非挂起函数：已在后台线程调用，直接阻塞式下载。 */
    fun download(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit
    ): File? {
        val candidates = listOf(url, Net.PROXY + url)
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

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 60000
            setRequestProperty("User-Agent", "WenCuiQuiz-App")
            instanceFollowRedirects = true
        }

        // HTTP 状态码必须显式校验：否则 404 错误页会被当作 APK 写盘
        val code = conn.responseCode
        if (code != 200) throw IllegalStateException("HTTP $code")

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

        // APK 魔数校验（PK\x03\x04）：防代理返回 HTML 错误页
        tmp.inputStream().use { ins ->
            val magic = ByteArray(2)
            if (ins.read(magic) < 2 || magic[0] != 'P'.code.toByte() || magic[1] != 'K'.code.toByte()) {
                throw IllegalStateException("下载内容不是有效的 APK")
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