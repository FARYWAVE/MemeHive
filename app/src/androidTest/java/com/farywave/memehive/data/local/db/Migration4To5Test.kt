package com.farywave.memehive.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Migration4To5Test {
    private val name = "image-dimensions-migration"
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), MemeHiveDatabase::class.java,
    )

    @After fun cleanUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
    }

    @Test fun preservesOldImagesWithUnknownDimensions() {
        helper.createDatabase(name, 4).use { db ->
            db.execSQL("INSERT INTO images VALUES (1, 'old.png', 'old-hash')")
            db.execSQL("INSERT INTO media_items VALUES (10, 1, 'caption', 'description')")
        }
        helper.runMigrationsAndValidate(name, 5, true, MIGRATION_4_5).use { db ->
            assertEquals(
                listOf(listOf("1", "old.png", "old-hash", null, null)),
                db.rows("SELECT * FROM images"),
            )
            db.execSQL("INSERT INTO images (name, sha256, width, height) VALUES ('new.png', 'new-hash', 640, 480)")
            assertEquals(listOf(listOf("640", "480")), db.rows("SELECT width, height FROM images WHERE name = 'new.png'"))
            db.assertForeignKeysValid()
        }
    }
}
