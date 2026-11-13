package com.loom.build.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loom.build.data.ApkInstaller
import com.loom.build.data.BuildStore
import com.loom.build.data.HistoryItem
import com.loom.build.ui.CardBox
import com.loom.build.ui.Chip
import com.loom.build.ui.DangerText
import com.loom.build.ui.Hairline
import com.loom.build.ui.InkButton
import com.loom.build.ui.Meter
import com.loom.build.ui.Muted
import com.loom.build.ui.OutlineButton
import com.loom.build.ui.PageHeader
import com.loom.build.ui.SectionHead
import com.loom.build.ui.Tone
import com.loom.build.ui.theme.LoomBuild
import com.loom.build.ui.theme.Phase
import com.loom.build.ui.theme.Sp
import com.loom.build.ui.theme.Type
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Step(val label: String, val hint: String)

private val STEPS = listOf(
    Step("读取项目", "扫描文件或读取远端仓库"),
    Step("上传 / 触发", "写入 blob 或直接 dispatch"),
    Step("提交并注入流水线", "commit + build-apk.yml"),
    Step("云端构建", "GitHub Actions 编译并签名"),
    Step("取回产物", "下载 artifact 并校验"),
)

private fun stageOf(phase: Phase): Int = when (phase) {
    Phase.IDLE -> -1
    Phase.SCANNING -> 0
    Phase.UPLOADING -> 1
    Phase.COMMITTING -> 2
    Phase.QUEUED, Phase.RUNNING -> 3
    Phase.DOWNLOADING -> 4
    Phase.DONE -> 5
    Phase.ERROR -> -2
}

@Composable
fun BuildScreen(store: BuildStore, onOpenUrl: (String) -> Unit) {
    val c = LoomBuild.colors
    val context = LocalContext.current
    val stage = stageOf(store.phase)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sp.page),
    ) {
        Spacer(Modifier.height(Sp.x6))
        PageHeader(
            eyebrow = "02 / 构建",
            title = store.headline,
            subtitle = if (store.detail.isBlank()) "上传后会自动开始，通常 3–6 分钟。" else store.detail,
        )
        Spacer(Modifier.height(Sp.x5))

        Meter(store.progress)
        Spacer(Modifier.height(Sp.x3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip(
                text = when (store.phase) {
                    Phase.IDLE -> "待机"
                    Phase.DONE -> "成功"
                    Phase.ERROR -> "失败"
                    Phase.RUNNING -> "进行中"
                    else -> "进行中"
                },
                tone = when (store.phase) {
                    Phase.DONE -> Tone.Ok
                    Phase.ERROR -> Tone.Danger
                    Phase.IDLE -> Tone.Neutral
                    else -> Tone.Accent
                },
            )
            Spacer(Modifier.width(Sp.x2))
            if (store.runUrl.isNotBlank()) {
                Chip(text = "RUN", tone = Tone.Neutral)
            }
            Spacer(Modifier.weight(1f))
            Text("${(store.progress * 100).toInt()}%", style = Type.mono, color = c.muted)
        }

        Spacer(Modifier.height(Sp.x6))
        Hairline()
        Spacer(Modifier.height(Sp.x5))

        STEPS.forEachIndexed { index, step ->
            val state = when {
                store.phase == Phase.ERROR && index == stage.coerceAtLeast(0) -> 2
                stage > index -> 1
                stage == index -> 2
                else -> 0
            }
            StepRow(index + 1, step, state)
            if (index != STEPS.lastIndex) Spacer(Modifier.height(Sp.x3))
        }

        if (store.error.isNotBlank()) {
            Spacer(Modifier.height(Sp.x5))
            DangerText(store.error)
        }

        Spacer(Modifier.height(Sp.x6))

        if (store.runUrl.isNotBlank()) {
            OutlineButton("查看云端日志", onClick = { onOpenUrl(store.runUrl) })
            Spacer(Modifier.height(Sp.x3))
        }

        val apk = store.apkFile
        if (apk != null) {
            CardBox {
                Text("产物已就绪", style = Type.h2, color = c.ink)
                Spacer(Modifier.height(Sp.x2))
                Text(store.apkLabel, style = Type.mono, color = c.accent)
                Spacer(Modifier.height(Sp.x2))
                Muted(BuildStore.formatSize(apk.length()) + " · 由云端签名，可直接安装")
                Spacer(Modifier.height(Sp.x4))
                InkButton("安装 APK") { ApkInstaller.install(context, apk) }
            }
        } else {
            Muted("构建完成后，这里会出现安装按钮。")
        }

        if (store.history.isNotEmpty()) {
            Spacer(Modifier.height(Sp.x7))
            Hairline()
            Spacer(Modifier.height(Sp.x5))
            SectionHead("04", "构建历史")
            Spacer(Modifier.height(Sp.x3))
            CardBox {
                val shown = store.history.take(10)
                shown.forEachIndexed { index, item ->
                    HistoryRow(
                        item = item,
                        onOpen = { url -> if (url.isNotBlank()) onOpenUrl(url) },
                        onInstall = { file -> ApkInstaller.install(context, file) },
                    )
                    if (index != shown.lastIndex) Hairline()
                }
            }
            Spacer(Modifier.height(Sp.x3))
            OutlineButton("清空历史") { store.clearHistory() }
        }

        Spacer(Modifier.height(Sp.x8))
    }
}

@Composable
private fun StepRow(index: Int, step: Step, state: Int) {
    val c = LoomBuild.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            index.toString().padStart(2, '0'),
            style = Type.mono,
            color = if (state == 0) c.faint else c.accent,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                step.label,
                style = Type.title,
                color = if (state == 0) c.faint else c.ink,
            )
            Text(step.hint, style = Type.micro, color = c.faint)
        }
        Spacer(Modifier.width(Sp.x3))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            Chip(
                text = when (state) {
                    1 -> "完成"
                    2 -> "进行"
                    else -> "等待"
                },
                tone = when (state) {
                    1 -> Tone.Ok
                    2 -> Tone.Accent
                    else -> Tone.Neutral
                },
            )
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onOpen: (String) -> Unit,
    onInstall: (File) -> Unit,
) {
    val c = LoomBuild.colors
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val ok = item.conclusion == "success"
    val apk = item.apk
    val file = if (apk.isNotBlank()) File(apk) else null
    val canInstall = ok && file != null && file.exists()

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(item.url) }
            .padding(vertical = Sp.x3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.repo.ifBlank { "—" },
                style = Type.mono,
                color = c.ink,
            )
            Text(
                "${fmt.format(Date(item.at))} · ${item.conclusion.ifBlank { "unknown" }}",
                style = Type.micro,
                color = c.faint,
            )
        }
        if (canInstall && file != null) {
            Box(
                Modifier
                    .clickable { onInstall(file) }
                    .padding(horizontal = Sp.x3, vertical = Sp.x2),
            ) {
                Text("安装", style = Type.micro, color = c.accent)
            }
        }
        Chip(
            text = if (ok) "成功" else "失败",
            tone = if (ok) Tone.Ok else Tone.Danger,
        )
    }
}

@Suppress("unused")
private val unusedColor: Color? = null
