package com.farywave.memehive.ui.use_case.media_item_search

data class SearchResult(
    val prefixMatches: Set<Long>,
    val containsMatches: Set<Long>,
    val tagMatches: Set<Long>,
    val trigramMatches: Set<Long>,
)