package com.tito.titodex.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tito.titodex.launcher.LauncherState

internal enum class WeatherAction { City, Refresh, Close }

/** Small drawn icons, with independent 44dp touch / gamepad targets. */
@Composable
internal fun WeatherActionGlyph(action: WeatherAction, modifier: Modifier = Modifier) {
    val ink = LocalLook.current.sheetInk
    Canvas(modifier) {
        val u = size.minDimension / 24f
        withTransform({ translate((size.width - 24 * u) / 2, (size.height - 24 * u) / 2); scale(u, u, Offset.Zero) }) {
            val stroke = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (action) {
                WeatherAction.City -> {
                    drawPath(Path().apply {
                        moveTo(3f, 21f); lineTo(3f, 10f); lineTo(10f, 10f); lineTo(10f, 3f)
                        lineTo(19f, 3f); lineTo(19f, 21f); moveTo(2f, 21f); lineTo(22f, 21f)
                        moveTo(10f, 10f); lineTo(10f, 21f)
                    }, ink, style = stroke)
                    for (y in listOf(7f, 11f, 15f)) for (x in listOf(13f, 16f))
                        drawLine(ink, Offset(x, y), Offset(x, y + .5f), 1.5f, StrokeCap.Round)
                    for (y in listOf(14f, 17f)) drawLine(ink, Offset(6.5f, y), Offset(6.5f, y + .5f), 1.5f, StrokeCap.Round)
                }
                WeatherAction.Refresh -> {
                    drawArc(ink, 40f, 290f, false, Offset(4f, 4f), Size(16f, 16f), style = stroke)
                    drawPath(Path().apply { moveTo(16f, 8f); lineTo(21f, 8f); lineTo(21f, 3f) }, ink, style = stroke)
                }
                WeatherAction.Close -> {
                    drawLine(ink, Offset(6f, 6f), Offset(18f, 18f), 1.8f, StrokeCap.Round)
                    drawLine(ink, Offset(18f, 6f), Offset(6f, 18f), 1.8f, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
internal fun WeatherCityControl(state: LauncherState) {
    val city = state.settings.city
    val label = if (city == null) "选择城市" else "更换城市"
    val changeCity = { state.pop = null; state.cityDialog = true }
    Row(Modifier.height(44.dp).testTag("weather-city-control")
        .padTarget(state, "p:c", label, 8f, changeCity)
        .pointerInput(changeCity) { detectTapGestures { changeCity() } },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        WeatherActionGlyph(WeatherAction.City, Modifier.size(19.dp))
        Text(city?.name ?: label, style = text(12.sp, FontWeight.Bold, LocalLook.current.sheetInk),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun WeatherIconButton(state: LauncherState, action: WeatherAction, onClick: () -> Unit) {
    val label = if (action == WeatherAction.Refresh) "刷新天气" else "关闭天气卡片"
    val key = if (action == WeatherAction.Refresh) "p:r" else "p:o"
    Box(Modifier.size(44.dp).testTag("weather-${action.name.lowercase()}")
        .padTarget(state, key, label, 12f, onClick)
        .background(LocalLook.current.sheetInk.copy(alpha = .055f), RoundedCornerShape(LauncherCorners.control))
        .pointerInput(onClick) { detectTapGestures { onClick() } }, contentAlignment = Alignment.Center) {
        WeatherActionGlyph(action, Modifier.size(21.dp))
    }
}
