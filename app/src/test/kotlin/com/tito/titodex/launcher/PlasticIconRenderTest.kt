package com.tito.titodex.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.tito.titodex.launcher.ui.AppIcon
import com.tito.titodex.launcher.ui.LocalLook
import com.tito.titodex.launcher.ui.lookOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Original test artwork, not copied app icons: circle, rounded square, hollow cross. */
internal fun plasticPreviewIcons(): Map<String, ImageBitmap> =
    listOf("com.a/A", "com.b/B", "com.retro/R").mapIndexed { i, id ->
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = listOf(0xff367fba.toInt(), 0xffcf7955.toInt(), 0xff438b72.toInt())[i]
        }
        when (i) {
            0 -> canvas.drawCircle(64f, 64f, 51f, paint)
            1 -> canvas.drawRoundRect(13f, 13f, 115f, 115f, 22f, 22f, paint)
            else -> canvas.drawPath(Path().apply {
                moveTo(40f, 8f); lineTo(88f, 8f); lineTo(88f, 40f); lineTo(120f, 40f)
                lineTo(120f, 88f); lineTo(88f, 88f); lineTo(88f, 120f); lineTo(40f, 120f)
                lineTo(40f, 88f); lineTo(8f, 88f); lineTo(8f, 40f); lineTo(40f, 40f); close()
            }, paint)
        }
        if (i == 2) {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            canvas.drawCircle(64f, 64f, 17f, paint)
        } else {
            paint.color = 0xfffff7e6.toInt()
            canvas.drawCircle(64f, 64f, 20f, paint)
        }
        id to bitmap.asImageBitmap()
    }.toMap()

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w480dp-h480dp-hdpi")
class PlasticIconRenderTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun lightStaysInsideArtworkAndDoesNotFillTransparentHoles() {
        val icon = plasticPreviewIcons().getValue("com.retro/R")
        val background = 0xffdcc9b7.toInt()
        rule.setContent {
            Row(Modifier.fillMaxSize().background(Color(background)).padding(20.dp)) {
                for (theme in listOf(LauncherTheme.flat, LauncherTheme.plastic)) {
                    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                        CompositionLocalProvider(LocalLook provides lookOf(Settings(theme = theme))) {
                            AppIcon(icon, theme.name, 96)
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        val plain = rule.onNodeWithContentDescription("flat").fetchSemanticsNode().boundsInRoot
        val lit = rule.onNodeWithContentDescription("plastic").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/validation/adaptive/plastic-icon-light.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            fun pixel(bounds: androidx.compose.ui.geometry.Rect, x: Float, y: Float) =
                bitmap.getPixel((bounds.left + bounds.width * x).toInt(), (bounds.top + bounds.height * y).toInt())
            for ((x, y) in listOf(.02f to .02f, .5f to .5f, -.04f to .5f, .5f to -.04f)) {
                assertEquals("no outer frame or painted transparent hole at $x,$y", background, pixel(lit, x, y))
            }
            val original = pixel(plain, .5f, .18f)
            val highlighted = pixel(lit, .5f, .18f)
            val delta = android.graphics.Color.red(highlighted) - android.graphics.Color.red(original)
            assertTrue("visible but restrained highlight: $delta", delta in 1..35)
            bitmap.recycle()
        }
    }
}
