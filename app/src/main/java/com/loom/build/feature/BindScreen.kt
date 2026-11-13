package com.loom.build.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loom.build.data.BuildStore
import com.loom.build.ui.CardBox
import com.loom.build.ui.DangerText
import com.loom.build.ui.Eyebrow
import com.loom.build.ui.Field
import com.loom.build.ui.Hairline
import com.loom.build.ui.InkButton
import com.loom.build.ui.Muted
import com.loom.build.ui.OutlineButton
import com.loom.build.ui.SectionHead
import com.loom.build.ui.theme.LoomBuild
import com.loom.build.ui.theme.Sp
import com.loom.build.ui.theme.Type
import kotlinx.coroutines.launch

private const val TOKEN_URL =
    "https://github.com/settings/tokens/new?scopes=repo,workflow&description=Loom%20Build"

@Composable
fun BindScreen(store: BuildStore, onOpenUrl: (String) -> Unit) {
    val c = LoomBuild.colors
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sp.page),
    ) {
        Spacer(Modifier.height(Sp.x8))
        Eyebrow("LOOM BUILD / 绑定")
        Spacer(Modifier.height(Sp.x3))
        Text("先借 GitHub 的手。", style = Type.display, color = c.ink)
        Spacer(Modifier.height(Sp.x3))
        Text(
            "这个应用不在手机上编译。它把你的项目推到 GitHub，由云端构建，再把装好的 APK 送回手机。",
            style = Type.body,
            color = c.muted,
        )
        Spacer(Modifier.height(Sp.x7))

        SectionHead("01", "创建令牌")
        Spacer(Modifier.height(Sp.x3))
        Muted("令牌相当于一把钥匙。只勾选两个权限即可：")
        Spacer(Modifier.height(Sp.x2))
        Text("repo（读写仓库）  ·  workflow（修改构建流水线）", style = Type.mono, color = c.ink)
        Spacer(Modifier.height(Sp.x4))
        OutlineButton("打开 GitHub 令牌页", onClick = { onOpenUrl(TOKEN_URL) })

        Spacer(Modifier.height(Sp.x7))
        SectionHead("02", "粘贴进来")
        Spacer(Modifier.height(Sp.x4))
        Field(
            label = "访问令牌",
            value = input,
            onValueChange = { input = it },
            placeholder = "ghp_… 或 github_pat_…",
            mono = true,
        )
        Spacer(Modifier.height(Sp.x4))
        InkButton(
            text = if (store.busy) "正在验证…" else "验证并绑定",
            enabled = input.isNotBlank() && !store.busy,
        ) {
            scope.launch { store.bind(input) }
        }

        if (store.error.isNotBlank()) {
            Spacer(Modifier.height(Sp.x4))
            DangerText(store.error)
        }

        Spacer(Modifier.height(Sp.x7))
        Hairline()
        Spacer(Modifier.height(Sp.x4))
        CardBox {
            Text("令牌存在哪里", style = Type.h2, color = c.ink)
            Spacer(Modifier.height(Sp.x2))
            Muted("只写入应用私有目录，未 root 的其他应用读不到。随时可在设置里解绑，GitHub 侧也能单独吊销。")
        }
        Spacer(Modifier.height(Sp.x8))
    }
}
