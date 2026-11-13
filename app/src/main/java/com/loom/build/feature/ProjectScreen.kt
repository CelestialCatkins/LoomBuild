package com.loom.build.feature

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loom.build.data.BuildStore
import com.loom.build.ui.CardBox
import com.loom.build.ui.Chip
import com.loom.build.ui.DangerText
import com.loom.build.ui.Field
import com.loom.build.ui.Hairline
import com.loom.build.ui.InkButton
import com.loom.build.ui.Muted
import com.loom.build.ui.OutlineButton
import com.loom.build.ui.PageHeader
import com.loom.build.ui.SectionHead
import com.loom.build.ui.Tone
import com.loom.build.ui.theme.LoomBuild
import com.loom.build.ui.theme.Sp
import com.loom.build.ui.theme.Type
import kotlinx.coroutines.launch

@Composable
fun ProjectScreen(store: BuildStore, onStarted: () -> Unit) {
    val c = LoomBuild.colors
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { store.pickFolder(uri) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sp.page),
    ) {
        Spacer(Modifier.height(Sp.x6))
        PageHeader(
            eyebrow = "01 / 项目",
            title = "选一个项目，交出去。",
            subtitle = "只读取你选中的目录，不申请全盘权限。",
        )
        Spacer(Modifier.height(Sp.x6))

        SectionHead("02", "本地项目")
        Spacer(Modifier.height(Sp.x3))

        CardBox {
            Text(
                if (store.folderName.isBlank()) "尚未选择文件夹" else store.folderName,
                style = Type.h2,
                color = if (store.folderName.isBlank()) c.faint else c.ink,
            )
            Spacer(Modifier.height(Sp.x2))
            Muted(if (store.scanNote.isBlank()) "选择 Android Studio 项目的根目录（含 settings.gradle.kts）" else store.scanNote)
            Spacer(Modifier.height(Sp.x4))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(
                    text = if (store.gradleOk) "GRADLE ✓" else "GRADLE ✗",
                    tone = if (store.gradleOk) Tone.Ok else Tone.Danger,
                )
                Spacer(Modifier.weight(1f))
                OutlineButton("选择文件夹") { picker.launch(null) }
            }
        }

        Spacer(Modifier.height(Sp.x6))
        Hairline()
        Spacer(Modifier.height(Sp.x6))

        SectionHead("03", "目标仓库")
        Spacer(Modifier.height(Sp.x4))
        Field(
            label = "仓库名",
            value = store.repoName,
            onValueChange = { store.updateRepoName(it) },
            placeholder = "例如 my-loom-app",
            mono = true,
        )
        Spacer(Modifier.height(Sp.x4))
        CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("私有仓库", style = Type.title, color = c.ink)
                    Muted("私有仓库的构建分钟数会计入你的额度。")
                }
                Switch(checked = store.isPrivate, onCheckedChange = { store.setIsPrivate(it) })
            }
        }

        Spacer(Modifier.height(Sp.x6))

        if (store.error.isNotBlank()) {
            DangerText(store.error)
            Spacer(Modifier.height(Sp.x3))
        }

        InkButton(
            text = when {
                store.busy -> "处理中…"
                !store.gradleOk -> "先选择一个 Gradle 项目"
                else -> "上传并构建"
            },
            enabled = store.gradleOk && !store.busy && store.repoName.isNotBlank(),
        ) {
            onStarted()
            scope.launch { store.start() }
        }

        Spacer(Modifier.height(Sp.x3))
        Muted("上传后会提交到 ${store.login.ifBlank { "你的账号" }}/${store.repoName.ifBlank { "仓库" }}，并自动注入构建流水线。")
        Spacer(Modifier.height(Sp.x8))
    }
}
