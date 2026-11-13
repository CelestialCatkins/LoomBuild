package com.loom.build.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubException(val status: Int, message: String) : IOException(message)

data class TreeEntry(val path: String, val sha: String)

/**
 * GitHub REST 的最小封装。只覆盖本应用真正用到的端点。
 * 不使用第三方 SDK，避免在 Android 上引入不必要的体积与兼容风险。
 */
class GitHub(private val token: String) {

    companion object {
        const val API = "https://api.github.com"
        const val WORKFLOW_FILE = "build-apk.yml"
        const val WORKFLOW_PATH = ".github/workflows/build-apk.yml"
    }

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()

    private fun call(
        method: String,
        path: String,
        body: JSONObject? = null,
        accept: String = "application/vnd.github+json",
    ): String {
        val rb = body?.toString()?.toRequestBody(jsonType)
        val builder = Request.Builder()
            .url(API + path)
            .header("Authorization", "Bearer $token")
            .header("Accept", accept)
            .header("User-Agent", "LoomBuild")
            .header("X-GitHub-Api-Version", "2022-11-28")
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(rb ?: "{}".toRequestBody(jsonType))
            "PATCH" -> builder.patch(rb ?: "{}".toRequestBody(jsonType))
            "PUT" -> builder.put(rb ?: "{}".toRequestBody(jsonType))
            "DELETE" -> builder.delete()
            else -> throw IllegalArgumentException("unsupported method $method")
        }
        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw GitHubException(resp.code, describe(text, resp.code))
            }
            return text
        }
    }

    private fun describe(text: String, code: Int): String {
        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
        return when {
            !msg.isNullOrBlank() -> "GitHub 返回 $code：$msg"
            else -> "GitHub 返回 $code"
        }
    }

    fun user(): JSONObject = JSONObject(call("GET", "/user"))

    fun repo(owner: String, name: String): JSONObject? = try {
        JSONObject(call("GET", "/repos/$owner/$name"))
    } catch (e: GitHubException) {
        if (e.status == 404) null else throw e
    }

    fun createRepo(name: String, isPrivate: Boolean, description: String): JSONObject =
        JSONObject(
            call(
                "POST",
                "/user/repos",
                JSONObject()
                    .put("name", name)
                    .put("private", isPrivate)
                    .put("description", description)
                    .put("auto_init", false),
            ),
        )

    /** 分支头提交；仓库为空时返回 null（GitHub 会给出 404 或 409）。 */
    fun branchHead(owner: String, name: String, branch: String): String? = try {
        JSONObject(call("GET", "/repos/$owner/$name/git/ref/heads/$branch"))
            .getJSONObject("object")
            .getString("sha")
    } catch (e: GitHubException) {
        if (e.status == 404 || e.status == 409) null else throw e
    }

    fun commitTree(owner: String, name: String, commitSha: String): String =
        JSONObject(call("GET", "/repos/$owner/$name/git/commits/$commitSha"))
            .getJSONObject("tree")
            .getString("sha")

    fun createBlob(owner: String, name: String, base64: String): String =
        JSONObject(
            call(
                "POST",
                "/repos/$owner/$name/git/blobs",
                JSONObject().put("content", base64).put("encoding", "base64"),
            ),
        ).getString("sha")

    fun createTree(owner: String, name: String, baseTree: String?, entries: List<TreeEntry>): String {
        val arr = JSONArray()
        for (e in entries) {
            arr.put(
                JSONObject()
                    .put("path", e.path)
                    .put("mode", "100644")
                    .put("type", "blob")
                    .put("sha", e.sha),
            )
        }
        val body = JSONObject().put("tree", arr)
        if (baseTree != null) body.put("base_tree", baseTree)
        return JSONObject(call("POST", "/repos/$owner/$name/git/trees", body)).getString("sha")
    }

    fun createCommit(
        owner: String,
        name: String,
        message: String,
        tree: String,
        parents: List<String>,
    ): String {
        val arr = JSONArray()
        for (p in parents) arr.put(p)
        val body = JSONObject()
            .put("message", message)
            .put("tree", tree)
            .put("parents", arr)
        return JSONObject(call("POST", "/repos/$owner/$name/git/commits", body)).getString("sha")
    }

    fun createRef(owner: String, name: String, branch: String, sha: String) {
        call(
            "POST",
            "/repos/$owner/$name/git/refs",
            JSONObject().put("ref", "refs/heads/$branch").put("sha", sha),
        )
    }

    fun updateRef(owner: String, name: String, branch: String, sha: String) {
        call(
            "PATCH",
            "/repos/$owner/$name/git/refs/heads/$branch",
            JSONObject().put("sha", sha).put("force", false),
        )
    }

    /** 触发 workflow_dispatch；workflow 尚未被索引时 GitHub 返回 404。 */
    fun dispatch(owner: String, name: String, branch: String) {
        call(
            "POST",
            "/repos/$owner/$name/actions/workflows/$WORKFLOW_FILE/dispatches",
            JSONObject().put("ref", branch),
        )
    }

    fun runs(owner: String, name: String, branch: String): JSONObject =
        JSONObject(call("GET", "/repos/$owner/$name/actions/runs?branch=$branch&per_page=20"))

    fun run(owner: String, name: String, runId: Long): JSONObject =
        JSONObject(call("GET", "/repos/$owner/$name/actions/runs/$runId"))

    fun artifacts(owner: String, name: String, runId: Long): JSONObject =
        JSONObject(call("GET", "/repos/$owner/$name/actions/runs/$runId/artifacts"))

    fun downloadArtifact(owner: String, name: String, artifactId: Long, dest: File) {
        val req = Request.Builder()
            .url("$API/repos/$owner/$name/actions/artifacts/$artifactId/zip")
            .header("Authorization", "Bearer $token")
            .header("User-Agent", "LoomBuild")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw GitHubException(resp.code, "下载产物失败（HTTP ${resp.code}）")
            }
            val body = resp.body ?: throw IOException("产物响应为空")
            body.byteStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}
