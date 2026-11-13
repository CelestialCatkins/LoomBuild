package com.loom.build.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalEntry(val path: String, val uri: Uri)

data class ScanResult(
    val files: List<LocalEntry>,
    val bytes: Long,
    val gradleProject: Boolean,
    val skipped: Int,
    val truncated: Boolean,
)

/**
 * 通过 SAF 树 URI 遍历本地项目。不申请全盘存储权限，只读用户亲手选中的目录。
 * 过滤构建产物与 VCS 元数据，避免把 APK / gradle 缓存推上去。
 */
object ProjectScanner {

    private val SKIP_DIRS = setOf(
        ".git", ".gradle", "build", ".idea", "node_modules",
        ".cxx", "captures", ".externalNativeBuild", "out",
    )
    private const val MAX_FILES = 800
    private const val MAX_FILE_BYTES = 12L * 1024 * 1024

    suspend fun scan(context: Context, tree: Uri): ScanResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, tree)
            ?: return@withContext ScanResult(emptyList(), 0, false, 0, false)

        val files = ArrayList<LocalEntry>()
        var bytes = 0L
        var skipped = 0
        var gradle = false
        var truncated = false

        fun walk(dir: DocumentFile, prefix: String) {
            if (files.size >= MAX_FILES) {
                truncated = true
                return
            }
            val children = dir.listFiles()
            for (child in children) {
                val name = child.name ?: continue
                if (child.isDirectory) {
                    if (name in SKIP_DIRS) {
                        skipped++
                        continue
                    }
                    walk(child, if (prefix.isEmpty()) name else "$prefix/$name")
                } else {
                    val path = if (prefix.isEmpty()) name else "$prefix/$name"
                    if (name.startsWith("settings.gradle") || name == "build.gradle" ||
                        name == "build.gradle.kts"
                    ) {
                        gradle = true
                    }
                    if (child.length() > MAX_FILE_BYTES) {
                        skipped++
                        continue
                    }
                    if (files.size >= MAX_FILES) {
                        truncated = true
                        continue
                    }
                    files.add(LocalEntry(path, child.uri))
                    bytes += child.length()
                }
            }
        }

        walk(root, "")
        ScanResult(files, bytes, gradle, skipped, truncated)
    }

    fun readBytes(context: Context, entry: LocalEntry): ByteArray? = try {
        context.contentResolver.openInputStream(entry.uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        null
    }
}
