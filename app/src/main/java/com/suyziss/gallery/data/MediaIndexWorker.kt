package com.suyziss.gallery.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MediaIndexWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        val repository = MediaStoreRepository(applicationContext.contentResolver)
        val items = mutableListOf<MediaItem>()
        var cursor: MediaCursor? = null
        var hasMore = true
        while (hasMore && items.size < 600 && !isStopped) {
            val page = repository.loadPage(cursor, 100)
            items += page.items
            cursor = page.nextCursor
            hasMore = page.hasMore
            if (page.items.isEmpty()) break
        }
        if (!isStopped) MediaCacheStore(applicationContext).write(items)
        Result.success()
    } catch (_: SecurityException) {
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
