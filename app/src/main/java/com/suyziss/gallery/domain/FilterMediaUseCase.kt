package com.suyziss.gallery.domain

import com.suyziss.gallery.data.MediaItem

enum class MediaFilter { ALL, PHOTOS, VIDEOS }
enum class MediaSort { DATE_NEWEST, NAME_ASCENDING, SIZE_LARGEST }

class FilterMediaUseCase {
    operator fun invoke(
        media: List<MediaItem>,
        query: String,
        filter: MediaFilter,
        sort: MediaSort
    ): List<MediaItem> {
        val normalizedQuery = query.trim()
        val filtered = media.asSequence()
            .filter { item ->
                when (filter) {
                    MediaFilter.ALL -> true
                    MediaFilter.PHOTOS -> !item.isVideo
                    MediaFilter.VIDEOS -> item.isVideo
                }
            }
            .filter { normalizedQuery.isEmpty() || it.name.contains(normalizedQuery, ignoreCase = true) }
            .toList()

        return when (sort) {
            MediaSort.DATE_NEWEST -> filtered.sortedWith(
                compareByDescending<MediaItem> { it.dateAddedSeconds }.thenByDescending { it.id }
            )
            MediaSort.NAME_ASCENDING -> filtered.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
            MediaSort.SIZE_LARGEST -> filtered.sortedWith(
                compareByDescending<MediaItem> { it.sizeBytes }.thenBy { it.name.lowercase() }
            )
        }
    }
}
