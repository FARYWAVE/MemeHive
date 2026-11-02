package com.farywave.memehive.data.local.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "migration-1-to-2-test"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MemeHiveDatabase::class.java,
        )

    @Before
    fun removeOldDatabase() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun emptyDatabaseUpgradesAndReopens() {
        helper.createDatabase(databaseName, 1).close()
        migrate().use { db ->
            databaseTables.forEach { assertTrue(db.rows("SELECT * FROM $it").isEmpty()) }
            assertMigrationFinished(db)
        }
        assertReopens()
    }

    @Test
    fun validDataAndAutoincrementSequencesSurviveUpgrade() {
        val before =
            helper.createDatabase(databaseName, 1).use { db ->
                seedValidData(db)
                // A deleted high ID must remain reserved after the table rebuild.
                db.execSQL("INSERT INTO media_items VALUES (500, NULL, 'deleted', '')")
                db.execSQL("DELETE FROM media_items WHERE id = 500")
                db.execSQL("INSERT INTO collections VALUES (600, 'deleted', NULL)")
                db.execSQL("DELETE FROM collections WHERE id = 600")
                db.execSQL("INSERT INTO media_item_trigrams VALUES (700, 'old', 10)")
                db.execSQL("DELETE FROM media_item_trigrams WHERE id = 700")
                snapshot(db)
            }
        migrate().use { db ->
            assertEquals(before, snapshot(db))
            assertMigrationFinished(db)
            db.execSQL(
                "INSERT INTO media_items (image_id, caption, description) VALUES (NULL, 'new', '')"
            )
            db.execSQL("INSERT INTO collections (name, cover_image_id) VALUES ('new', NULL)")
            db.execSQL(
                "INSERT INTO media_item_trigrams (trigram, media_item_id) VALUES ('new', 10)"
            )
            assertEquals(
                listOf(listOf("501")),
                db.rows("SELECT id FROM media_items WHERE caption = 'new'"),
            )
            assertEquals(
                listOf(listOf("601")),
                db.rows("SELECT id FROM collections WHERE name = 'new'"),
            )
            assertEquals(
                listOf(listOf("701")),
                db.rows("SELECT id FROM media_item_trigrams WHERE trigram = 'new'"),
            )
        }
        assertReopens()
    }

    @Test
    fun inconsistentDataIsRepairedWithoutLosingValidRelationships() {
        helper.createDatabase(databaseName, 1).use { db ->
            seedValidData(db)
            // Simulate a legacy database written with FK enforcement disabled. Re-enable it
            // BEFORE upgrading: the migration itself must work with enforcement enabled.
            db.setForeignKeyConstraintsEnabled(false)
            db.execSQL(
                "INSERT INTO media_items VALUES (12, 999, 'keep broken-image item', 'keep description')"
            )
            db.execSQL("INSERT INTO collections VALUES (22, 'keep broken-cover collection', 999)")
            db.execSQL("INSERT INTO media_item_trigrams VALUES (33, 'cat', 10), (34, 'bad', 999)")
            db.execSQL("INSERT INTO collection_entries VALUES (22, 12), (20, 999), (999, 10)")
            db.execSQL("INSERT INTO media_item_tags VALUES (12, 2), (999, 2), (10, 999)")
            db.setForeignKeyConstraintsEnabled(true)
        }
        migrate().use { db ->
            assertEquals(
                listOf(listOf("12", null, "keep broken-image item", "keep description")),
                db.rows("SELECT * FROM media_items WHERE id = 12"),
            )
            assertEquals(
                listOf(listOf("22", "keep broken-cover collection", null)),
                db.rows("SELECT * FROM collections WHERE id = 22"),
            )
            assertEquals(
                listOf(
                    listOf("30", "cat", "10"),
                    listOf("31", "dog", "10"),
                    listOf("32", "cat", "11"),
                ),
                db.rows("SELECT * FROM media_item_trigrams ORDER BY id"),
            )
            assertEquals(
                listOf(
                    listOf("20", "10"),
                    listOf("20", "11"),
                    listOf("21", "10"),
                    listOf("22", "12"),
                ),
                db.rows("SELECT * FROM collection_entries ORDER BY collection_id, media_item_id"),
            )
            assertEquals(
                listOf(listOf("10", "2"), listOf("11", "3"), listOf("12", "2")),
                db.rows("SELECT * FROM media_item_tags ORDER BY media_item_id, tag_id"),
            )
            assertEquals(
                listOf(listOf("1", "/cover.png", "cover-hash")),
                db.rows("SELECT * FROM images"),
            )
            assertEquals(
                listOf(listOf("2", "cats"), listOf("3", "dogs")),
                db.rows("SELECT * FROM tags ORDER BY id"),
            )
            assertEquals(
                listOf(listOf("10", "1", "first", "first description")),
                db.rows("SELECT * FROM media_items WHERE id = 10"),
            )
            assertEquals(
                listOf(listOf("20", "first collection", "1")),
                db.rows("SELECT * FROM collections WHERE id = 20"),
            )
            assertMigrationFinished(db)
        }
        assertReopens()
    }

    @Test
    fun roomOpensAnOldDatabaseThroughTheRegisteredMigration(): Unit = runBlocking {
        helper.createDatabase(databaseName, 1).use(::seedValidData)
        val db =
            Room.databaseBuilder(context, MemeHiveDatabase::class.java, databaseName)
                .addMigrations(
                    MIGRATION_1_2,
                    ImageFilenameMigration(context.filesDir),
                    MIGRATION_3_4,
                )
                .build()
        try {
            val summaries = db.collectionDao().observeAll().first()
            assertEquals(listOf(2, 1), summaries.map { it.mediaItemCount })
            assertEquals(1L, summaries.first().coverImage?.id)
            assertEquals("first", db.mediaItemDao().get(10)?.mediaItem?.caption)
            val id =
                db.mediaItemDao()
                    .insert(
                        MediaItemEntity(imageId = null, caption = "after upgrade", description = "")
                    )
            assertTrue(id > 11)
            assertEquals(4, db.openHelper.writableDatabase.version)
            db.openHelper.writableDatabase.assertForeignKeysValid()
        } finally {
            db.close()
        }
        assertReopens()
    }

    @Test
    fun failedMigrationRollsBackRebuiltTablesAndData() {
        helper.createDatabase(databaseName, 1).use { db ->
            seedValidData(db)
            val before = snapshot(db)
            val failingDatabase =
                object : SupportSQLiteDatabase by db {
                    override fun execSQL(sql: String) {
                        if (sql.startsWith("CREATE INDEX")) {
                            throw IllegalStateException("Simulated failure after restoring data")
                        }
                        db.execSQL(sql)
                    }
                }
            db.beginTransaction()
            try {
                try {
                    MIGRATION_1_2.migrate(failingDatabase)
                    org.junit.Assert.fail("Expected the simulated migration failure")
                } catch (failure: IllegalStateException) {
                    assertEquals("Simulated failure after restoring data", failure.message)
                }
            } finally {
                // No setTransactionSuccessful(): mirrors Room rolling back a failed upgrade.
                db.endTransaction()
            }
            assertEquals(1, db.version)
            assertEquals(before, snapshot(db))
            db.assertForeignKeysValid()
            assertTrue(
                db.rows("SELECT name FROM sqlite_temp_master WHERE name LIKE 'migration_1_2_%'")
                    .isEmpty()
            )
        }
        // Retrying the real upgrade after rollback must still succeed.
        migrate().use(::assertMigrationFinished)
        assertReopens()
    }

    private fun migrate(): SupportSQLiteDatabase =
        helper.runMigrationsAndValidate(databaseName, 2, true, MIGRATION_1_2)

    private fun assertMigrationFinished(db: SupportSQLiteDatabase) {
        assertEquals(2, db.version)
        db.assertForeignKeysValid()
        assertTrue(
            db.rows("SELECT name FROM sqlite_temp_master WHERE name LIKE 'migration_1_2_%'")
                .isEmpty()
        )
    }

    private fun assertReopens(): Unit = runBlocking {
        val db =
            Room.databaseBuilder(context, MemeHiveDatabase::class.java, databaseName)
                .addMigrations(
                    MIGRATION_1_2,
                    ImageFilenameMigration(context.filesDir),
                    MIGRATION_3_4,
                )
                .build()
        try {
            assertEquals(4, db.openHelper.writableDatabase.version)
            db.openHelper.writableDatabase.assertForeignKeysValid()
            db.collectionDao().observeAll().first()
            db.mediaItemDao().observeAll().first()
        } finally {
            db.close()
        }
    }

    private fun snapshot(db: SupportSQLiteDatabase): Map<String, List<List<String?>>> =
        (databaseTables + "sqlite_sequence").associateWith { table ->
            db.rows("SELECT * FROM $table ORDER BY 1, 2")
        }

    private fun seedValidData(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO images VALUES (1, '/cover.png', 'cover-hash')")
        db.execSQL("INSERT INTO tags VALUES (2, 'cats'), (3, 'dogs')")
        db.execSQL(
            "INSERT INTO media_items VALUES (10, 1, 'first', 'first description'), (11, NULL, 'second', '')"
        )
        db.execSQL(
            "INSERT INTO collections VALUES (20, 'first collection', 1), (21, 'second collection', NULL)"
        )
        db.execSQL("INSERT INTO collection_entries VALUES (20, 10), (20, 11), (21, 10)")
        db.execSQL("INSERT INTO media_item_tags VALUES (10, 2), (11, 3)")
        db.execSQL(
            "INSERT INTO media_item_trigrams VALUES (30, 'cat', 10), (31, 'dog', 10), (32, 'cat', 11)"
        )
    }
}
