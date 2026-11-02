package com.farywave.memehive.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SchemaExportTest {

    private val testDatabaseName = "schema-export-test"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MemeHiveDatabase::class.java,
        )

    @After
    fun cleanUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(testDatabaseName)
    }

    @Test
    fun version1SchemaCanCreateAUsableDatabase() {
        helper.createDatabase(testDatabaseName, 1).use { database ->
            assertEquals(1, database.version)

            database.execSQL(
                """
                INSERT INTO collections (id, name, cover_image_id)
                VALUES (1, 'Test collection', NULL)
                """
                    .trimIndent()
            )

            database.query("SELECT name FROM collections WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Test collection", cursor.getString(0))
            }
        }
    }
}
