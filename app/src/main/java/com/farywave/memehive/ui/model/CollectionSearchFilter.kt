package com.farywave.memehive.ui.model

import com.farywave.memehive.application.usecase.MediaSearchState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal data class CollectionMembershipState(
    val memberships: Map<Long, Set<Long>>? = null,
    val failure: Throwable? = null,
)

internal fun Flow<Map<Long, Set<Long>>>.asMembershipState(): Flow<CollectionMembershipState> =
    map { CollectionMembershipState(memberships = it) }
        .onStart { emit(CollectionMembershipState()) }
        .catch {
            if (it is CancellationException) throw it
            emit(CollectionMembershipState(failure = it))
        }


internal fun Flow<MediaSearchState>.filterByCollection(
    collectionIds: Flow<Long?>,
    memberships: Flow<CollectionMembershipState>,
): Flow<MediaSearchState> =
    combine(this, collectionIds, memberships) { search, collectionId, membership ->
        if (collectionId == null) search
        else {
            val ids = membership.memberships?.get(collectionId).orEmpty()
            search.copy(
                items = search.items.filter { it.id in ids },
                isLoading = search.isLoading ||
                    (membership.memberships == null && membership.failure == null),
                failure = search.failure ?: membership.failure,
            )
        }
    }.distinctUntilChanged()
