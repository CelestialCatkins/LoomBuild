package com.loom.build.feature

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loom.build.data.BuildStore
import com.loom.build.data.RepoRef
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
import com.loom.build.ui.theme.R
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
            subtitle = "本地目录会上传后构建；已有仓库直接触发流水线。",
        )
        Spacer(Modifier.height(Sp.x5))

        ModeSwitch(store.mode) { store.selectMode(it) }
        Spacer(Modifier.height(Sp.x6))

        if (store.mode == 0) {
            LocalSection(store) { picker.launch(null) }
        } else {
            ExistingSection(store)
        }

        Spacer(Modifier.height(Sp.x6))

        if (store.error.isNotBlank()) {
            DangerText(store.error)
            Spacer(Modifier.height(Sp.x3))
        }

        if (store.mode == 0) {
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
            Muted("提交到 ${store.login.ifBlank { "你的账号" }}/${store.repoName.ifBlank { "仓库" }}，并自动注入构建流水线。")
        } else {
            InkButton(
                text = if (store.busy) "处理中…" else "构建这个仓库",
                enabled = store.existingRepo.contains("/") && !store.busy,
            ) {
                onStarted()
                scope.launch { store.startExisting() }
            }
            Spacer(Modifier.height(Sp.x3))
            Muted("只触发云端构建，不会上传任何本地文件。首次会为该仓库注入 build-apk.yml。")
        }
        Spacer(Modifier.height(Sp.x8))
    }
}

@Composable
private fun ModeSwitch(selected: Int, onSelect: (Int) -> Unit) {
    val c = LoomBuild.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surfaceAlt, RoundedCornerShape(R.r3))
            .padding(4.dp),
    ) {
        Segment("本地项目", selected == 0, Modifier.weight(1f)) { onSelect(0) }
        Segment("已有仓库", selected == 1, Modifier.weight(1f)) { onSelect(1) }
    }
}

@Composable
private fun RowScope.Segment(
    label: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = LoomBuild.colors
    Box(
        modifier
            .height(40.dp)
            .background(if (active) c.surface else Color.Transparent, RoundedCornerShape(R.r2))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Type.title, color = if (active) c.ink else c.muted)
    }
}

@Composable
private fun LocalSection(store: BuildStore, onPick: () -> Unit) {
    val c = LoomBuild.colors
    SectionHead("02", "本地项目")
    Spacer(Modifier.height(Sp.x3))
    CardBox {
        Text(
            if (store.folderName.isBlank()) "尚未选择文件夹" else store.folderName,
            style = Type.h2,
            color = if (store.folderName.isBlank()) c.faint else c.ink,
        )
        Spacer(Modifier.height(Sp.x2))
        Muted(
            if (store.scanNote.isBlank()) {
                "选择 Android Studio 项目的根目录（含 settings.gradle.kts）"
            } else {
                store.scanNote
            },
        )
        Spacer(Modifier.height(Sp.x4))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip(
                text = if (store.gradleOk) "GRADLE ✓" else "GRADLE ✗",
                tone = if (store.gradleOk) Tone.Ok else Tone.Danger,
            )
            Spacer(Modifier.weight(1f))
            OutlineButton("选择文件夹", onClick = onPick)
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
}

@Composable
private fun ExistingSection(store: BuildStore) {
    val c = LoomBuild.colors
    val scope = rememberCoroutineScope()

    SectionHead("02", "GitHub 仓库")
    Spacer(Modifier.height(Sp.x4))
    Field(
        label = "仓库",
        value = store.existingRepo,
        onValueChange = { store.updateExistingRepo(it) },
        placeholder = "owner/name",
        mono = true,
    )
    Spacer(Modifier.height(Sp.x4))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlineButton(
            text = if (store.reposLoading) "读取中…" else "载入我的仓库",
            enabled = !store.reposLoading,
        ) {
            scope.launch { store.loadRepos() }
        }
        Spacer(Modifier.weight(1f))
        if (store.repos.isNotEmpty()) {
            Chip(text = "${store.repos.size} 个", tone = Tone.Neutral)
        }
    }

    if (store.repos.isNotEmpty()) {
        Spacer(Modifier.height(Sp.x4))
        CardBox {
            val shown = store.repos.take(12)
            shown.forEachIndexed { index, repo ->
                RepoRow(repo, selected = store.existingRepo == repo.fullName) {
                    store.updateExistingRepo(repo.fullName)
                }
                if (index != shown.lastIndex) Hairline()
            }
            if (store.repos.size > shown.size) {
                Spacer(Modifier.height(Sp.x2))
                Muted("仅显示最近更新的 12 个")
            }
        }
    }
    Spacer(Modifier.height(Sp.x2))
    Muted("仓库需要可访问且未归档；分支使用其默认分支。")
}

@Composable
private fun RepoRow(repo: RepoRef, selected: Boolean, onClick: () -> Unit) {
    val c = LoomBuild.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Sp.x3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            repo.fullName,
            style = Type.mono,
            color = if (selected) c.accent else c.ink,
            modifier = Modifier.weight(1f),
        )
        if (repo.isPrivate) {
            Chip(text = "私有", tone = Tone.Neutral)
        } else {
            Chip(text = "公开", tone = Tone.Ok)
        }
    }
}
