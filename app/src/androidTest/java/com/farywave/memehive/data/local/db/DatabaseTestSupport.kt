package com.farywave.memehive.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertFalse

internal val databaseTables =
    listOf(
        "images",
        "media_items",
        "collections",
        "tags",
        "collection_entries",
        "media_item_tags",
        "media_item_trigrams",
    )

internal fun SupportSQLiteDatabase.rows(sql: String): List<List<String?>> =
    query(sql).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    (0 until cursor.columnCount).map { column ->
                        if (cursor.isNull(column)) null else cursor.getString(column)
                    }
                )
            }
        }
    }

internal fun SupportSQLiteDatabase.assertForeignKeysValid() {
    query("PRAGMA foreign_key_check").use {
        assertFalse("Database contains a dangling foreign key", it.moveToFirst())
    }
}
