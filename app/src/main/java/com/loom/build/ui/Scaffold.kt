package com.loom.build.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loom.build.ui.theme.LoomBuild
import com.loom.build.ui.theme.Sp
import com.loom.build.ui.theme.Type

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LoomBuild.colors
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(eyebrow)
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(Sp.x3))
        Text(title, style = Type.display, color = c.ink)
        if (subtitle != null) {
            Spacer(Modifier.height(Sp.x2))
            Text(subtitle, style = Type.small, color = c.muted)
        }
        Spacer(Modifier.height(Sp.x5))
        Hairline()
    }
}

/** 底部导航：五项太高，此应用只需三项。激活项 = 铜色 2dp 指示线 + 墨色。 */
@Composable
fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val c = LoomBuild.colors
    Column {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.paper)
                .height(62.dp),
        ) {
            NavItem(0, "项目", Ico.Folder, selected, onSelect)
            NavItem(1, "构建", Ico.Play, selected, onSelect)
            NavItem(2, "设置", Ico.Gear, selected, onSelect)
        }
    }
}

@Composable
private fun RowScope.NavItem(
    index: Int,
    label: String,
    icon: Ico,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val c = LoomBuild.colors
    val active = index == selected
    Column(
        Modifier
            .weight(1f)
            .height(62.dp)
            .clickable { onSelect(index) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .width(18.dp)
                .height(2.dp)
                .background(if (active) c.accent else c.paper),
        )
        Spacer(Modifier.height(7.dp))
        Icon(icon, if (active) c.ink else c.faint, 19.dp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = Type.eyebrow, color = if (active) c.ink else c.faint)
    }
}
