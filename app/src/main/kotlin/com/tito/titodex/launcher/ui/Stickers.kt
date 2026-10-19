package com.tito.titodex.launcher.ui

import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.em
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tito.titodex.launcher.AppEntry
import com.tito.titodex.launcher.LauncherState
import com.tito.titodex.launcher.Pop
import com.tito.titodex.launcher.Settings
import com.tito.titodex.launcher.ShelfItem
import com.tito.titodex.launcher.WeatherReading
import com.tito.titodex.launcher.clockParts
import com.tito.titodex.launcher.dateLabel
import com.tito.titodex.launcher.greeting
import java.util.Calendar
import kotlin.math.min
import kotlin.math.roundToInt

// ------------------------------------------------------------------ clock

@Composable
fun ClockSticker(state: LauncherState, s: Settings, size: String, now: Calendar, weather: WeatherReading?, live: Boolean) {
    val look = LocalLook.current
    val (hh, mm) = clockParts(now, s.use24Hour)
    val density = LocalDensity.current
    val timeMeasurer = rememberTextMeasurer()
    // Reserve space for readable metadata first. Only the clock numerals adapt
    // to the available cell; never scale the date/weather as a rendered group.
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
        val banner = size == "w"
        val preferred = when { banner -> 74f; size == "l" -> 64f; else -> 50f }
        val numberSize = min(preferred, ((maxHeight.value - if (banner) 8f else 38f) / density.fontScale).coerceAtLeast(24f))
        val metaSize = 14.sp
        val timeStyle = text(numberSize.sp, if (look.journal) FontWeight.Bold else FontWeight.ExtraBold, look.bgInk,
            (if (look.flat) -3.5 else -2.0).sp).copy(
            lineHeight = (numberSize * .95f).sp,
            shadow = if (look.plastic) Shadow(Color(0x400a1422), Offset(0f, 4f), 0f) else null,
        )
        if (size == "v") {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                BoxWithConstraints(Modifier.weight(1f, fill = false).fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                    // A single two-line paragraph shares compact baseline spacing; separate
                    // Text nodes each reserve full font ascent/descent and leave a large gap.
                    val digits = buildAnnotatedString {
                        append("$hh\n")
                        withStyle(SpanStyle(color = look.accentInk)) { append(mm) }
                    }
                    val candidate = timeStyle.copy(fontSize = 96.sp, lineHeight = 78.sp,
                        fontFeatureSettings = "tnum",
                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                        platformStyle = PlatformTextStyle(includeFontPadding = false))
                    // Reserve a stable two-digit width so the clock does not resize each minute.
                    val measured = timeMeasurer.measure("88\n88", candidate, softWrap = false, maxLines = 2).size
                    val metricSpace = with(density) { 96.sp.toPx() * .40f }
                    val fit = with(density) { minOf(1f,
                        maxWidth.toPx() / measured.width.coerceAtLeast(1),
                        maxHeight.toPx() / (measured.height - metricSpace).coerceAtLeast(1f)) } * .98f
                    // Trim only unused digit ascent/descent; let Text measure the full font
                    // box before placing its visible portion inside the available grid cell.
                    val edgeSpace = with(density) { (96f * fit).sp.toPx() * .20f }.roundToInt()
                    Text(digits, style = candidate.copy(fontSize = (96f * fit).sp, lineHeight = (78f * fit).sp),
                        maxLines = 2, softWrap = false, modifier = Modifier.layout { measurable, constraints ->
                            val textConstraints = if (constraints.hasBoundedHeight)
                                constraints.copy(maxHeight = constraints.maxHeight + edgeSpace * 2) else constraints
                            val p = measurable.measure(textConstraints)
                            layout(p.width, (p.height - edgeSpace * 2).coerceAtLeast(0)) { p.placeRelative(0, -edgeSpace) }
                        })
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    CompactDateStamp(now)
                    if (s.showWeather) WeatherChip(state, s, weather, 16.sp, live)
                }
            }
        } else {
            // Always one line for time, one shared line for date and weather.
            // Narrow screens only tighten decoration, never change this hierarchy.
            val horizontalTime = @Composable { modifier: Modifier ->
                BoxWithConstraints(modifier, contentAlignment = Alignment.BottomCenter) {
                    val candidate = timeStyle.copy(fontSize = 100.sp, lineHeight = 100.sp,
                        letterSpacing = (if (look.flat) -.035f else -.02f).em,
                        fontFeatureSettings = "tnum",
                        platformStyle = PlatformTextStyle(includeFontPadding = false))
                    val measured = timeMeasurer.measure("88:88", candidate, softWrap = false).size
                    val ink = textInk("88:88", candidate)
                    val fit = with(density) { minOf(1f,
                        maxWidth.toPx() / measured.width.coerceAtLeast(1),
                        maxHeight.toPx() / ink.height().coerceAtLeast(1)) } * .98f
                    OpticalText(buildAnnotatedString { append("$hh:"); withStyle(SpanStyle(color = look.accentInk)) { append(mm) } },
                        style = candidate.copy(fontSize = (100f * fit).sp, lineHeight = (100f * fit).sp),
                        modifier = Modifier.testTag("clock-time"))
                }
            }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                horizontalTime(Modifier.weight(1f).fillMaxWidth())
                Row(Modifier.fillMaxWidth().heightIn(min = 24.dp).testTag("clock-metadata"),
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically) {
                    CompactDateStamp(now, metaSize, wrap = false)
                    if (s.showWeather) WeatherChip(state, s, weather, metaSize, live, compact = true)
                }
            }
        }
    }
}

@Composable
private fun CompactDateStamp(now: Calendar, size: androidx.compose.ui.unit.TextUnit = 16.sp, wrap: Boolean = true) {
    val look = LocalLook.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val date = dateLabel(now).substringBefore(' ')
    val weekday = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")[now.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]
    val dateStyle = text(size, FontWeight.ExtraBold, look.bgInk)
    val weekdayStyle = dateStyle.copy(
        drawStyle = Stroke(width = with(density) { (.8f * size.value / 16f).dp.toPx() }, join = StrokeJoin.Round),
    )
    val dateText = @Composable { modifier: Modifier ->
        Text(date, style = dateStyle, maxLines = 1, softWrap = false, modifier = modifier)
    }
    val weekdayText = @Composable { modifier: Modifier ->
        Text(weekday, style = weekdayStyle, maxLines = 1, softWrap = false,
            modifier = modifier.padding(horizontal = 1.dp).testTag("clock-weekday").semantics {
                contentDescription = dateLabel(now).substringAfter(' ')
            })
    }
    BoxWithConstraints(if (wrap) Modifier.fillMaxWidth() else Modifier) {
        val needed = measurer.measure(date, dateStyle, softWrap = false).size.width +
            measurer.measure(weekday, weekdayStyle, softWrap = false).size.width + with(density) { 7.dp.toPx() }
        // Keep both labels readable at accessibility font sizes; only wrap if
        // the real cell cannot hold the row without shrinking either label.
        if (!wrap || needed <= with(density) { maxWidth.toPx() }) {
            Row(Modifier.heightIn(min = if (wrap) 28.dp else 24.dp), horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                dateText(Modifier.alignByBaseline()); weekdayText(Modifier.alignByBaseline())
            }
        } else {
            Column { dateText(Modifier); weekdayText(Modifier) }
        }
    }
}

@Composable
private fun WeatherChip(state: LauncherState, s: Settings, weather: WeatherReading?, size: androidx.compose.ui.unit.TextUnit, live: Boolean, compact: Boolean = false) {
    val look = LocalLook.current
    val city = s.city
    val label = when {
        city == null -> if (compact) "天气" else "＋ 天气"
        weather == null -> if (state.weatherError != null) "—" else "…"
        else -> "${weather.temperature.roundToInt()}°"
    }
    val (bg, fg, shape) = when {
        look.journal -> Triple(Paper, JournalInk, RoundedCornerShape(5.dp, 17.dp, 17.dp, 5.dp))
        look.plastic -> Triple(Cream.copy(alpha = .9f), PlasticInk, RoundedCornerShape(10.dp))
        else -> Triple(Color(0xffafc7da), FlatInk, RoundedCornerShape(17.dp))
    }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = text(size, FontWeight.ExtraBold, fg).copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
    BoxWithConstraints {
        val labelWidth = with(density) { measurer.measure(label, style, softWrap = false).size.width.toDp() }
        val showIcon = city != null && weather != null
        val fullWidth = labelWidth + if (showIcon) 38.dp else 20.dp
        val narrow = compact || fullWidth > maxWidth
        // The smallest 1x2 cell needs room for the degree sign as well as the icon.
        val tight = narrow && showIcon && labelWidth + 24.dp > maxWidth
        val displayLabel = if (city == null && narrow) "天气" else label
        Row(
            (if (live) Modifier.padTarget(state, "wx", "天气") { state.pop = Pop.Weather(state.rects["wx"] ?: androidx.compose.ui.geometry.Rect.Zero) } else Modifier)
                .testTag("clock-weather")
                .background(bg, shape)
                .then(if (look.journal) Modifier.border(1.25.dp, JournalLine, shape) else if (look.plastic) Modifier.border(1.dp, Color.White.copy(alpha = .7f), shape) else Modifier)
                .padding(horizontal = if (tight) 2.dp else if (narrow) 4.dp else 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (tight) 2.dp else 4.dp),
        ) {
            if (showIcon) Canvas(Modifier.size(if (narrow) 12.dp else 14.dp).testTag("weather-icon")) { drawCloud(fg) }
            Text(displayLabel, style = style, maxLines = 1, softWrap = false)
        }
    }
}

private fun DrawScope.drawCloud(c: Color) {
    val w = size.width
    drawCircle(c, w * .22f, Offset(w * .33f, w * .58f))
    drawCircle(c, w * .28f, Offset(w * .55f, w * .45f))
    drawCircle(c, w * .2f, Offset(w * .75f, w * .6f))
    drawRect(c, Offset(w * .3f, w * .55f), Size(w * .5f, w * .25f))
}

// ------------------------------------------------------------------ trainer

@Composable
fun TrainerSticker(state: LauncherState, s: Settings, size: String, hour: Int, live: Boolean) {
    val look = LocalLook.current
    val initial = s.name.codePoints().findFirst().let { if (it.isPresent) String(Character.toChars(it.asInt)) else "T" }
    val density = LocalDensity.current
    val avatar = @Composable { d: androidx.compose.ui.unit.Dp ->
        val (bg, fg) = when {
            look.journal -> Color(0xfff5e7b6) to Color(0xff93674f)
            look.plastic -> Yellow to PlasticInk
            else -> Navy to Yellow
        }
        // An inset version of the plate's corners remains visibly rectangular.
        val shape = if (s.avatarShape == com.tito.titodex.launcher.AvatarShape.circle) CircleShape
            else RoundedCornerShape(if (look.flat) 12.dp else 10.dp)
        Box(
            Modifier.size(d).testTag("trainer-avatar").clip(shape)
                .background(if (look.plastic) Brush.linearGradient(listOf(Color(0xffffd778), Color(0xffffac78))) else Brush.linearGradient(listOf(bg, bg)))
                .then(if (look.journal) Modifier.border(2.dp, Paper, shape).padding(2.dp).border(1.dp, Color(0xffb18168), shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) { OpticalText(initial, style = text((d.value * .44f).sp, FontWeight.ExtraBold, fg)) }
    }
    val identity = @Composable { availableHeight: androidx.compose.ui.unit.Dp, caption: Boolean ->
        val title = text(if (caption) 22.sp else 20.sp, FontWeight.ExtraBold, look.ink)
        val greetingStyleBase = text(14.sp, FontWeight.Bold, look.ink.copy(alpha = .8f))
        val label = text(8.5.sp, FontWeight.ExtraBold, look.ink.copy(alpha = .6f), 1.6.sp)
        val titleInk = textInk(s.name, title)
        val greetingInk = textInk(greeting(hour), greetingStyleBase)
        val labelInk = textInk("PLAYER CARD", label)
        val gapPx = with(density) { 8.dp.toPx() }
        val captionPx = if (caption) labelInk.height() + with(density) { 5.dp.toPx() } else 0f
        val desired = titleInk.height() + greetingInk.height() + gapPx + captionPx
        val fit = minOf(1f, with(density) { availableHeight.toPx() } / desired.coerceAtLeast(1f))
        val titleStyle = title.copy(fontSize = title.fontSize * fit)
        val greetingStyle = greetingStyleBase.copy(fontSize = greetingStyleBase.fontSize * fit)
        val labelStyle = label.copy(fontSize = label.fontSize * fit)
        val heightPx = textInk(s.name, titleStyle).height() + textInk(greeting(hour), greetingStyle).height() + gapPx * fit +
            (if (caption) textInk("PLAYER CARD", labelStyle).height() + with(density) { 5.dp.toPx() } * fit else 0f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            avatar(with(density) { heightPx.toDp() })
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).testTag("trainer-identity")) {
                if (caption) {
                    OpticalText("PLAYER CARD", labelStyle)
                    Spacer(Modifier.height((5f * fit).dp))
                }
                OpticalText(s.name, titleStyle, Modifier.testTag("trainer-name"))
                Spacer(Modifier.height((8f * fit).dp))
                OpticalText(greeting(hour), greetingStyle, Modifier.testTag("trainer-greeting"))
            }
        }
    }
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .then(if (look.journal) Modifier.rotate(-1.4f) else Modifier)
            .then(if (live) Modifier.padTarget(state, "trainer", "个人名片") { state.pop = Pop.Trainer(state.rects["trainer"] ?: androidx.compose.ui.geometry.Rect.Zero) } else Modifier)
            .plate(look, PlateKind.Trainer),
    ) {
        val cardHeight = maxHeight
        when (size) {
            "s" -> Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                avatar(minOf(46f, cardHeight.value - 40).coerceAtLeast(24f).dp)
                Spacer(Modifier.height(4.dp))
                OpticalText(s.name, text(13.sp, FontWeight.ExtraBold, look.ink))
            }
            "l" -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                identity((cardHeight - 68.dp).coerceAtLeast(36.dp), true)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().drawBehind {
                    drawLine(look.ink.copy(alpha = .3f), Offset.Zero, Offset(this.size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                }.padding(top = 7.dp)) {
                    Text(s.motto.ifEmpty { "写一句今天的冒险宣言" }, style = text(12.sp, FontWeight.Normal, look.ink).copy(lineHeight = 17.sp))
                }
            }
            else -> Box(Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                identity((cardHeight - 20.dp).coerceAtLeast(24.dp), false)
            }
        }
        if (look.journal) Canvas(Modifier.align(Alignment.TopCenter).offset(y = (-9).dp).size(62.dp, 18.dp).rotate(-4f)) {
            drawRect(Color(0xb8d8d5b1))
        }
    }
}

// ------------------------------------------------------------------ titodex

/** A dedicated Launcher emblem: the App's split sphere, expressed in each theme's material. */
@Composable
fun TitoEmblem(d: Int) {
    val look = LocalLook.current
    Canvas(Modifier.size(d.dp)) {
        // Proportional geometry keeps the complete rim/shadow inside both the home and menu sizes.
        val u = size.minDimension
        val c = Offset(size.width / 2, u * .46f)
        val rim = u * .46f
        val r = u * .405f
        val blue = when {
            look.journal -> Color(0xff6c96b5)
            look.plastic -> Color(0xff4a90e2)
            else -> Color(0xff4e97e8)
        }
        val dark = if (look.flat) NavyDeep else Navy
        val light = if (look.journal) Paper else Cream
        when {
            look.journal -> {
                drawCircle(Color(0x70354557), rim, c + Offset(u * .018f, u * .055f))
                drawCircle(Paper, rim, c)
                drawCircle(JournalLine, rim - u * .007f, c, style = Stroke(u * .014f))
                drawCircle(JournalLine.copy(alpha = .75f), u * .435f, c,
                    style = Stroke(u * .009f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u * .025f, u * .025f))))
            }
            look.plastic -> {
                drawCircle(Color(0x49304055), rim - u * .008f, c + Offset(0f, u * .07f))
                drawCircle(Brush.verticalGradient(listOf(Color.White, Color(0xffb6c7d9)), 0f, u), rim, c)
                drawCircle(Color.White.copy(alpha = .85f), rim - u * .01f, c, style = Stroke(u * .014f))
                drawCircle(Color(0xff263c58), r + u * .018f, c)
            }
            else -> {
                drawCircle(Cream, rim, c)
                drawCircle(NavyDeep, rim - u * .01f, c, style = Stroke(u * .02f))
            }
        }
        val sphere = Path().apply { addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
        clipPath(sphere) {
            if (look.plastic) {
                drawCircle(Brush.radialGradient(listOf(Color(0xff57718f), Color(0xff1c2c43)),
                    center = c + Offset(-r * .4f, -r * .3f), radius = r * 1.7f), r, c)
                drawRect(Brush.verticalGradient(listOf(Color(0xff9cc9f0), blue, Color(0xff2862a3)),
                    c.y - r, c.y + r), Offset(c.x - r, c.y - r), Size(r * 2, r))
            } else {
                drawCircle(dark, r, c)
                drawRect(blue, Offset(c.x - r, c.y - r), Size(r * 2, r))
            }
            // The white equator and ring retain TitoDex's identity without copying an app tile.
            drawRect(light, Offset(c.x - r, c.y - u * .036f), Size(r * 2, u * .072f))
            if (look.plastic) {
                drawArc(Color.White.copy(alpha = .5f), 207f, 82f, false,
                    Offset(c.x - r * .79f, c.y - r * .79f), Size(r * 1.58f, r * 1.58f),
                    style = Stroke(u * .018f))
            }
        }
        if (look.journal) drawCircle(Navy.copy(alpha = .6f), r, c, style = Stroke(u * .01f))
        if (look.plastic) drawCircle(Color(0x800c1e35), u * .155f, c + Offset(0f, u * .025f))
        drawCircle(light, u * .145f, c)
        if (look.plastic) {
            drawCircle(Brush.radialGradient(listOf(Color(0xffb9def7), blue, Color(0xff346ba6)),
                center = c + Offset(-u * .035f, -u * .04f), radius = u * .17f), u * .092f, c)
        } else {
            drawCircle(if (look.flat) Coral else blue, u * .09f, c)
            if (look.journal) drawCircle(Navy.copy(alpha = .7f), u * .09f, c, style = Stroke(u * .012f))
        }
    }
}

@Composable
fun TitoSticker(state: LauncherState, size: String, live: Boolean) {
    val look = LocalLook.current
    val title = state.primaryTitle
    val m = if (live) Modifier.padTarget(state, "tito", "打开 $title", cornerDp = 24f) { state.launchTitoDex() } else Modifier
    BoxWithConstraints(m.fillMaxSize()) {
        val diameter = minOf(if (size == "m") 60f else 76f, maxHeight.value - 24f, maxWidth.value - 8f).coerceAtLeast(24f).toInt()
        if (size == "m") Row(Modifier.fillMaxSize().plate(look, PlateKind.Tito).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryBadge(state, diameter)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = text(17.sp, FontWeight.ExtraBold, look.ink), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("主应用", style = text(12.sp, color = look.ink.copy(alpha = .75f)))
            }
        } else Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            PrimaryBadge(state, diameter)
            Spacer(Modifier.height(3.dp))
            Text(title, style = text(13.sp, FontWeight.ExtraBold, look.bgInk), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

// ------------------------------------------------------------------ shelf

@Composable
fun AppIcon(icon: ImageBitmap?, label: String, d: Int = 50) {
    val look = LocalLook.current
    Box(
        Modifier.size(d.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Image(icon, label, Modifier.fillMaxSize().then(if (look.plastic) Modifier.iconMaterialLight() else Modifier))
        else Box(Modifier.fillMaxSize().background(Navy, if (look.flat) CircleShape else RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Text(label.take(1), style = text((d * .4f).sp, FontWeight.ExtraBold, Cream))
        }
    }
}

/** Light belongs to the app's existing silhouette, including transparent holes.
 * Offscreen isolation prevents SrcAtop from painting a rectangle on the shelf.
 * This is a small cached static pass: no sweep, glow outside the icon or blur. */
private fun Modifier.iconMaterialLight(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithCache {
    val light = Brush.linearGradient(
        0f to Color.White.copy(alpha = .16f),
        .42f to Color.White.copy(alpha = .025f),
        .65f to Color.Transparent,
        1f to NavyDeep.copy(alpha = .055f),
    )
    onDrawWithContent {
        drawContent()
        drawRect(light, blendMode = BlendMode.SrcAtop)
    }
}

@Composable
fun IconTile(
    icon: ImageBitmap?, label: String, ink: Color, modifier: Modifier = Modifier, width: Int = 74, height: Int = 84,
    badge: String? = null, letter: String? = null, star: Boolean = false, check: Boolean? = null, lifted: Boolean = false, insertBefore: Boolean = false, insertAfter: Boolean = false,
) {
    BoxWithConstraints(modifier.size(width.dp, height.dp).drawBehind {
        val bar = 3.dp.toPx()
        if (insertBefore) drawRoundRect(Coral, Offset(-bar / 2, 12.dp.toPx()), Size(bar, size.height - 30.dp.toPx()), CornerRadius(2.dp.toPx()))
        if (insertAfter) drawRoundRect(Coral, Offset(size.width - bar / 2, 12.dp.toPx()), Size(bar, size.height - 30.dp.toPx()), CornerRadius(2.dp.toPx()))
    }) {
        val iconSize = minOf(50, (maxHeight.value - 28).toInt()).coerceAtLeast(24)
        Column(
            Modifier.fillMaxSize().alpha(if (lifted) .25f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            AppIcon(icon, label, iconSize)
            Spacer(Modifier.height(5.dp))
            Text(label, style = text(11.sp, color = ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
        }
        if (badge != null) Text(badge, style = text(8.5.sp, FontWeight.ExtraBold, Color(0xfffff7e6)), modifier = Modifier.align(Alignment.TopEnd).offset((-9).dp, 6.dp).background(Color(0xc72f4361), RoundedCornerShape(6.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
        if (letter != null) Text(letter, style = text(10.5.sp, FontWeight.ExtraBold, Cream), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.TopStart).offset(5.dp, 3.dp).background(Navy.copy(alpha = .82f), RoundedCornerShape(6.dp)).padding(horizontal = 4.dp))
        if (star) Box(Modifier.align(Alignment.TopCenter).offset(23.dp, 6.dp).size(16.dp).background(Coral, CircleShape).border(2.dp, Paper, CircleShape), contentAlignment = Alignment.Center) {
            Text("★", style = text(9.sp, color = Color.White))
        }
        if (check != null) Box(
            Modifier.align(Alignment.TopCenter).offset(24.dp, 4.dp).size(21.dp).background(if (check) Navy else Color.White, CircleShape).border(2.dp, if (check) Navy else Navy.copy(alpha = .35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (check) Text("✓", style = text(12.sp, FontWeight.ExtraBold, Color.White)) }
    }
}

@Composable
fun DashedTile(glyph: String, label: String, ink: Color, modifier: Modifier) {
    BoxWithConstraints(modifier.size(74.dp, 84.dp)) {
        val iconSize = minOf(50f, maxHeight.value - 28f).coerceAtLeast(24f)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(iconSize.dp).drawBehind {
                drawRoundRect(ink, cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(1.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
            }, contentAlignment = Alignment.Center) { Text(glyph, style = text(18.sp, FontWeight.ExtraBold, ink)) }
            Spacer(Modifier.height(4.dp))
            Text(label, style = text(11.sp, color = ink), maxLines = 1)
        }
    }
}

@Composable
fun ShelfSticker(state: LauncherState, items: List<ShelfItem>, size: String, live: Boolean) {
    val look = LocalLook.current
    val density = LocalDensity.current
    val drag = state.iconDrag
    Box(
        Modifier.fillMaxSize().plate(look, PlateKind.Shelf)
            .trackedBounds(state, "shelfbox", live)
            .then(if (look.journal) Modifier.drawBehind {
                val inset = 5.dp.toPx()
                drawRoundRect(Color(0x8c7c8999), Offset(inset, inset), Size(this.size.width - inset * 2, this.size.height - inset * 2), CornerRadius(11.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))))
            } else Modifier)
            .padding(6.dp)
            .clipToBounds(),
    ) {
        val pinnedIds = items.filter { !it.auto }.map { it.app.id }
        val tile = @Composable { item: ShelfItem, index: Int ->
            val key = "shelf:${item.app.id}"
            val pinIndex = pinnedIds.indexOf(item.app.id)
            IconTile(
                state.device.icon(item.app), item.app.label, look.ink,
                modifier = if (live) Modifier.padTarget(state, key, item.app.label, 16f) { state.launch(item.app) } else Modifier,
                badge = if (item.auto) "最近" else null,
                lifted = drag?.fromShelf == true && drag.app.id == item.app.id,
                insertBefore = drag?.insert != null && pinIndex >= 0 && drag.insert == pinIndex && drag.insert != pinnedIds.size,
                insertAfter = drag?.insert != null && pinIndex == pinnedIds.size - 1 && drag.insert == pinnedIds.size,
            )
        }
        val tail = @Composable {
            if (items.isNotEmpty()) DashedTile("⋯", "全部", look.ink, if (live) Modifier.padTarget(state, "shelf:all", "全部应用", 16f) { state.openLibrary() } else Modifier)
            else DashedTile("＋", "挑选常玩", look.ink, if (live) Modifier.padTarget(state, "shelf:pick", "挑选常玩", 16f) { state.openLibrary(pick = true) } else Modifier)
        }
        if (size == "c") {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val cols = maxOf(4, ((maxWidth.value - 10) / 74).toInt())
                val all = items.size + 1
                val rows = 2
                val perRow = maxOf(cols, (all + rows - 1) / rows)
                val cellW = maxWidth / cols
                Column(Modifier.fillMaxSize().offset { IntOffset(if (live) -state.shelfScroll.roundToInt() else 0, 0) }) {
                    for (r in 0 until rows) Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        for (c in 0 until perRow) {
                            val i = r * perRow + c
                            Box(Modifier.width(cellW), contentAlignment = Alignment.Center) {
                                when {
                                    i < items.size -> tile(items[i], i)
                                    i == items.size -> tail()
                                }
                            }
                        }
                    }
                }
                val max = with(density) { (maxWidth / cols * perRow).toPx() - maxWidth.toPx() }.coerceAtLeast(0f)
                if (live) SideEffect { state.shelfMax = max }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxSize().offset { IntOffset(-(if (live) state.shelfScroll.roundToInt() else 0), 0) }.onSizeChanged { },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { i, it -> tile(it, i) }
                    tail()
                }
                val max = with(density) { (74.dp * (items.size + 1)).toPx() - maxWidth.toPx() }.coerceAtLeast(0f)
                if (live) SideEffect { state.shelfMax = max }
            }
        }
    }
}

// ------------------------------------------------------------------ widget

@Composable
fun WidgetSticker(state: LauncherState, appWidgetId: Int, live: Boolean, label: String) {
    val look = LocalLook.current
    val shape = RoundedCornerShape(16.dp)
    if (!live) {
        Box(Modifier.fillMaxSize().plate(look), contentAlignment = Alignment.Center) {
            Text(label, style = text(12.sp, color = look.ink), maxLines = 2, textAlign = TextAlign.Center)
        }
        return
    }
    val bridge = state.device.widgets
    val widgetDensity = LocalDensity.current.density
    Box(Modifier.fillMaxSize().clip(shape)) {
        if (bridge == null) return@Box
        AndroidView(
            factory = { ctx ->
                val v = bridge.view(appWidgetId) { state.enterEdit(sel = "${com.tito.titodex.launcher.Sticker.WIDGET_PREFIX}$appWidgetId") }
                    ?: android.widget.TextView(ctx).apply { text = "小组件不可用"; setTextColor(android.graphics.Color.WHITE) }
                (v.parent as? ViewGroup)?.removeView(v)
                v
            },
            modifier = Modifier.fillMaxSize().onSizeChanged { dimensions ->
                bridge.resize(appWidgetId, (dimensions.width / widgetDensity).toInt(), (dimensions.height / widgetDensity).toInt())
            },
        )
    }
}
