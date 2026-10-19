package com.tito.titodex.launcher

import android.graphics.Canvas
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.tito.titodex.launcher.ui.LauncherApp
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w480dp-h480dp-hdpi")
class AdaptiveUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun launch(insets: WindowInsets = WindowInsets(left = 0, top = 36, right = 0, bottom = 48)): Pair<FakeDevice, LauncherState> {
        val device = FakeDevice(Settings(coachSeen = true, hints = Hints.always, motion = false).encode())
        lateinit var state: LauncherState
        rule.setContent {
            val scope = rememberCoroutineScope()
            state = remember { LauncherState(device, NoWeather, scope, Dispatchers.Unconfined) }
            LaunchedEffect(Unit) { state.start() }
            // Explicit system bar geometry isolates app layout from the Robolectric host window.
            LauncherApp(state, contentInsets = insets)
        }
        rule.waitUntil(5_000) { state.loaded && state.apps.isNotEmpty() }
        rule.waitForIdle()
        return device to state
    }

    private fun capture(name: String) {
        val out = File("build/validation/adaptive/$name.png")
        out.parentFile!!.mkdirs()
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun verifyChrome(state: LauncherState, name: String, expected: Orient) {
        assertEquals(expected, state.orient)
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val footer = rule.onNodeWithTag("bottom-actions").fetchSemanticsNode().boundsInRoot
        assertTrue("footer over navigation bar", footer.bottom <= root.bottom - 47)
        assertTrue("footer above status bar", footer.top > 36)
        rule.runOnIdle {
            val entry = state.rects["tito"]!!
            assertTrue("primary entry overlaps action lane", entry.bottom <= footer.top + 1)
            assertTrue("primary entry outside safe width", entry.left >= 0 && entry.right <= root.right)
        }
        if (state.screenDp.first < 460f) {
            val handle = rule.onNodeWithContentDescription("应用库").fetchSemanticsNode().boundsInRoot
            val search = rule.onNode(hasText("搜索") and hasAnyAncestor(hasTestTag("controller-hints")))
                .fetchSemanticsNode().boundsInRoot
            assertTrue("library handle overlaps full controller hints", handle.bottom <= search.top)
        }
        capture("$name-home")
        rule.runOnIdle { state.enterEdit() }
        rule.waitForIdle()
        val done = rule.onNodeWithContentDescription("完成").fetchSemanticsNode().boundsInRoot
        assertTrue("edit toolbar under status bar", done.top >= 36)
        assertTrue("edit toolbar under navigation", done.bottom < footer.top)
        capture("$name-edit")
        rule.onNodeWithContentDescription("完成").performClick()
        rule.onNodeWithContentDescription("应用库").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(Mode.Library, state.mode)
        val sheet = rule.onNodeWithTag("library-sheet").fetchSemanticsNode().boundsInRoot
        assertTrue("library overlaps action lane", sheet.bottom <= footer.top + 1)
        rule.onNodeWithText("Beta").performClick()
        rule.waitForIdle()
        assertEquals(Mode.Home, state.mode)
    }

    @Test fun squareChromeAndEditRemainInsideSystemBars() {
        val (_, state) = launch()
        verifyChrome(state, "square", Orient.square)
    }

    @Test @Config(qualifiers = "w800dp-h450dp-hdpi")
    fun wideChromeAndEditUseAdditionalColumns() {
        val (_, state) = launch()
        verifyChrome(state, "wide", Orient.wide)
    }

    @Test @Config(qualifiers = "w360dp-h800dp-hdpi")
    fun portraitChromeAndEditUseAdditionalRows() {
        val (_, state) = launch()
        verifyChrome(state, "portrait", Orient.tall)
    }

    @Test @Config(qualifiers = "w640dp-h360dp-hdpi")
    fun compactLandscapeKeepsNavigationAndEditControlsReachable() {
        val (_, state) = launch()
        verifyChrome(state, "landscape", Orient.land)
    }

    @Test @Config(qualifiers = "w800dp-h450dp-hdpi")
    fun sideNavigationInsetsAlsoApplyToPopoverAndTouchTargets() {
        val (_, state) = launch(WindowInsets(left = 48, top = 36, right = 60, bottom = 0))
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { state.pop = Pop.Tito(state.rects.getValue("tito")) }
        val replace = rule.onNodeWithContentDescription("⇄ 更换主应用")
        val bounds = replace.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= 48 && bounds.right <= root.right - 60)
        assertTrue(bounds.top >= 36)
        replace.performTouchInput { click() }
        rule.onNodeWithTag("primary-picker").assertExists()
        rule.onNodeWithText("取消").performClick()
        assertFalse(state.primaryPicker)
    }

    @Test fun controllerCancelRestoresPickedUpModuleAndItsUndoHistory() {
        val (_, state) = launch()
        rule.runOnIdle { state.enterEdit(sel = "tito") }
        val before = state.settings
        rule.runOnIdle { state.padActivateSticker("tito"); state.padMove(Dir.Up); state.back() }
        assertNull(state.holding)
        assertEquals(before, state.settings)
        assertTrue(state.session!!.stack.isEmpty())
    }

    @Test fun primarySelectionLaunchHideAndUndoPreserveOtherPreferences() {
        val (device, state) = launch()
        val before = state.settings
        rule.runOnIdle { state.enterEdit(sel = "tito") }
        rule.onNodeWithText("更换主应用").performClick()
        rule.onNodeWithContentDescription("设为主应用：RetroArch").performClick()
        rule.waitForIdle()
        assertEquals("com.retro/R", state.settings.primaryApp)
        assertEquals(before.pinned, state.settings.pinned)
        assertEquals(before.layouts, state.settings.layouts)
        rule.runOnIdle { state.undo() }
        assertEquals(TITODEX_PACKAGE, state.settings.primaryApp)
        rule.runOnIdle { state.choosePrimary(state.apps.last()); state.finishEdit() }
        rule.onNodeWithContentDescription("打开 RetroArch").performClick()
        assertEquals(listOf("com.retro/R"), device.launched)
        rule.runOnIdle { state.enterEdit(sel = "tito"); state.hide("tito") }
        assertTrue(state.layout().first { it.id == "tito" }.hidden)
        rule.runOnIdle { state.undo(); state.finishEdit() }
        rule.onNodeWithContentDescription("打开 RetroArch").assertExists()
    }

    @Test fun originalControllerHintsRemainVisibleWithoutMovingDesktop() {
        val (_, state) = launch()
        val before = state.rects["tito"]
        val hints = hasAnyAncestor(hasTestTag("controller-hints"))
        listOf("打开", "选项", "搜索", "整理", "设置").forEach {
            rule.onNode(hasText(it) and hints).assertIsDisplayed()
        }
        rule.onNodeWithContentDescription("按键帮助").assertDoesNotExist()
        rule.onNode(hasText("搜索") and hints).performTouchInput { click() }
        assertEquals(Mode.Library, state.mode)
        rule.onNode(hasText("跳字母") and hints).assertIsDisplayed()
        rule.runOnIdle { state.back() }
        rule.runOnIdle { state.update { it.copy(hints = Hints.off) } }
        rule.waitForIdle()
        rule.onNodeWithTag("controller-hints").assertDoesNotExist()
        assertEquals(before, state.rects["tito"])
        rule.onNodeWithContentDescription("应用库").performClick()
        assertEquals(Mode.Library, state.mode)
    }
}
