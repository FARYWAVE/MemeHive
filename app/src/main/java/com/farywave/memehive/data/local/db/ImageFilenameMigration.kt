package com.farywave.memehive.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.io.IOException
import java.net.URI

class ImageFilenameMigration(private val filesDirectory: File) : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val names = mutableMapOf<Long, String>()
        db.query("SELECT id, path FROM images").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                names[id] = copyLegacyImage(id, cursor.getString(1))
            }
        }

        val tables = listOf("images", "media_items", "collections", "media_item_trigrams", "collection_entries", "media_item_tags")
        for (table in tables) {
            db.execSQL("CREATE TEMP TABLE migration_2_3_$table AS SELECT * FROM `$table`")
        }
        db.execSQL("CREATE TEMP TABLE migration_2_3_sequences AS SELECT name, seq FROM sqlite_sequence")
        for (table in tables.asReversed()) db.execSQL("DROP TABLE `$table`")

        db.execSQL("CREATE TABLE IF NOT EXISTS `images` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sha256` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_images_sha256` ON `images` (`sha256`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_images_name` ON `images` (`name`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `media_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `image_id` INTEGER, `caption` TEXT NOT NULL, `description` TEXT NOT NULL, FOREIGN KEY(`image_id`) REFERENCES `images`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_items_image_id` ON `media_items` (`image_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `cover_image_id` INTEGER, FOREIGN KEY(`cover_image_id`) REFERENCES `images`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_collections_cover_image_id` ON `collections` (`cover_image_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `media_item_trigrams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trigram` TEXT NOT NULL, `media_item_id` INTEGER NOT NULL, FOREIGN KEY(`media_item_id`) REFERENCES `media_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_media_item_trigrams_media_item_id_trigram` ON `media_item_trigrams` (`media_item_id`, `trigram`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_item_trigrams_trigram_media_item_id` ON `media_item_trigrams` (`trigram`, `media_item_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `collection_entries` (`collection_id` INTEGER NOT NULL, `media_item_id` INTEGER NOT NULL, PRIMARY KEY(`collection_id`, `media_item_id`), FOREIGN KEY(`collection_id`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`media_item_id`) REFERENCES `media_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_entries_media_item_id` ON `collection_entries` (`media_item_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `media_item_tags` (`media_item_id` INTEGER NOT NULL, `tag_id` INTEGER NOT NULL, PRIMARY KEY(`media_item_id`, `tag_id`), FOREIGN KEY(`media_item_id`) REFERENCES `media_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_item_tags_tag_id` ON `media_item_tags` (`tag_id`)")

        for ((id, name) in names) {
            db.execSQL(
                "INSERT INTO images (id, name, sha256) SELECT id, ?, sha256 FROM migration_2_3_images WHERE id = ?",
                arrayOf<Any>(name, id),
            )
        }
        for (table in tables.drop(1)) {
            db.execSQL("INSERT INTO `$table` SELECT * FROM migration_2_3_$table")
        }
        db.execSQL("DELETE FROM sqlite_sequence")
        db.execSQL("INSERT INTO sqlite_sequence (name, seq) SELECT name, seq FROM migration_2_3_sequences")
        for (table in tables) db.execSQL("DROP TABLE migration_2_3_$table")
        db.execSQL("DROP TABLE migration_2_3_sequences")
    }

    private fun copyLegacyImage(id: Long, path: String): String {
        val legacy = if (path.startsWith("file:")) File(URI(path)) else File(path)
        val root = filesDirectory.canonicalFile
        val source = if (legacy.canonicalFile.toPath().startsWith(root.toPath())) legacy
            else File(root, legacy.name)
        val extension = source.extension.lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,10}")) } ?: "bin"
        val name = "legacy_${id}.$extension"
        val directory = File(root, "images")
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Could not create image directory: $directory")
        }
        val destination = File(directory, name)
        if (!source.exists()) return name
        if (!source.isFile) throw IOException("Legacy image is not a file: $source")
        val staging = File(directory, "$name.migrating")
        try {
            source.inputStream().use { input ->
                staging.outputStream().use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            if (!staging.renameTo(destination)) throw IOException("Could not migrate image: $source")
        } finally {
            if (staging.exists() && !staging.delete()) throw IOException("Could not remove migration file: $staging")
        }
        return name
    }
}
