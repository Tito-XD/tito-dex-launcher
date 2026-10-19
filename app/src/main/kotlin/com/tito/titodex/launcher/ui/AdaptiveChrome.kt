package com.tito.titodex.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tito.titodex.launcher.*

/** Preserve the original full controller legends inside the reserved action lane. */
@Composable
fun BottomActions(state: LauncherState, widthDp: Float, modifier: Modifier = Modifier) {
    if (state.settingsOpen || state.licenses || state.primaryPicker) return
    val narrow = widthDp < 460f
    Box(modifier.fillMaxWidth().height(Grid.bottom(widthDp).dp).testTag("bottom-actions")) {
        if (state.mode == Mode.Home) LibraryHandle(state, true,
            Modifier.align(if (narrow) Alignment.TopCenter else Alignment.BottomCenter))
        LegacyHints(state)
    }
}

@Composable
private fun LegacyHints(state: LauncherState) {
    val h = state.settings.hints
    if (!(h == Hints.always || (h == Hints.auto && state.input == InputKind.Pad)) || state.settingsOpen) return
    @Composable
    fun b(key: String, label: String, btn: PadButton?) {
        Row(
            Modifier.height(26.dp).pointerInput(btn) { detectTapGestures { if (btn != null) state.pad(btn) } }.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.heightIn(min = 18.dp).widthIn(min = 18.dp).background(Cream, RoundedCornerShape(9.dp)).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                Text(key, style = text(10.sp, FontWeight.ExtraBold, NavyDeep))
            }
            Text(label, style = text(11.5.sp, FontWeight.ExtraBold, Cream))
        }
    }
    Box(Modifier.fillMaxSize().testTag("controller-hints")) {
        val bar = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(30.dp)
        when (state.mode) {
            Mode.Edit -> Row(bar.horizontalScroll(rememberScrollState()).background(Color(0xb808101c)), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (state.holding != null) { b("A", "放下", null); b("B", "取消", PadButton.B); Text("方向键 移动", style = text(11.5.sp, FontWeight.ExtraBold, Cream)) }
                else { b("A", "拿起", null); b("X", "尺寸", PadButton.X); b("Y", "隐藏", PadButton.Y); b("L/R", "切换", PadButton.R1); b("⊟", "撤销", PadButton.Select); b("B", "完成", PadButton.B) }
            }
            Mode.Library -> Row(bar.horizontalScroll(rememberScrollState()).background(Color(0xb808101c)), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                b("A", "打开", null); b("X", "选项", PadButton.X); b("Y", "搜索", PadButton.Y); b("L/R", "跳字母", PadButton.R1); b("B", "返回", PadButton.B)
            }
            Mode.Home -> Row(bar.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.background(Color(0x9e08101c), RoundedCornerShape(8.dp))) { b("A", "打开", null); b("X", "选项", PadButton.X) }
                Row(Modifier.background(Color(0x9e08101c), RoundedCornerShape(8.dp))) { b("Y", "搜索", PadButton.Y); b("☰", "整理", PadButton.Start); b("⊟", "设置", PadButton.Select) }
            }
        }
    }
}

@Composable
private fun LibraryHandle(state: LauncherState, live: Boolean, modifier: Modifier) {
    val look = LocalLook.current
    val shape = if (look.journal) RoundedCornerShape(11.dp, 11.dp, 0.dp, 0.dp) else RoundedCornerShape(15.dp)
    val (bg, fg) = when {
        look.journal -> Paper.copy(alpha = .94f) to JournalInk
        look.plastic -> Cream.copy(alpha = .9f) to PlasticInk
        else -> Navy to Cream
    }
    Box(modifier.width(140.dp).height(44.dp).pointerInput(Unit) { detectTapGestures { state.openLibrary() } }.then(if (live) Modifier.padTarget(state, "handle", "应用库", 14f) { state.openLibrary() } else Modifier), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.padding(bottom = if (look.journal) 0.dp else 5.dp).background(bg, shape)
                .then(if (look.journal) Modifier.border(1.25.dp, JournalLine, shape) else if (look.plastic) Modifier.border(1.dp, Color.White.copy(alpha = .7f), shape) else Modifier)
                .padding(horizontal = 16.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(10.dp, 6.dp)) {
                drawLine(fg, Offset(0f, size.height), Offset(size.width / 2, 0f), 2.dp.toPx(), StrokeCap.Round)
                drawLine(fg, Offset(size.width / 2, 0f), Offset(size.width, size.height), 2.dp.toPx(), StrokeCap.Round)
            }
            Spacer(Modifier.width(5.dp))
            Text("应用库", style = text(12.sp, FontWeight.ExtraBold, fg))
        }
    }
}

@Composable
fun PrimaryBadge(state: LauncherState, diameter: Int) {
    if (state.usesTitoEmblem) { TitoEmblem(diameter); return }
    val look = LocalLook.current
    Box(Modifier.size(diameter.dp).background(if (look.flat) Yellow else Paper.copy(alpha = .88f), CircleShape)
        .border(if (look.flat) 0.dp else 1.5.dp, if (look.plastic) Color.White else JournalLine, CircleShape),
        contentAlignment = Alignment.Center) {
        val icon = state.primaryEntry?.let { state.device.icon(it) }
        if (icon != null) Image(icon, null, Modifier.size((diameter * .65f).dp))
        else Text(if (state.settings.primaryApp == null) "＋" else "?", style = text((diameter * .4f).sp, color = Navy))
    }
}

@Composable
fun PrimaryPicker(state: LauncherState) {
    if (!state.primaryPicker) return
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    DisposableEffect(Unit) { onDispose { focus.clearFocus(); keyboard?.hide() } }
    var query by remember { mutableStateOf("") }
    val apps = filterApps(state.apps, query)
    Column(Modifier.fillMaxSize().imePadding().background(Paper).pointerInput(Unit) { detectTapGestures { } }.padding(16.dp).testTag("primary-picker")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("选择主应用", style = text(20.sp, FontWeight.ExtraBold, Navy), modifier = Modifier.weight(1f))
            Chip(state, "primary:cancel", "取消") { state.primaryPicker = false }
        }
        Text("保留桌面位置，换成你最常打开的应用。", style = text(12.sp, color = Navy), modifier = Modifier.padding(vertical = 12.dp))
        Field(query, { query = it }, 80, Modifier.fillMaxWidth(), hint = "搜索应用")
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            items(apps, key = { it.id }) { app ->
                Row(Modifier.fillMaxWidth().height(58.dp)
                    .padTarget(state, "primary:${app.id}", "设为主应用：${app.label}", 12f) { state.choosePrimary(app) }
                    .pointerInput(app.id) { detectTapGestures { state.choosePrimary(app) } }
                    .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    state.device.icon(app)?.let { Image(it, null, Modifier.size(36.dp)) }
                    Text(app.label, style = text(15.sp, color = Navy), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 12.dp))
                    if (state.primaryEntry?.id == app.id) Text("✓", style = text(18.sp, color = Navy))
                }
            }
            if (apps.isEmpty()) item { Text(state.appsError ?: "没有匹配的应用", style = text(14.sp, color = Navy), modifier = Modifier.padding(20.dp)) }
        }
    }
}
