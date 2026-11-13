package com.loom.build.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 统一 24×24 网格、1.75 线宽的线稿图标；与 Loom 同一套规矩。 */
enum class Ico {
    Folder, Box, Gear, Check, Link, Download, Play, Alert, External, Trash, Refresh, ArrowUp,
}

@Composable
fun Icon(ico: Ico, tint: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val sw = 1.75f * u
        val cap = StrokeCap.Round
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), sw, cap)
        }
        fun path(build: Path.() -> Unit) {
            val p = Path()
            p.build()
            drawPath(p, tint, style = Stroke(width = sw, cap = cap))
        }

        when (ico) {
            Ico.Folder -> path {
                moveTo(3f * u, 7f * u)
                lineTo(9f * u, 7f * u)
                lineTo(11f * u, 9f * u)
                lineTo(21f * u, 9f * u)
                lineTo(21f * u, 19f * u)
                lineTo(3f * u, 19f * u)
                close()
            }

            Ico.Box -> {
                path {
                    moveTo(12f * u, 3f * u)
                    lineTo(21f * u, 8f * u)
                    lineTo(12f * u, 13f * u)
                    lineTo(3f * u, 8f * u)
                    close()
                }
                line(3f, 8f, 3f, 16f)
                line(21f, 8f, 21f, 16f)
                line(12f, 13f, 12f, 21f)
                line(3f, 16f, 12f, 21f)
                line(21f, 16f, 12f, 21f)
            }

            Ico.Gear -> {
                drawCircle(tint, 3.4f * u, Offset(12f * u, 12f * u), style = Stroke(sw))
                line(12f, 3f, 12f, 6f)
                line(12f, 18f, 12f, 21f)
                line(3f, 12f, 6f, 12f)
                line(18f, 12f, 21f, 12f)
                line(5.6f, 5.6f, 7.7f, 7.7f)
                line(16.3f, 16.3f, 18.4f, 18.4f)
                line(18.4f, 5.6f, 16.3f, 7.7f)
                line(7.7f, 16.3f, 5.6f, 18.4f)
            }

            Ico.Check -> path {
                moveTo(5f * u, 13f * u)
                lineTo(10f * u, 18f * u)
                lineTo(19f * u, 6f * u)
            }

            Ico.Link -> {
                drawCircle(tint, 3.6f * u, Offset(9f * u, 15f * u), style = Stroke(sw))
                drawCircle(tint, 3.6f * u, Offset(15f * u, 9f * u), style = Stroke(sw))
                line(9.6f, 11.4f, 14.4f, 12.6f)
            }

            Ico.Download -> {
                line(12f, 4f, 12f, 15f)
                path {
                    moveTo(8f * u, 11f * u)
                    lineTo(12f * u, 15f * u)
                    lineTo(16f * u, 11f * u)
                }
                line(5f, 19f, 19f, 19f)
            }

            Ico.Play -> path {
                moveTo(8f * u, 5f * u)
                lineTo(19f * u, 12f * u)
                lineTo(8f * u, 19f * u)
                close()
            }

            Ico.Alert -> {
                path {
                    moveTo(12f * u, 4f * u)
                    lineTo(21f * u, 20f * u)
                    lineTo(3f * u, 20f * u)
                    close()
                }
                line(12f, 10f, 12f, 14f)
                drawCircle(tint, 0.9f * u, Offset(12f * u, 17f * u))
            }

            Ico.External -> {
                path {
                    moveTo(14f * u, 4f * u)
                    lineTo(20f * u, 4f * u)
                    lineTo(20f * u, 10f * u)
                }
                line(20f, 4f, 12f, 12f)
                path {
                    moveTo(18f * u, 14f * u)
                    lineTo(18f * u, 20f * u)
                    lineTo(4f * u, 20f * u)
                    lineTo(4f * u, 6f * u)
                    lineTo(10f * u, 6f * u)
                }
            }

            Ico.Trash -> {
                line(4f, 7f, 20f, 7f)
                line(10f, 7f, 10f, 4f)
                line(14f, 7f, 14f, 4f)
                line(10f, 4f, 14f, 4f)
                path {
                    moveTo(6f * u, 7f * u)
                    lineTo(7f * u, 20f * u)
                    lineTo(17f * u, 20f * u)
                    lineTo(18f * u, 7f * u)
                }
            }

            Ico.Refresh -> {
                drawArc(
                    color = tint,
                    startAngle = 40f,
                    sweepAngle = 280f,
                    useCenter = false,
                    topLeft = Offset(4f * u, 4f * u),
                    size = Size(16f * u, 16f * u),
                    style = Stroke(sw, cap = cap),
                )
                path {
                    moveTo(17f * u, 2.5f * u)
                    lineTo(20f * u, 6.5f * u)
                    lineTo(15.5f * u, 7f * u)
                }
            }

            Ico.ArrowUp -> {
                line(12f, 20f, 12f, 5f)
                path {
                    moveTo(6f * u, 11f * u)
                    lineTo(12f * u, 5f * u)
                    lineTo(18f * u, 11f * u)
                }
            }
        }
    }
}
