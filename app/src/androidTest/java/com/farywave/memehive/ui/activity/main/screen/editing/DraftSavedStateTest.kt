package com.farywave.memehive.ui.activity.main.screen.editing

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.model.MediaItemDraft
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftSavedStateTest {
    @Test
    fun draftSurvivesParcelRoundTripWithIdentityAndContent() {
        val draft = MediaItemDraft(
            id = 42L,
            image = Image(id = 7L, uri = Uri.parse("content://images/7")),
            caption = "Unsaved caption",
            description = "Unsaved description",
            tags = listOf(MediaItemDraft.EditableTag(8, "one"), MediaItemDraft.EditableTag(12, "two")),
            collectionIds = setOf(3L, 9L),
        )
        val restored = roundTrip(draft.toSavedBundle()).toDraft()
        assertTrue(draft.hasSameContentAs(restored))
        assertEquals(draft.id, restored.id)
        assertEquals(draft.image, restored.image)
        assertEquals(listOf(0, 1), restored.tags.map { it.key })
    }

    @Test
    fun clearedFieldsRemainEmptyRatherThanFallingBackToSavedContent() {
        val restored = roundTrip(MediaItemDraft(id = 42L).toSavedBundle()).toDraft()
        assertEquals(MediaItemDraft(id = 42L), restored)
        assertNull(restored.image)
    }

    private fun roundTrip(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            parcel.setDataPosition(0)
            requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }
}
