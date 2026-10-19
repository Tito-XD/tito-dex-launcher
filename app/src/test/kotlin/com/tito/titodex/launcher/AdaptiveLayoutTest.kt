package com.tito.titodex.launcher

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AdaptiveLayoutTest {
    @Test fun safeViewportDeterminesDensityAndPortraitGetsExtraRows() {
        assertEquals(Orient.square, Grid.orientFor(480f, 408f))
        assertEquals(Orient.land, Grid.orientFor(640f, 288f))
        assertEquals(Orient.wide, Grid.orientFor(800f, 378f))
        assertEquals(Orient.portrait, Grid.orientFor(360f, 680f))
        assertEquals(Orient.tall, Grid.orientFor(412f, 840f))
        assertEquals(Orient.portraitWide, Grid.orientFor(480f, 840f))
    }

    @Test fun upgradePreservesOriginalLayoutsAndCopiesWidgetsIntoNewProfiles() {
        val old = Settings(name = "原称呼", photo = "/private/photo.jpg", wallpaper = Wall.photo)
            .withLayout(Orient.square, Grid.template(Orient.square, Template.quiet) + Sticker.widget(42, 1, 1, 2, 2))
        val json = JSONObject(old.encode()).put("schema", 3)
        val layouts = json.getJSONObject("layouts")
        Orient.entries.filter { it != it.legacy }.forEach { layouts.remove(it.name) }
        json.remove("primaryApp"); json.remove("primaryLabel")
        val migrated = Settings.decode(json.toString())
        assertEquals(old.layout(Orient.square), migrated.layout(Orient.square))
        assertEquals(old.layout(Orient.land), migrated.layout(Orient.land))
        assertEquals(old.photo, migrated.photo)
        assertEquals(old.name, migrated.name)
        assertEquals(TITODEX_PACKAGE, migrated.primaryApp)
        assertEquals(42, migrated.layout(Orient.tall).first { it.isWidget }.widgetId)
        assertTrue(migrated.layout(Orient.tall).first { it.id == "trainer" }.hidden)
        assertEquals("v", migrated.layout(Orient.tall).first { it.id == "clock" }.size)
        assertEquals(migrated, Settings.decode(migrated.encode()))
    }

    @Test fun independentPortraitEditsAndHiddenPrimarySurviveReload() {
        val original = Settings(primaryApp = "com.retro/R", primaryLabel = "RetroArch")
        val edited = original.updateSticker(Orient.tall, "tito") { it.copy(c = 2, r = 3, hidden = true) }
        val reloaded = Settings.decode(edited.encode())
        assertEquals(original.layout(Orient.square), reloaded.layout(Orient.square))
        assertEquals(edited.layout(Orient.tall), reloaded.layout(Orient.tall))
        assertEquals("com.retro/R", reloaded.primaryApp)
    }

    @Test fun oversizedWidgetKeepsItsIdentityAndSpanInPendingTray() {
        val widget = Sticker.widget(99, 0, 0, 6, 2)
        val resized = Grid.reflow(Orient.land, Orient.tall, listOf(widget)).single()
        assertEquals(99, resized.widgetId)
        assertEquals("6x2", resized.size)
        assertTrue(resized.hidden)
        assertTrue(resized.pending)
        assertEquals(setOf(99), Settings().withLayout(Orient.tall, listOf(resized)).widgetIds())
        val restored = Grid.reflow(Orient.tall, Orient.land, listOf(resized)).single()
        assertFalse(restored.hidden)
        assertFalse(restored.pending)
    }
}
