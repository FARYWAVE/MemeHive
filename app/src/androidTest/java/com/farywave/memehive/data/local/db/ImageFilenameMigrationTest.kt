package com.farywave.memehive.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImageFilenameMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "image-filename-migration-test"
    private val directory = File(context.cacheDir, "image-migration-fixture")

    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), MemeHiveDatabase::class.java,
    )

    @Before fun prepare() {
        context.deleteDatabase(databaseName)
        directory.deleteRecursively()
        check(directory.mkdirs())
    }

    @After fun cleanUp() {
        context.deleteDatabase(databaseName)
        directory.deleteRecursively()
    }

    @Test fun migratesPathsAndFileUrisWithoutLosingRelationsOrSequences() {
        val first = File(directory, "мем with spaces.png").apply { writeText("first") }
        val second = File(directory, "other.png").apply { writeText("second") }
        helper.createDatabase(databaseName, 2).use { db ->
            db.execSQL("INSERT INTO images VALUES (1, ?, 'first'), (2, ?, 'second')", arrayOf(first.path, second.toURI().toString()))
            db.execSQL("INSERT INTO images VALUES (100, '/missing.png', 'deleted')")
            db.execSQL("DELETE FROM images WHERE id = 100")
            db.execSQL("INSERT INTO media_items VALUES (10, 1, 'caption', '')")
            db.execSQL("INSERT INTO collections VALUES (20, 'collection', 2)")
            db.execSQL("INSERT INTO collection_entries VALUES (20, 10)")
            db.execSQL("INSERT INTO tags VALUES (30, 'tag')")
            db.execSQL("INSERT INTO media_item_tags VALUES (10, 30)")
            db.execSQL("INSERT INTO media_item_trigrams VALUES (40, 'cap', 10)")
        }
        helper.runMigrationsAndValidate(databaseName, 3, true, ImageFilenameMigration(directory)).use { db ->
            assertEquals(listOf(listOf("1", "legacy_1.png"), listOf("2", "legacy_2.png")), db.rows("SELECT id, name FROM images ORDER BY id"))
            assertEquals(listOf(listOf("1")), db.rows("SELECT image_id FROM media_items WHERE id = 10"))
            assertEquals(listOf(listOf("2")), db.rows("SELECT cover_image_id FROM collections WHERE id = 20"))
            assertEquals(listOf(listOf("20", "10")), db.rows("SELECT * FROM collection_entries"))
            assertEquals(listOf(listOf("10", "30")), db.rows("SELECT * FROM media_item_tags"))
            assertEquals(listOf(listOf("40", "cap", "10")), db.rows("SELECT * FROM media_item_trigrams"))
            db.execSQL("INSERT INTO images (name, sha256) VALUES ('new.png', 'new')")
            assertEquals(listOf(listOf("101")), db.rows("SELECT id FROM images WHERE name = 'new.png'"))
            db.assertForeignKeysValid()
        }
        assertEquals("first", File(directory, "images/legacy_1.png").readText())
        assertEquals("second", File(directory, "images/legacy_2.png").readText())
        assertTrue(first.exists())
        assertTrue(second.exists())
    }

    @Test fun databaseRollbackCanRetryAfterFilesHaveAlreadyBeenCopied() {
        val source = File(directory, "old.png").apply { writeText("original") }
        helper.createDatabase(databaseName, 2).use { db ->
            db.execSQL("INSERT INTO images VALUES (1, ?, 'hash')", arrayOf(source.path))
            db.beginTransaction()
            try {
                ImageFilenameMigration(directory).migrate(db)
                assertTrue(File(directory, "images/legacy_1.png").exists())
            } finally {
                db.endTransaction() // Simulate failure after file copy, before database commit.
            }
            assertEquals(listOf(listOf(source.path)), db.rows("SELECT path FROM images"))
        }
        helper.runMigrationsAndValidate(databaseName, 3, true, ImageFilenameMigration(directory)).use { db ->
            assertEquals(listOf(listOf("legacy_1.png")), db.rows("SELECT name FROM images"))
            db.assertForeignKeysValid()
        }
        assertEquals("original", File(directory, "images/legacy_1.png").readText())
        assertEquals(listOf("legacy_1.png"), File(directory, "images").list()!!.toList())
        assertTrue(source.exists())
    }
}
