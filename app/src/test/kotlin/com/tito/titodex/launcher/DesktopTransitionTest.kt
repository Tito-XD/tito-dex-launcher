package com.tito.titodex.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.tito.titodex.launcher.ui.*
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalTestApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w480dp-h480dp-hdpi")
class DesktopTransitionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun mutate(action: () -> Unit) = rule.runOnIdle { action(); Snapshot.sendApplyNotifications() }
    private fun advance(ms: Long = 32) {
        rule.runOnIdle { Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }
    private fun capture(name: String): Bitmap {
        lateinit var bitmap: Bitmap
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val out = File("build/validation/motion/$name.png")
            out.parentFile!!.mkdirs()
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return bitmap
    }
    private fun launch(theme: LauncherTheme = LauncherTheme.plastic): LauncherState {
        val device = FakeDevice(Settings(theme = theme, coachSeen = true, hints = Hints.always,
            city = City("测试城市", 31.0, 121.0), pinned = listOf("com.a/A", "com.b/B", "com.retro/R")).encode())
        device.icons = plasticPreviewIcons()
        lateinit var state: LauncherState
        rule.setContent {
            val scope = rememberCoroutineScope()
            state = remember { LauncherState(device, NoWeather, scope, Dispatchers.Unconfined) }
            LaunchedEffect(Unit) { state.start(); state.weather = WeatherReading(26.0, 3, 0L) }
            LauncherApp(state, clock = { Calendar.getInstance().apply { set(2026, 9, 19, 10, 8, 0) } },
                contentInsets = WindowInsets(left = 0, top = 36, right = 0, bottom = 48))
        }
        rule.waitUntil(5_000) { state.loaded && state.appsLoaded }
        return state
    }

    @Test fun savedThemeLoadingDoesNotConsumeEntranceAndInterruptNeverReplays() {
        val ready = mutableStateOf(false)
        val interrupted = mutableStateOf(false)
        val theme = mutableStateOf(LauncherTheme.journal)
        val enabled = mutableStateOf(true)
        var value = -1f
        rule.setContent {
            val p = desktopEntrance(ready.value, interrupted.value, LauncherMotion(theme.value, enabled.value))
            SideEffect { value = p }
        }
        rule.mainClock.autoAdvance = false
        mutate { theme.value = LauncherTheme.plastic }
        advance(400)
        assertEquals(0f, value, 0f)
        mutate { ready.value = true }
        advance(112)
        assertTrue("saved theme must still get its first entrance", value > 0 && value < 1)
        assertTrue(entranceFraction(value, 0, LauncherMotion(theme.value, true)) > entranceFraction(value, 3, LauncherMotion(theme.value, true)))
        mutate { interrupted.value = true }
        advance()
        assertEquals(1f, value, 0f)
        mutate { interrupted.value = false; theme.value = LauncherTheme.flat; ready.value = false }
        advance()
        mutate { ready.value = true }
        advance()
        assertEquals(1f, value, 0f)
    }

    @Test fun disablingMotionDuringEntranceSettlesImmediately() {
        val enabled = mutableStateOf(true)
        var value = -1f
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val p = desktopEntrance(true, false, LauncherMotion(LauncherTheme.journal, enabled.value))
            SideEffect { value = p }
        }
        advance(96)
        assertTrue(value < 1)
        mutate { enabled.value = false }
        advance()
        assertEquals(1f, value, 0f)
        mutate { enabled.value = true }
        advance()
        assertEquals(1f, value, 0f)
    }

    @Test fun templateSizeAndPositionInterpolateAndUndoWinsMidFlight() {
        val state = launch()
        rule.waitForIdle()
        mutate { state.enterEdit() }
        rule.waitForIdle()
        val beforeSettings = state.settings
        val before = state.rects.getValue("st:tito")
        rule.mainClock.autoAdvance = false
        mutate { state.applyTemplate(Template.panel) }
        advance(80)
        val middle = state.rects.getValue("st:tito")
        assertTrue(middle.width > before.width)
        assertTrue(middle.top < before.top)
        capture("layout-middle").recycle()
        mutate { state.undo() }
        advance(800)
        assertEquals(beforeSettings, state.settings)
        val end = state.rects.getValue("st:tito")
        assertEquals(before.width, end.width, 1f)
        assertEquals(before.top, end.top, 1f)
    }

    @Test fun restoringCardUsesTrayOriginAndUndoCannotLeaveGhostOrChangeSettingsLater() {
        val state = launch()
        mutate { state.enterEdit(); state.applyTemplate(Template.quiet) }
        rule.waitForIdle()
        val before = state.settings
        val origin = state.rects.getValue("e:show:trainer")
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("＋ 个人名片").performClick()
        advance(80)
        assertEquals(origin, state.moduleArrival?.origin)
        assertFalse(state.layout().first { it.id == "trainer" }.hidden)
        capture("card-middle").recycle()
        mutate { state.undo() }
        advance(800)
        assertEquals(before, state.settings)
        assertNull(state.moduleArrival)
        assertFalse(FocusTargets.requesters.containsKey("st:trainer"))
    }

    @Test fun sharedFocusSlidesBetweenLiveTargetsAndReducedMotionSnaps() {
        val device = FakeDevice()
        lateinit var state: LauncherState
        val rail = FocusRail()
        val enabled = mutableStateOf(true)
        var activated: String? = null
        rule.setContent {
            val scope = rememberCoroutineScope()
            state = remember { LauncherState(device, NoWeather, scope, Dispatchers.Unconfined).apply {
                update { it.copy(coachSeen = true) }; input = InputKind.Pad
            } }
            CompositionLocalProvider(LocalLauncherMotion provides LauncherMotion(LauncherTheme.plastic, enabled.value),
                LocalFocusRail provides rail) {
                Box(Modifier.fillMaxSize()) {
                    Row(Modifier.padding(40.dp), horizontalArrangement = Arrangement.spacedBy(80.dp)) {
                        Box(Modifier.size(60.dp).padTarget(state, "tito") { activated = "tito" })
                        Box(Modifier.size(100.dp).padTarget(state, "trainer") { activated = "trainer" })
                    }
                    SlidingFocus(state, rail)
                }
            }
        }
        mutate { FocusTargets.request("tito") }
        rule.waitForIdle()
        capture("focus-first").recycle()
        val first = rail.painted!!
        rule.mainClock.autoAdvance = false
        mutate { FocusTargets.request("trainer") }
        advance(64)
        capture("focus-middle").recycle()
        val middle = rail.painted!!
        val destination = state.rects.getValue("trainer")
        assertTrue(middle.left > first.left && middle.left < destination.left)
        assertTrue(middle.width > first.width && middle.width < destination.width)
        rule.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals("logical focus must activate immediately while the outline is moving", "trainer", activated)
        mutate { enabled.value = false }
        advance()
        capture("focus-settled").recycle()
        assertEquals(destination, rail.painted)
        mutate { state.input = InputKind.Touch }
        advance()
        assertNull(rail.painted)
    }

    @Test fun rapidWallpaperAndEffectChangesKeepIntermediateFramesAndLatestStateWins() {
        val state = launch()
        rule.waitForIdle()
        val baseline = capture("wall-baseline")
        val original = baseline.getPixel(350, 350)
        baseline.recycle()
        rule.mainClock.autoAdvance = false
        mutate { state.setEffect(Effect.dim) }
        advance(48)
        val partial = capture("effect-middle")
        val middle = partial.getPixel(350, 350)
        partial.recycle()
        advance(500)
        val dark = capture("effect-end")
        assertNotEquals(original, middle)
        assertNotEquals(dark.getPixel(350, 350), middle)
        dark.recycle()
        mutate { state.setWall(Wall.coast); state.setEffect(Effect.scanlines) }
        advance(48)
        mutate { state.setWall(Wall.forest); state.setEffect(Effect.clear) }
        advance(48)
        mutate { state.setWall(Wall.none); state.update { it.copy(motion = false) } }
        advance()
        val settled = capture("wall-reduced")
        assertEquals(original, settled.getPixel(350, 350))
        settled.recycle()
        mutate { state.update { it.copy(motion = true) } }
        advance(800)
        val final = capture("wall-latest")
        assertEquals(original, final.getPixel(350, 350))
        final.recycle()
    }

    @Test fun renderContinuousDesktopTransitions() {
        rule.mainClock.autoAdvance = false
        val state = launch()
        for (i in 0..18) { advance(); capture("entrance-${i.toString().padStart(2, '0')}").recycle() }
        val before = state.settings
        mutate { state.setWall(Wall.dusk); state.setEffect(Effect.dim) }
        val old = capture("wall-start")
        for (i in 0..12) { advance(); capture("wall-${i.toString().padStart(2, '0')}").recycle() }
        val final = capture("wall-end")
        assertNotEquals(old.getPixel(350, 350), final.getPixel(350, 350))
        old.recycle(); final.recycle()
        mutate { state.enterEdit(); state.applyTemplate(Template.quiet) }
        advance(800)
        mutate { state.show("trainer") }
        for (i in 0..12) { advance(); capture("card-${i.toString().padStart(2, '0')}").recycle() }
        assertNull(state.moduleArrival)
        mutate { state.applyTemplate(Template.panel) }
        for (i in 0..18) { advance(); capture("layout-${i.toString().padStart(2, '0')}").recycle() }
        mutate { state.finishEdit(); state.update { before }; state.input = InputKind.Pad }
        advance(800)
        mutate { FocusTargets.request("tito") }
        advance(500)
        // Robolectric's offscreen window only paints this source frame when
        // explicitly drawn; the on-device window paints it every display frame.
        capture("pad-00").recycle()
        mutate { FocusTargets.request("shelf:com.a/A") }
        for (i in 1..13) { advance(); capture("pad-${i.toString().padStart(2, '0')}").recycle() }
    }
}
