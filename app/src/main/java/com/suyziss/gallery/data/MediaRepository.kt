package com.suyziss.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore

data class MediaItem(
    val id: Long,
    val uri: String,
    val name: String,
    val mimeType: String?,
    val dateAddedSeconds: Long,
    val sizeBytes: Long,
    val durationMs: Long
) {
    val isVideo: Boolean get() = mimeType?.startsWith("video/") == true
}

data class MediaCursor(val dateAddedSeconds: Long, val id: Long)

data class MediaPage(
    val items: List<MediaItem>,
    val nextCursor: MediaCursor?,
    val hasMore: Boolean
)

interface MediaRepository {
    suspend fun loadPage(cursor: MediaCursor?, pageSize: Int = 80): MediaPage
}

class MediaStoreRepository(private val resolver: ContentResolver) : MediaRepository {
    override suspend fun loadPage(cursor: MediaCursor?, pageSize: Int): MediaPage {
        require(pageSize in 1..500) { "pageSize must be between 1 and 500" }
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.VideoColumns.DURATION
        )
        val mediaType = MediaStore.Files.FileColumns.MEDIA_TYPE
        val dateAdded = MediaStore.Files.FileColumns.DATE_ADDED
        val idColumn = MediaStore.Files.FileColumns._ID
        val selection = buildString {
            append("$mediaType IN (?, ?)")
            if (cursor != null) {
                append(" AND ($dateAdded < ? OR ($dateAdded = ? AND $idColumn < ?))")
            }
        }
        val selectionArgs = buildList {
            add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            if (cursor != null) {
                add(cursor.dateAddedSeconds.toString())
                add(cursor.dateAddedSeconds.toString())
                add(cursor.id.toString())
            }
        }
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs.toTypedArray())
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "$dateAdded DESC, $idColumn DESC"
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize + 1)
        }

        val rows = mutableListOf<MediaItem>()
        resolver.query(collection, projection, args, null)?.use { result ->
            val idIndex = result.getColumnIndexOrThrow(idColumn)
            val nameIndex = result.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeIndex = result.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateIndex = result.getColumnIndexOrThrow(dateAdded)
            val sizeIndex = result.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val durationIndex = result.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

            while (result.moveToNext() && rows.size <= pageSize) {
                val id = result.getLong(idIndex)
                rows += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    name = result.getString(nameIndex) ?: "Без названия",
                    mimeType = if (mimeIndex >= 0 && !result.isNull(mimeIndex)) result.getString(mimeIndex) else null,
                    dateAddedSeconds = result.getLong(dateIndex),
                    sizeBytes = if (result.isNull(sizeIndex)) 0L else result.getLong(sizeIndex),
                    durationMs = if (durationIndex >= 0 && !result.isNull(durationIndex)) result.getLong(durationIndex) else 0L
                )
            }
        } ?: throw IllegalStateException("MediaStore не вернул курсор.")

        val hasMore = rows.size > pageSize
        val pageItems = rows.take(pageSize)
        val last = pageItems.lastOrNull()
        val next = if (hasMore && last != null) MediaCursor(last.dateAddedSeconds, last.id) else null
        return MediaPage(pageItems, next, hasMore)
    }
}
