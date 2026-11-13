package com.loom.build.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import com.loom.build.ui.theme.Phase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 应用的全部状态与流程编排。刻意不引入 ViewModel / DI，
 * 保持单文件可读：谁改了什么，一眼能看到。
 */
class BuildStore(private val app: Context) {

    private val prefs = Prefs(app)

    // ---------- 账号 ----------
    var token by mutableStateOf(prefs.token)
        private set
    var login by mutableStateOf(prefs.login)
        private set

    val bound: Boolean get() = token.isNotBlank()

    // ---------- 项目 ----------
    var folderUri by mutableStateOf(prefs.lastFolder)
        private set
    var folderName by mutableStateOf("")
        private set
    var repoName by mutableStateOf(prefs.repo)
        private set
    var isPrivate by mutableStateOf(true)
        private set

    var fileCount by mutableStateOf(0)
        private set
    var totalBytes by mutableStateOf(0L)
        private set
    var skipped by mutableStateOf(0)
        private set
    var gradleOk by mutableStateOf(false)
        private set
    var scanNote by mutableStateOf("")
        private set

    // ---------- 构建 ----------
    var phase by mutableStateOf(Phase.IDLE)
        private set
    var headline by mutableStateOf("尚未开始")
        private set
    var detail by mutableStateOf("")
        private set
    var progress by mutableStateOf(0f)
        private set
    var runUrl by mutableStateOf("")
        private set
    var apkFile by mutableStateOf<File?>(null)
        private set
    var apkLabel by mutableStateOf("")
        private set
    var error by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set

    fun updateRepoName(value: String) {
        repoName = slug(value)
        prefs.repo = repoName
    }

    fun setIsPrivate(value: Boolean) {
        isPrivate = value
    }

    // ---------- 绑定 ----------

    suspend fun bind(raw: String) {
        val candidate = raw.trim()
        if (candidate.isEmpty()) {
            error = "先把令牌粘贴进来"
            return
        }
        busy = true
        error = ""
        try {
            val me = withContext(Dispatchers.IO) { GitHub(candidate).user() }
            val name = me.optString("login")
            token = candidate
            login = name
            prefs.token = candidate
            prefs.login = name
            if (repoName.isBlank()) updateRepoName("$name-app")
        } catch (e: Exception) {
            error = e.message ?: "绑定失败"
        } finally {
            busy = false
        }
    }

    fun unbind() {
        prefs.clearAuth()
        token = ""
        login = ""
        reset()
    }

    // ---------- 选目录 ----------

    suspend fun pickFolder(uri: Uri) {
        try {
            app.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: Exception) {
            // 某些文件管理器不授予持久权限，仅本次可用；扫描仍可进行。
        }
        folderUri = uri.toString()
        prefs.lastFolder = folderUri
        folderName = DocumentFile.fromTreeUri(app, uri)?.name ?: "已选目录"
        if (repoName.isBlank()) updateRepoName(folderName)
        rescan()
    }

    suspend fun rescan() {
        val uri = folderUri
        if (uri.isBlank()) return
        phase = Phase.SCANNING
        headline = "读取项目"
        detail = folderName
        busy = true
        error = ""
        try {
            val scan = withContext(Dispatchers.IO) { ProjectScanner.scan(app, Uri.parse(uri)) }
            fileCount = scan.files.size
            totalBytes = scan.bytes
            skipped = scan.skipped
            gradleOk = scan.gradleProject
            scanNote = buildString {
                append("${scan.files.size} 个文件 · ${formatSize(scan.bytes)}")
                if (scan.skipped > 0) append(" · 跳过 ${scan.skipped}")
                if (scan.truncated) append(" · 已达文件上限")
            }
            phase = if (scan.gradleProject) Phase.IDLE else Phase.ERROR
            headline = if (scan.gradleProject) "准备就绪" else "这不是 Gradle 项目"
            detail = if (scan.gradleProject) {
                "可直接上传构建"
            } else {
                "根目录缺少 settings.gradle(.kts) / build.gradle(.kts)"
            }
            if (!scan.gradleProject) error = "请选择 Android Studio 项目的根目录"
        } catch (e: Exception) {
            phase = Phase.ERROR
            error = e.message ?: "读取失败"
        } finally {
            busy = false
        }
    }

    // ---------- 上传 + 构建 ----------

    suspend fun start() {
        if (busy) return
        if (token.isBlank()) {
            error = "请先在设置里绑定 GitHub"
            return
        }
        if (folderUri.isBlank()) {
            error = "请先选择项目文件夹"
            return
        }
        if (repoName.isBlank()) {
            error = "请填写仓库名"
            return
        }

        busy = true
        error = ""
        apkFile = null
        apkLabel = ""
        runUrl = ""
        progress = 0f

        try {
            val owner = if (login.isNotBlank()) login else withContext(Dispatchers.IO) {
                GitHub(token).user().optString("login")
            }
            login = owner
            prefs.login = owner

            phase = Phase.SCANNING
            headline = "读取项目"
            detail = folderName
            val scan = withContext(Dispatchers.IO) {
                ProjectScanner.scan(app, Uri.parse(folderUri))
            }
            if (!scan.gradleProject) {
                throw IllegalStateException("根目录缺少 Gradle 构建文件，无法构建")
            }
            val workflowText = WorkflowTemplate.load(app)
            val hasWorkflow = scan.files.any { it.path == GitHub.WORKFLOW_PATH }

            val gh = GitHub(token)

            phase = Phase.UPLOADING
            headline = "准备仓库"
            detail = "$owner/$repoName"
            val exists = withContext(Dispatchers.IO) { gh.repo(owner, repoName) }
            if (exists == null) {
                withContext(Dispatchers.IO) {
                    gh.createRepo(repoName, isPrivate, "由 Loom Build 上传并云端构建")
                }
            }

            val branch = "main"
            val head = withContext(Dispatchers.IO) { gh.branchHead(owner, repoName, branch) }
            val baseTree = head?.let { withContext(Dispatchers.IO) { gh.commitTree(owner, repoName, it) } }

            val total = scan.files.size + if (hasWorkflow) 0 else 1
            val entries = ArrayList<TreeEntry>()
            var done = 0

            phase = Phase.UPLOADING
            headline = "上传文件"
            coroutineScope {
                val gate = Semaphore(4)
                val jobs = scan.files.map { entry ->
                    async(Dispatchers.IO) {
                        gate.withPermit {
                            val bytes = ProjectScanner.readBytes(app, entry)
                            if (bytes == null) {
                                null
                            } else {
                                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                val sha = gh.createBlob(owner, repoName, b64)
                                val result = TreeEntry(entry.path, sha)
                                withContext(Dispatchers.Main) {
                                    done++
                                    progress = done.toFloat() / total
                                    detail = "$done / $total"
                                }
                                result
                            }
                        }
                    }
                }
                entries.addAll(jobs.awaitAll().filterNotNull())
            }

            if (!hasWorkflow) {
                val sha = withContext(Dispatchers.IO) {
                    gh.createBlob(
                        owner,
                        repoName,
                        Base64.encodeToString(workflowText.toByteArray(), Base64.NO_WRAP),
                    )
                }
                entries.add(TreeEntry(GitHub.WORKFLOW_PATH, sha))
                done++
                progress = 1f
                detail = "已注入 build-apk.yml"
            }

            if (entries.isEmpty()) throw IllegalStateException("没有可上传的文件")

            phase = Phase.COMMITTING
            headline = "提交到 GitHub"
            val commitSha = withContext(Dispatchers.IO) {
                val tree = gh.createTree(owner, repoName, baseTree, entries)
                if (head == null) {
                    val commit = gh.createCommit(
                        owner,
                        repoName,
                        "Loom Build：上传项目",
                        tree,
                        emptyList(),
                    )
                    gh.createRef(owner, repoName, branch, commit)
                    commit
                } else {
                    val commit = gh.createCommit(
                        owner,
                        repoName,
                        "Loom Build：更新项目",
                        tree,
                        listOf(head),
                    )
                    gh.updateRef(owner, repoName, branch, commit)
                    commit
                }
            }

            phase = Phase.QUEUED
            headline = "触发云端构建"
            detail = commitSha.take(7)
            withContext(Dispatchers.IO) {
                // workflow 文件刚提交时可能尚未被索引，404 属于正常，稍后由 push 触发兜底。
                repeat(3) {
                    try {
                        gh.dispatch(owner, repoName, branch)
                        return@withContext
                    } catch (e: GitHubException) {
                        if (e.status != 404) throw e
                    }
                    Thread.sleep(2500)
                }
            }

            // 等待 run 出现
            val findDeadline = System.currentTimeMillis() + 120_000
            var runId = 0L
            while (runId == 0L && System.currentTimeMillis() < findDeadline) {
                val arr = withContext(Dispatchers.IO) {
                    gh.runs(owner, repoName, branch).optJSONArray("workflow_runs")
                }
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val r = arr.getJSONObject(i)
                        if (r.optString("head_sha") == commitSha) {
                            runId = r.optLong("id")
                            runUrl = r.optString("html_url")
                            break
                        }
                    }
                }
                if (runId == 0L) delay(4000)
            }
            if (runId == 0L) throw IllegalStateException("没有找到对应的 Actions 运行，请到仓库确认 Actions 是否开启")

            phase = Phase.RUNNING
            headline = "云端构建中"
            val runDeadline = System.currentTimeMillis() + 30 * 60 * 1000
            var conclusion = ""
            while (true) {
                val run = withContext(Dispatchers.IO) { gh.run(owner, repoName, runId) }
                val status = run.optString("status")
                conclusion = run.optString("conclusion")
                detail = when (status) {
                    "queued" -> "排队中"
                    "in_progress" -> "编译中"
                    "completed" -> "已结束"
                    else -> status
                }
                runUrl = run.optString("html_url", runUrl)
                if (status == "completed") break
                if (System.currentTimeMillis() > runDeadline) {
                    throw IllegalStateException("构建超时（30 分钟）")
                }
                delay(5000)
            }

            if (conclusion != "success") {
                phase = Phase.ERROR
                headline = "构建失败"
                detail = "结论：$conclusion"
                error = "打开构建日志查看原因（右上角可跳转）"
                return
            }

            phase = Phase.DOWNLOADING
            headline = "下载产物"
            detail = ""

            val arts = withContext(Dispatchers.IO) {
                gh.artifacts(owner, repoName, runId).optJSONArray("artifacts")
            }
            var artifactId = 0L
            var artifactName = ""
            if (arts != null) {
                for (i in 0 until arts.length()) {
                    val a = arts.getJSONObject(i)
                    if (a.optLong("size_in_bytes") > 0) {
                        artifactId = a.optLong("id")
                        artifactName = a.optString("name")
                        break
                    }
                }
            }
            if (artifactId == 0L) throw IllegalStateException("这次运行没有产物，检查是否真的产出了 APK")

            val outDir = File(app.cacheDir, "apk").apply {
                deleteRecursively()
                mkdirs()
            }
            val zip = File(outDir, "$artifactName.zip")
            withContext(Dispatchers.IO) { gh.downloadArtifact(owner, repoName, artifactId, zip) }

            val apk = withContext(Dispatchers.IO) { extractApk(zip, outDir) }
                ?: throw IllegalStateException("产物里没有 .apk 文件")
            zip.delete()

            apkFile = apk
            apkLabel = apk.name
            phase = Phase.DONE
            headline = "构建完成"
            detail = "${apk.name} · ${formatSize(apk.length())}"
            progress = 1f
        } catch (e: Exception) {
            phase = Phase.ERROR
            if (headline == "尚未开始" || headline == "") headline = "出错"
            error = e.message ?: e.toString()
        } finally {
            busy = false
        }
    }

    fun reset() {
        phase = Phase.IDLE
        headline = "尚未开始"
        detail = ""
        progress = 0f
        error = ""
    }

    fun clearApk() {
        apkFile?.delete()
        apkFile = null
        apkLabel = ""
    }

    // ---------- 工具 ----------

    private fun extractApk(zip: File, outDir: File): File? {
        var debug: File? = null
        var biggest: File? = null
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                    val safe = File(outDir, File(entry.name).name)
                    safe.outputStream().use { out -> zis.copyTo(out) }
                    if (safe.name.contains("debug", ignoreCase = true)) debug = safe
                    if (biggest == null || safe.length() > biggest!!.length()) biggest = safe
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return debug ?: biggest
    }

    companion object {
        fun slug(input: String): String {
            val sb = StringBuilder()
            for (ch in input.lowercase()) {
                sb.append(
                    if (ch in 'a'..'z' || ch in '0'..'9' || ch == '-' || ch == '_' || ch == '.') ch else '-',
                )
            }
            var out = sb.toString()
            while (out.startsWith("-") || out.startsWith(".")) out = out.drop(1)
            while (out.endsWith("-") || out.endsWith(".")) out = out.dropLast(1)
            if (out.isEmpty()) out = "loom-app"
            return out.take(60)
        }

        fun formatSize(bytes: Long): String = when {
            bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / 1024.0 / 1024 / 1024)
            bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024)
            bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
