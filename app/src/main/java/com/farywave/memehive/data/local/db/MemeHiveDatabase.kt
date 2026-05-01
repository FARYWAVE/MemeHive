package com.farywave.memehive.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.farywave.memehive.data.local.db.dao.*
import com.farywave.memehive.data.local.db.entity.*

@Database(
    entities = [
        MediaItemEntity::class,
        TagEntity::class,
        MediaItemTagEntity::class,
        MediaItemTrigramEntity::class,
        CollectionEntity::class,
        CollectionEntryEntity::class
    ],
    version = 1
)
abstract class MemeHiveDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun mediaItemTagDao(): MediaItemTagDao
    abstract fun tagDao(): TagDao
    abstract fun mediaItemTrigramDao(): MediaItemTrigramDao

    abstract fun collectionDao(): CollectionDao
    abstract fun collectionEntryDao(): CollectionEntryDao

    companion object {
        @Volatile
        private var INSTANCE: MemeHiveDatabase? = null

        fun getInstance(context: Context): MemeHiveDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemeHiveDatabase::class.java,
                    "memehive_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}