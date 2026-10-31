package com.farywave.memehive.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration3To4Test {
    private val name = "remove-unused-search-index"
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), MemeHiveDatabase::class.java,
    )

    @After fun cleanUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
    }

    @Test fun removesOnlyDerivedSearchDataAndPreservesRelationships() {
        val preservedTables = listOf("images", "media_items", "collections", "tags", "media_item_tags", "collection_entries")
        val before = helper.createDatabase(name, 3).use { db ->
            db.execSQL("INSERT INTO images VALUES (1, 'image.png', 'hash')")
            db.execSQL("INSERT INTO media_items VALUES (10, 1, 'caption', 'description')")
            db.execSQL("INSERT INTO collections VALUES (20, 'collection', 1)")
            db.execSQL("INSERT INTO tags VALUES (30, 'tag')")
            db.execSQL("INSERT INTO collection_entries VALUES (20, 10)")
            db.execSQL("INSERT INTO media_item_tags VALUES (10, 30)")
            db.execSQL("INSERT INTO media_item_trigrams VALUES (40, 'cap', 10)")
            db.execSQL("INSERT INTO media_items VALUES (100, NULL, 'deleted', '')")
            db.execSQL("DELETE FROM media_items WHERE id = 100")
            preservedTables.associateWith { db.rows("SELECT * FROM $it ORDER BY 1") }
        }
        helper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4).use { db ->
            assertEquals(before, preservedTables.associateWith { db.rows("SELECT * FROM $it ORDER BY 1") })
            db.query("SELECT name FROM sqlite_master WHERE name = 'media_item_trigrams'").use {
                assertFalse(it.moveToFirst())
            }
            db.execSQL("INSERT INTO media_items (caption, description) VALUES ('new', '')")
            assertEquals(listOf(listOf("101")), db.rows("SELECT id FROM media_items WHERE caption = 'new'"))
            db.assertForeignKeysValid()
        }
    }
}
