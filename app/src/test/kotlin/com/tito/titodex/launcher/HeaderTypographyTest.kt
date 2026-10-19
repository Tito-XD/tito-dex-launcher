package com.tito.titodex.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
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
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.floor

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-hdpi")
class HeaderTypographyTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun launch(): Pair<FakeDevice, LauncherState> {
        val device = FakeDevice(Settings(theme = LauncherTheme.plastic, motion = false, coachSeen = true,
            hints = Hints.always, city = City("测试城市", 31.0, 121.0)).encode())
        lateinit var state: LauncherState
        rule.setContent {
            val scope = rememberCoroutineScope()
            state = remember { LauncherState(device, NoWeather, scope, Dispatchers.Unconfined) }
            LaunchedEffect(Unit) { state.start(); state.weather = WeatherReading(26.0, 3, 0L) }
            LauncherApp(state, clock = { Calendar.getInstance().apply { set(2026, 9, 19, 10, 8, 0) } },
                contentInsets = WindowInsets(left = 0, top = 36, right = 0, bottom = 48))
        }
        rule.waitUntil(5_000) { state.loaded && state.appsLoaded }
        rule.waitForIdle()
        return device to state
    }

    private fun capture(name: String): Bitmap {
        lateinit var bitmap: Bitmap
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val out = File("build/validation/header/$name.png")
            out.parentFile!!.mkdirs()
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return bitmap
    }

    /** Check actual painted ink, independently of Compose layout boxes/font metrics. */
    private fun pixels(bitmap: Bitmap, bounds: Rect, matches: (Int) -> Boolean): Rect {
        var left = bitmap.width; var top = bitmap.height; var right = -1; var bottom = -1
        for (y in floor(bounds.top).toInt().coerceAtLeast(0) until ceil(bounds.bottom).toInt().coerceAtMost(bitmap.height))
            for (x in floor(bounds.left).toInt().coerceAtLeast(0) until ceil(bounds.right).toInt().coerceAtMost(bitmap.width)) {
                if (matches(bitmap.getPixel(x, y))) {
                    left = minOf(left, x); top = minOf(top, y); right = maxOf(right, x); bottom = maxOf(bottom, y)
                }
            }
        assertTrue("no visible text pixels in $bounds", right >= left && bottom >= top)
        return Rect(left.toFloat(), top.toFloat(), right + 1f, bottom + 1f)
    }

    @Test fun portraitClockUsesCellWidthAndAvatarAlignsWithVisibleTwoLineInk() {
        val (_, state) = launch()
        val bitmap = capture("portrait-plastic")
        val avatar = rule.onNodeWithTag("trainer-avatar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithTag("trainer-name", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val greeting = rule.onNodeWithTag("trainer-greeting", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val textBounds = Rect(title.left, title.top - 3, maxOf(title.right, greeting.right), greeting.bottom + 3)
        val painted = pixels(bitmap, textBounds) { Color.red(it) < 140 && Color.green(it) < 140 && Color.blue(it) < 140 }
        assertEquals("avatar must align to visible title ink, not padded font ascent", painted.top, avatar.top, 3f)
        assertEquals("avatar must align to visible greeting ink", painted.bottom, avatar.bottom, 3f)
        assertTrue("portrait avatar should grow beyond the previous 38dp", avatar.height > 38 * rule.activity.resources.displayMetrics.density)
        val time = rule.onNodeWithTag("clock-time").fetchSemanticsNode().boundsInRoot
        val digits = pixels(bitmap, time) { Color.red(it) > 145 && Color.green(it) > 150 && Color.blue(it) > 150 }
        val metadata = rule.onNodeWithTag("clock-metadata").fetchSemanticsNode().boundsInRoot
        val date = rule.onNodeWithText("10/19").fetchSemanticsNode().boundsInRoot
        val weather = rule.onNodeWithTag("clock-weather").fetchSemanticsNode().boundsInRoot
        assertEquals("painted time should be centered in its cell", metadata.center.x, digits.center.x, 3f)
        assertEquals("date, weekday and weather form one centered row", metadata.center.x, (date.left + weather.right) / 2, 2f)
        val usableWidth = (state.cellDp().first * 2 - 18) * rule.activity.resources.displayMetrics.density
        assertTrue("time should visually fill most of the 2x1 width: $digits / $usableWidth", digits.width >= usableWidth * .85f)
        bitmap.recycle()
        rule.onNodeWithTag("weather-icon", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("MON").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            val result = mutableListOf<TextLayoutResult>(); assertTrue(action(result))
            assertTrue("weekday must be hollow", result.single().layoutInput.style.drawStyle is Stroke)
        }
        rule.onNodeWithContentDescription("周一").assertExists()
        for (theme in listOf(LauncherTheme.journal, LauncherTheme.flat)) {
            rule.runOnIdle { state.setTheme(theme) }
            capture("portrait-${theme.name}").recycle()
            rule.onNodeWithText("MON").assertIsDisplayed()
            rule.onNodeWithTag("weather-icon", useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test @Config(qualifiers = "w480dp-h480dp-hdpi")
    fun avatarShapeSavesOnlyOnDoneAndUndoPreservesOtherPreferences() {
        val (device, state) = launch()
        val before = state.settings
        capture("square-plastic").recycle()
        rule.onNodeWithContentDescription("个人名片").performClick()
        rule.onNodeWithContentDescription("圆形").performClick()
        assertEquals(AvatarShape.rounded, state.settings.avatarShape)
        rule.onNodeWithContentDescription("✓ 完成").performClick()
        assertEquals(before.copy(avatarShape = AvatarShape.circle), state.settings)
        assertEquals(state.settings, Settings.decode(device.stored))
        rule.onNodeWithContentDescription("撤销").performClick()
        assertEquals(before, state.settings)
        assertEquals(before, Settings.decode(device.stored))
        // Capture the saved shape without the transient undo message covering the avatar.
        rule.runOnIdle {
            state.update { it.copy(avatarShape = AvatarShape.circle) }
        }
        capture("square-circle").recycle()
    }
}
