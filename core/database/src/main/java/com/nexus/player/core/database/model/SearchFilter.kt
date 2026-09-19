package com.nexus.player.core.database.model

/**
 * Filter specification for video search queries.
 * Designed to be extensible for future filters (resolution, duration ranges, folder paths, etc.).
 */
data class SearchFilter(
    val query: String = "",
    val resolution: String? = null,
    val minDurationMs: Long? = null,
    val maxDurationMs: Long? = null,
    val folderPath: String? = null,
    val sortOrder: SearchSortOrder = SearchSortOrder.RELEVANCE
)

/**
 * Sort order options for search results.
 */
enum class SearchSortOrder {
    RELEVANCE,
    DATE_ADDED_DESC,
    TITLE_ASC,
    DURATION_DESC,
    SIZE_DESC
}
