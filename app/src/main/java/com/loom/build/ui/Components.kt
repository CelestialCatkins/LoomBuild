package com.loom.build.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loom.build.ui.theme.LoomBuild
import com.loom.build.ui.theme.R
import com.loom.build.ui.theme.Sp
import com.loom.build.ui.theme.Type

enum class Tone { Neutral, Accent, Ok, Warn, Danger }

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    val c = LoomBuild.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.hairline),
    )
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    val c = LoomBuild.colors
    Text(text, style = Type.eyebrow, color = c.muted, modifier = modifier)
}

/** 主按钮：墨块。 */
@Composable
fun InkButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = LoomBuild.colors
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(c.ink, RoundedCornerShape(R.r3))
            .clickable(enabled = enabled) { onClick() }
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.title, color = c.inkInverse)
    }
}

/** 次按钮：描边，不争主次。 */
@Composable
fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = LoomBuild.colors
    Box(
        modifier
            .heightIn(min = 48.dp)
            .border(1.dp, c.hairStrong, RoundedCornerShape(R.r3))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = Sp.x5, vertical = Sp.x3)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.title, color = c.ink)
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    mono: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val c = LoomBuild.colors
    val style: TextStyle = if (mono) Type.mono.copy(fontSize = 13.sp) else Type.body
    Column(modifier) {
        Eyebrow(label)
        Spacer(Modifier.height(Sp.x2))
        Box(
            Modifier
                .fillMaxWidth()
                .background(c.surfaceAlt, RoundedCornerShape(R.r2))
                .border(1.dp, c.hairline, RoundedCornerShape(R.r2))
                .padding(horizontal = Sp.x4, vertical = Sp.x3),
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, style = style, color = c.faint)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = style.copy(color = c.ink),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LoomBuild.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(R.r4))
            .border(1.dp, c.hairline, RoundedCornerShape(R.r4))
            .padding(Sp.x5),
        content = content,
    )
}

@Composable
fun SectionHead(index: String, title: String, trailing: (@Composable () -> Unit)? = null) {
    val c = LoomBuild.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(index, style = Type.eyebrow, color = c.accent)
        Spacer(Modifier.width(Sp.x2))
        Text(title, style = Type.h2, color = c.ink)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Chip(text: String, tone: Tone = Tone.Neutral) {
    val c = LoomBuild.colors
    val bg = when (tone) {
        Tone.Accent -> c.accentSoft
        Tone.Ok -> c.okSoft
        Tone.Warn -> c.warnSoft
        Tone.Danger -> c.dangerSoft
        Tone.Neutral -> c.surfaceAlt
    }
    val fg = when (tone) {
        Tone.Accent -> c.accent
        Tone.Ok -> c.ok
        Tone.Warn -> c.warn
        Tone.Danger -> c.danger
        Tone.Neutral -> c.muted
    }
    Box(
        Modifier
            .background(bg, RoundedCornerShape(R.pill))
            .padding(horizontal = Sp.x3, vertical = 5.dp),
    ) {
        Text(text, style = Type.mono, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 进度条：发线轨道 + 铜色填充。 */
@Composable
fun Meter(progress: Float, modifier: Modifier = Modifier) {
    val c = LoomBuild.colors
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(c.hairline),
    ) {
        Box(
            Modifier
                .fillMaxWidth(p)
                .height(3.dp)
                .background(c.accent),
        )
    }
}

@Composable
fun KeyValue(key: String, value: String, valueColor: Color? = null) {
    val c = LoomBuild.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Sp.x2),
        verticalAlignment = Alignment.Top,
    ) {
        Text(key, style = Type.micro, color = c.faint, modifier = Modifier.width(92.dp))
        Text(
            value,
            style = Type.mono,
            color = valueColor ?: c.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) {
    val c = LoomBuild.colors
    Text(text, style = Type.small, color = c.muted, modifier = modifier)
}

@Composable
fun DangerText(text: String, modifier: Modifier = Modifier) {
    val c = LoomBuild.colors
    Text(text, style = Type.small, color = c.danger, modifier = modifier)
}
