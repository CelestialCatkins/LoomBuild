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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loom.build.data.BuildStore
import com.loom.build.ui.CardBox
import com.loom.build.ui.Hairline
import com.loom.build.ui.KeyValue
import com.loom.build.ui.Muted
import com.loom.build.ui.OutlineButton
import com.loom.build.ui.PageHeader
import com.loom.build.ui.SectionHead
import com.loom.build.ui.theme.LoomBuild
import com.loom.build.ui.theme.Sp
import com.loom.build.ui.theme.Type

@Composable
fun SettingsScreen(store: BuildStore, onOpenUrl: (String) -> Unit) {
    val c = LoomBuild.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sp.page),
    ) {
        Spacer(Modifier.height(Sp.x6))
        PageHeader(
            eyebrow = "03 / 设置",
            title = "账号与边界",
            subtitle = "这里不保存任何项目代码，只保存一把令牌。",
        )
        Spacer(Modifier.height(Sp.x6))

        SectionHead("01", "GitHub 账号")
        Spacer(Modifier.height(Sp.x3))
        CardBox {
            KeyValue("登录身份", store.login.ifBlank { "—" })
            Hairline()
            KeyValue("默认仓库", store.repoName.ifBlank { "—" })
            Hairline()
            KeyValue("令牌", if (store.bound) "••••••••  已保存" else "未绑定")
        }
        Spacer(Modifier.height(Sp.x4))
        if (store.bound) {
            OutlineButton("解绑 GitHub") { store.unbind() }
        }

        Spacer(Modifier.height(Sp.x7))
        SectionHead("02", "快捷入口")
        Spacer(Modifier.height(Sp.x3))
        if (store.login.isNotBlank()) {
            OutlineButton("打开我的仓库") {
                onOpenUrl("https://github.com/${store.login}")
            }
            Spacer(Modifier.height(Sp.x3))
            OutlineButton("查看 Actions 运行") {
                onOpenUrl("https://github.com/${store.login}/${store.repoName}/actions")
            }
        } else {
            Muted("绑定后可用")
        }

        Spacer(Modifier.height(Sp.x7))
        Hairline()
        Spacer(Modifier.height(Sp.x5))
        CardBox {
            Text("它是怎么工作的", style = Type.h2, color = c.ink)
            Spacer(Modifier.height(Sp.x2))
            Muted(
                "选目录 → 读取文件 → 通过 GitHub Git Data API 写入新仓库 → " +
                    "注入 build-apk.yml → 触发 workflow_dispatch → 轮询运行状态 → " +
                    "下载 artifact → 交给系统安装器。",
            )
            Spacer(Modifier.height(Sp.x3))
            Text("v1.0.0 · compileSdk 34 · Kotlin 2.0.21", style = Type.mono, color = c.faint)
        }
        Spacer(Modifier.height(Sp.x8))
    }
}
