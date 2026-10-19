package com.tito.titodex.launcher.ui

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.core.content.res.ResourcesCompat
import com.tito.titodex.launcher.R

/** Visible glyph bounds, including Android's CJK fallback, rather than the font's line box. */
@Composable
internal fun textInk(value: String, style: TextStyle): Rect {
    val context = LocalContext.current
    val density = LocalDensity.current
    val font = when (style.fontWeight) {
        FontWeight.ExtraBold -> R.font.nunito_extrabold
        FontWeight.Normal -> R.font.nunito_regular
        else -> R.font.nunito_bold
    }
    val face = remember(context, font) { ResourcesCompat.getFont(context, font) }
    val px = with(density) { style.fontSize.toPx() }
    return remember(value, face, px) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = face; textSize = px }
        Rect().also { paint.getTextBounds(value.ifBlank { "Hg" }, 0, value.ifBlank { "Hg" }.length, it) }
    }
}

/** The layout box follows visible ink; baseline placement still comes from Compose shaping. */
@Composable
internal fun OpticalText(value: AnnotatedString, style: TextStyle, modifier: Modifier = Modifier) {
    val ink = textInk(value.text, style)
    Text(value, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            val height = constraints.constrainHeight(ink.height().coerceAtLeast(1))
            layout(placeable.width, height) {
                placeable.placeRelative(0, -placeable[FirstBaseline] - ink.top)
            }
        })
}

@Composable
internal fun OpticalText(value: String, style: TextStyle, modifier: Modifier = Modifier) =
    OpticalText(AnnotatedString(value), style, modifier)
