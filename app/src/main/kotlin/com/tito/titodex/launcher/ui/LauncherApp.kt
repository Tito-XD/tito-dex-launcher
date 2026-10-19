package com.tito.titodex.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tito.titodex.launcher.Grid
import com.tito.titodex.launcher.InputKind
import com.tito.titodex.launcher.LauncherState
import com.tito.titodex.launcher.LauncherTheme
import com.tito.titodex.launcher.Mode
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun LauncherApp(state: LauncherState, clock: () -> Calendar = { Calendar.getInstance() }, contentInsets: WindowInsets? = null) {
    val view = LocalView.current
    val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()
    val editingNow = state.mode == Mode.Edit
    SideEffect {
        activity?.window?.let { window ->
            @Suppress("DEPRECATION")
            run { window.statusBarColor = android.graphics.Color.TRANSPARENT; window.navigationBarColor = android.graphics.Color.TRANSPARENT }
            val dark = editingNow || com.tito.titodex.launcher.inkOnDark(state.settings)
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Wallpaper(state.settings.theme, state.settings.wallpaper, state.settings.effect, state.photoBitmap, Modifier.fillMaxSize())
        if (state.settings.wallpaper == com.tito.titodex.launcher.Wall.photo) {
            Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .4f), Color.Black.copy(alpha = .15f)))))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Color.Black.copy(alpha = .3f)))
        }
        if (editingNow) Box(Modifier.fillMaxSize().background(editBackdrop(state.settings.theme)))
        // Keep the desktop stable when IME opens; only sheets/dialogs consume keyboard insets.
        val safe = contentInsets ?: WindowInsets.systemBars.union(WindowInsets.displayCutout)
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(safe).testTag("safe-viewport")
            .onGloballyPositioned { state.viewportOrigin = it.positionInRoot() }) {
            val wDp = maxWidth.value
            val hDp = maxHeight.value
            val insetDensity = LocalDensity.current
            val keyboard = (WindowInsets.ime.getBottom(insetDensity) - safe.getBottom(insetDensity)).coerceAtLeast(0) / insetDensity.density
            val o = Grid.orientFor(wDp, hDp)
            SideEffect {
                state.screenDp = wDp to hDp
                if (state.orient != o) {
                    if (state.holding != null) state.cancelHold()
                    state.orient = o
                    state.selected = null
                    state.pop = null
                }
            }
            LaunchedEffect(o, state.loaded) {
                val pending = state.layout().count { it.pending }
                if (state.loaded && pending > 0) state.showToast("当前布局有 $pending 项待放置", "整理", onAction = { state.enterEdit() })
            }
            var now by remember { mutableStateOf(clock()) }
            LaunchedEffect(Unit) {
                while (true) {
                    now = clock()
                    state.refreshWeather()
                    delay(60_000L - now.get(Calendar.SECOND) * 1000L - now.get(Calendar.MILLISECOND) + 50)
                }
            }
            LaunchedEffect(state.settings.photo) { state.loadPhoto() }
            LaunchedEffect(state.input, state.mode, state.homeFocusRequest, state.settingsOpen, state.loaded, state.primaryPicker, o) {
                if (state.input != InputKind.Pad || !state.loaded || state.settingsOpen) return@LaunchedEffect
                delay(80)
                if (state.primaryPicker) { FocusTargets.request("primary:cancel"); return@LaunchedEffect }
                val current = state.focusKey
                val owned = when (state.mode) {
                    Mode.Home -> current != null && !current.startsWith("lib") && !current.startsWith("e:") && !current.startsWith("st:")
                    Mode.Edit -> current != null && (current.startsWith("e:") || current.startsWith("st:"))
                    Mode.Library -> current != null && (current.startsWith("lib") || current.startsWith("l:"))
                }
                if (owned && FocusTargets.requesters.containsKey(current)) return@LaunchedEffect
                when (state.mode) {
                    Mode.Home -> if (!FocusTargets.request("tito")) FocusTargets.request("handle")
                    Mode.Edit -> if (!FocusTargets.request("st:tito")) FocusTargets.request("e:done")
                    Mode.Library -> FocusTargets.requesters.keys.firstOrNull { it.startsWith("lib") }?.let { FocusTargets.request(it) }
                }
            }

            val look = lookOf(state.settings)
            val motion = launcherMotion(look.theme, state.settings.motion)
            CompositionLocalProvider(LocalLook provides look, LocalLauncherMotion provides motion) {
                val editing = state.mode == Mode.Edit
                val f = stageFrame(editing, state.tab, o, wDp, hDp)
                val scale = motionFloat(f.scale, motion)
                val tx = motionFloat(f.x, motion)
                val ty = motionFloat(f.y, motion)
                val density = LocalDensity.current.density
                Stage(
                    state, state.settings, o, wDp, hDp, live = true,
                    editLayout = editing && state.tab == com.tito.titodex.launcher.EditTab.layout,
                    photo = state.photoBitmap, now = now, paintWallpaper = editing,
                    modifier = Modifier.graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = scale; scaleY = scale
                        translationX = tx * density; translationY = ty * density
                        if (scale < .999f) { clip = true; shape = RoundedCornerShape((18 / scale).dp); shadowElevation = 18.dp.toPx() }
                    },
                )
                if (editing) Box(Modifier.fillMaxSize().padding(bottom = Grid.bottom(wDp).dp)) {
                    EditChrome(state, wDp, hDp - Grid.bottom(wDp), state.photoBitmap, now)
                }
                LibrarySheet(state, wDp, (hDp - keyboard - Grid.bottom(wDp)).coerceAtLeast(80f))
                SettingsScreen(state)
                LicensesScreen(state)
                Box(Modifier.fillMaxSize().imePadding()) { BottomActions(state, wDp, Modifier.align(Alignment.BottomCenter)) }
                CoachLayer(state)
                DragLayer(state)
                Box(Modifier.fillMaxWidth().height((hDp - keyboard).coerceAtLeast(80f).dp)) { PopLayer(state, wDp, (hDp - keyboard).coerceAtLeast(80f)) }
                CityDialog(state)
                WidgetPicker(state)
                PrimaryPicker(state)
                ToastLayer(state)
            }
        }
    }
}


private fun editBackdrop(t: LauncherTheme): Brush = when (t) {
    LauncherTheme.journal -> Brush.verticalGradient(listOf(Color(0xff2a3550), Color(0xff1b2338)))
    LauncherTheme.plastic -> Brush.verticalGradient(listOf(Color(0xff27333d), Color(0xff161d24)))
    LauncherTheme.flat -> Brush.verticalGradient(listOf(Color(0xff2f3438), Color(0xff1d2124)))
}
