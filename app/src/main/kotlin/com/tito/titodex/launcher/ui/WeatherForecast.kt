package com.tito.titodex.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tito.titodex.launcher.WeatherReading
import com.tito.titodex.launcher.weatherDescription
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Condition artwork shared by the desktop chip and the forecast; no network image assets. */
@Composable
internal fun WeatherGlyph(code: Int, modifier: Modifier = Modifier, monochrome: Color? = null) {
    val look = LocalLook.current
    val cloudColor = monochrome ?: if (look.journal) Color(0xff819dac) else Color(0xff719ab7)
    val sunColor = monochrome ?: if (look.journal) Color(0xffbe8758) else Color(0xffeeb650)
    val rainColor = monochrome ?: Color(0xff5086ae)
    Canvas(modifier.semantics { contentDescription = weatherDescription(code) }) {
        val u = size.minDimension
        withTransform({ translate((size.width - u) / 2, (size.height - u) / 2); scale(u, u, Offset.Zero) }) {
            when (code) {
                0 -> sun(sunColor, Offset(.5f, .5f), .4f)
                1, 2 -> {
                    sun(sunColor, Offset(.35f, .35f), .24f)
                    cloud(cloudColor, Offset(.55f, .6f), .8f)
                }
                3 -> cloud(cloudColor)
                45, 48 -> {
                    cloud(cloudColor, Offset(.5f, .35f), .7f)
                    for (y in listOf(.73f, .87f)) drawLine(cloudColor, Offset(.2f, y), Offset(.8f, y), .05f, StrokeCap.Round)
                }
                in 51..67, in 80..82 -> {
                    cloud(cloudColor, Offset(.5f, .38f), .78f)
                    for (x in listOf(.3f, .5f, .7f)) drawLine(rainColor, Offset(x + .025f, .75f), Offset(x - .025f, .87f), .05f, StrokeCap.Round)
                }
                in 71..77, 85, 86 -> {
                    cloud(cloudColor, Offset(.5f, .38f), .78f)
                    for (x in listOf(.3f, .5f, .7f)) for (angle in listOf(0.0, PI / 3, PI * 2 / 3)) {
                        val d = Offset(cos(angle).toFloat() * .055f, sin(angle).toFloat() * .055f)
                        drawLine(rainColor, Offset(x, .84f) - d, Offset(x, .84f) + d, .025f, StrokeCap.Round)
                    }
                }
                95, 96, 99 -> {
                    cloud(cloudColor, Offset(.5f, .38f), .78f)
                    drawPath(Path().apply {
                        moveTo(.48f, .57f); lineTo(.35f, .76f); lineTo(.49f, .76f)
                        lineTo(.44f, .9f); lineTo(.65f, .68f); lineTo(.51f, .68f); close()
                    }, sunColor)
                }
                else -> {
                    drawCircle(cloudColor, .32f, Offset(.5f, .5f), style = Stroke(.04f))
                    for (x in listOf(.37f, .5f, .63f)) drawCircle(cloudColor, .025f, Offset(x, .5f))
                }
            }
        }
    }
}

private fun DrawScope.cloud(color: Color, center: Offset = Offset(.5f, .5f), scale: Float = 1f) {
    withTransform({ translate(center.x - .5f, center.y - .5f); scale(scale, scale, Offset(.5f, .5f)) }) {
        drawCircle(color, .22f, Offset(.30f, .63f))
        drawCircle(color, .32f, Offset(.50f, .47f))
        drawCircle(color, .22f, Offset(.70f, .63f))
        drawRect(color, Offset(.3f, .6f), Size(.4f, .25f))
    }
}

private fun DrawScope.sun(color: Color, center: Offset, radius: Float) {
    drawCircle(color, radius * .51f, center)
    for (i in 0..7) {
        val angle = i * PI / 4
        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        drawLine(color, center + direction * (radius * .75f), center + direction * (radius * .95f), radius * .1f, StrokeCap.Round)
    }
}

@Composable
internal fun WeatherForecast(reading: WeatherReading, city: String?) {
    val look = LocalLook.current
    val ink = look.sheetInk
    val muted = ink.copy(alpha = .6f)
    val accent = if (look.journal) Color(0xffb0745a) else Color(0xff5086ae)
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(city ?: "当前天气", style = text(12.sp, FontWeight.Bold, muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                OpticalText("${reading.temperature.roundToInt()}°", text(44.sp, FontWeight.ExtraBold, ink))
                Text(reading.description, style = text(14.sp, FontWeight.ExtraBold, ink))
            }
            WeatherGlyph(reading.code, Modifier.size(78.dp).testTag("forecast-current-icon"))
        }
        if (reading.hours.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("未来几小时", style = text(12.sp, FontWeight.ExtraBold, muted))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reading.hours.forEachIndexed { i, hour ->
                        Column(Modifier.weight(1f)
                            .background(if (i == 0) accent.copy(alpha = .09f) else Color.Transparent, RoundedCornerShape(LauncherCorners.small))
                            .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(hour.label, style = text(11.sp, FontWeight.Bold, muted), maxLines = 1)
                            WeatherGlyph(hour.code, Modifier.size(24.dp).testTag("forecast-hour-$i"))
                            OpticalText("${hour.temperature}°", text(15.sp, FontWeight.ExtraBold, ink))
                        }
                    }
                }
            }
        }
        if (reading.days.isNotEmpty()) {
            val lowest = reading.days.minOf { it.low }
            val highest = reading.days.maxOf { it.high }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("未来几天", style = text(12.sp, FontWeight.ExtraBold, muted), modifier = Modifier.weight(1f))
                    Text("低温 — 高温", style = text(10.sp, FontWeight.Bold, muted))
                }
                reading.days.forEachIndexed { i, day ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(day.label, style = text(12.sp, FontWeight.Bold, ink), modifier = Modifier.width(34.dp), maxLines = 1)
                        WeatherGlyph(day.code, Modifier.size(24.dp).testTag("forecast-day-$i"))
                        Box(Modifier.width(30.dp), contentAlignment = Alignment.CenterEnd) {
                            OpticalText("${day.low}°", text(13.sp, FontWeight.Bold, muted))
                        }
                        Canvas(Modifier.weight(1f).height(6.dp).testTag("forecast-range-$i").semantics {
                            contentDescription = "${day.label}气温范围：${day.low}到${day.high}度"
                        }) {
                            val y = size.height / 2
                            val start = if (highest == lowest) .5f else (day.low - lowest).toFloat() / (highest - lowest)
                            val end = if (highest == lowest) .5f else (day.high - lowest).toFloat() / (highest - lowest)
                            val inset = y
                            val width = (size.width - 2 * inset).coerceAtLeast(0f)
                            drawLine(ink.copy(alpha = .08f), Offset(inset, y), Offset(inset + width, y), size.height, StrokeCap.Round)
                            drawLine(Brush.horizontalGradient(listOf(accent, if (look.journal) Color(0xffcda778) else Coral)),
                                Offset(inset + start.coerceIn(0f, 1f) * width, y), Offset(inset + end.coerceIn(0f, 1f) * width, y), size.height, StrokeCap.Round)
                        }
                        OpticalText("${day.high}°", text(13.sp, FontWeight.ExtraBold, ink), Modifier.width(30.dp))
                    }
                }
            }
        }
        if (reading.hours.isEmpty() && reading.days.isEmpty())
            Text("暂时没有预报", style = text(12.sp, FontWeight.Bold, muted))
        Spacer(Modifier.height(2.dp))
    }
}
