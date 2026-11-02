package com.farywave.memehive.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds relationship constraints and lookup indexes without discarding valid v1 data.
 *
 * Room runs this inside its upgrade transaction. Keep foreign keys enabled: snapshot and drop the
 * junctions before rebuilding their parents so cascades cannot erase memberships. Names and SQL
 * here intentionally describe these historical versions, not current entities.
 */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val rebuiltTables =
                listOf(
                    "media_items",
                    "collections",
                    "media_item_trigrams",
                    "collection_entries",
                    "media_item_tags",
                )
            for (table in rebuiltTables) {
                db.execSQL("CREATE TEMP TABLE migration_1_2_$table AS SELECT * FROM `$table`")
            }
            db.execSQL(
                """
                CREATE TEMP TABLE migration_1_2_sequences AS
                SELECT name, seq FROM sqlite_sequence
                WHERE name IN ('media_items', 'collections', 'media_item_trigrams')
                """
                    .trimIndent()
            )

            for (table in
                listOf(
                    "collection_entries",
                    "media_item_tags",
                    "media_item_trigrams",
                    "collections",
                    "media_items",
                )) {
                db.execSQL("DROP TABLE `$table`")
            }

            db.execSQL(
                """
                CREATE TABLE media_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    image_id INTEGER,
                    caption TEXT NOT NULL,
                    description TEXT NOT NULL,
                    FOREIGN KEY (image_id) REFERENCES images (id)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE collections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    cover_image_id INTEGER,
                    FOREIGN KEY (cover_image_id) REFERENCES images (id)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE media_item_trigrams (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    trigram TEXT NOT NULL,
                    media_item_id INTEGER NOT NULL,
                    FOREIGN KEY (media_item_id) REFERENCES media_items (id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE collection_entries (
                    collection_id INTEGER NOT NULL,
                    media_item_id INTEGER NOT NULL,
                    PRIMARY KEY (collection_id, media_item_id),
                    FOREIGN KEY (collection_id) REFERENCES collections (id)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY (media_item_id) REFERENCES media_items (id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE media_item_tags (
                    media_item_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    PRIMARY KEY (media_item_id, tag_id),
                    FOREIGN KEY (media_item_id) REFERENCES media_items (id)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags (id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
                    .trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO media_items (id, image_id, caption, description)
                SELECT old.id, image.id, old.caption, old.description
                FROM migration_1_2_media_items AS old
                LEFT JOIN images AS image ON image.id = old.image_id
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO collections (id, name, cover_image_id)
                SELECT old.id, old.name, image.id FROM migration_1_2_collections AS old
                LEFT JOIN images AS image ON image.id = old.cover_image_id
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO media_item_trigrams (id, trigram, media_item_id)
                SELECT MIN(old.id), old.trigram, old.media_item_id
                FROM migration_1_2_media_item_trigrams AS old
                INNER JOIN media_items AS item ON item.id = old.media_item_id
                GROUP BY old.media_item_id, old.trigram
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO collection_entries (collection_id, media_item_id)
                SELECT old.collection_id, old.media_item_id
                FROM migration_1_2_collection_entries AS old
                INNER JOIN collections AS collection ON collection.id = old.collection_id
                INNER JOIN media_items AS item ON item.id = old.media_item_id
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO media_item_tags (media_item_id, tag_id)
                SELECT old.media_item_id, old.tag_id
                FROM migration_1_2_media_item_tags AS old
                INNER JOIN media_items AS item ON item.id = old.media_item_id
                INNER JOIN tags AS tag ON tag.id = old.tag_id
                """
                    .trimIndent()
            )

            db.execSQL("CREATE INDEX index_media_items_image_id ON media_items (image_id)")
            db.execSQL(
                "CREATE INDEX index_collections_cover_image_id ON collections (cover_image_id)"
            )
            db.execSQL(
                "CREATE INDEX index_collection_entries_media_item_id ON collection_entries (media_item_id)"
            )
            db.execSQL("CREATE INDEX index_media_item_tags_tag_id ON media_item_tags (tag_id)")
            db.execSQL(
                """
                CREATE UNIQUE INDEX index_media_item_trigrams_media_item_id_trigram
                ON media_item_trigrams (media_item_id, trigram)
                """
                    .trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX index_media_item_trigrams_trigram_media_item_id
                ON media_item_trigrams (trigram, media_item_id)
                """
                    .trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM sqlite_sequence
                WHERE name IN ('media_items', 'collections', 'media_item_trigrams')
                """
                    .trimIndent()
            )
            db.execSQL(
                "INSERT INTO sqlite_sequence (name, seq) SELECT name, seq FROM migration_1_2_sequences"
            )
            for (table in rebuiltTables) {
                db.execSQL("DROP TABLE migration_1_2_$table")
            }
            db.execSQL("DROP TABLE migration_1_2_sequences")

            db.query("PRAGMA foreign_key_check").use { violations ->
                check(!violations.moveToFirst()) { "Foreign key violation after migration 1 -> 2" }
            }
        }
    }

/** Search now uses an in-memory index; discard only the obsolete derived data. */
val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS media_item_trigrams")
        }
    }

// Existing images deliberately keep unknown dimensions; no files are inspected here.
val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE images ADD COLUMN width INTEGER")
            db.execSQL("ALTER TABLE images ADD COLUMN height INTEGER")
        }
    }
