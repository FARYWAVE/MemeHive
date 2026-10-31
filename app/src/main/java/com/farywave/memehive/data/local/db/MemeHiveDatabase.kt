package com.farywave.memehive.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.farywave.memehive.data.local.db.dao.CollectionDao
import com.farywave.memehive.data.local.db.dao.CollectionEntryDao
import com.farywave.memehive.data.local.db.dao.ImageDao
import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.dao.MediaItemTagDao
import com.farywave.memehive.data.local.db.dao.TagDao
import com.farywave.memehive.data.local.db.entity.CollectionEntity
import com.farywave.memehive.data.local.db.entity.CollectionEntryEntity
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.TagEntity

@Database(
    entities = [
        MediaItemEntity::class,
        ImageEntity::class,
        TagEntity::class,
        MediaItemTagEntity::class,
        CollectionEntity::class,
        CollectionEntryEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class MemeHiveDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun tagDao(): TagDao
    abstract fun mediaItemTagDao(): MediaItemTagDao

    abstract fun collectionDao(): CollectionDao
    abstract fun collectionEntryDao(): CollectionEntryDao

    abstract fun imageDao(): ImageDao

    companion object {
        @Volatile
        private var INSTANCE: MemeHiveDatabase? = null

        fun getInstance(context: Context): MemeHiveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemeHiveDatabase::class.java,
                    "memehive_db"
                ).addMigrations(MIGRATION_1_2, ImageFilenameMigration(context.applicationContext.filesDir), MIGRATION_3_4).build().also { INSTANCE = it }
            }
        }
    }
}
