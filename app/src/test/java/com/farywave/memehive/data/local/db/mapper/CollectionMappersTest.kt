package com.farywave.memehive.data.local.db.mapper

import com.farywave.memehive.data.local.db.entity.CollectionEntity
import com.farywave.memehive.data.local.db.relation.CollectionDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionMappersTest {
    @Test
    fun renamingLoadedCollectionPreservesItsDatabaseId() {
        val storedCollection = CollectionDetails(
            collection = CollectionEntity(id = 42L, name = "Original", coverImageId = null),
            mediaItemCount = 3
        )

        val renamed = storedCollection.toModel(cover = null).copy(name = "Renamed")
        val update = renamed.toEntity()

        assertEquals(42L, update.id)
        assertEquals("Renamed", update.name)
        assertEquals(3, renamed.mediaItemCount)
    }
}
