package com.suyziss.gallery.data

import android.content.Context
import java.io.DataInputStream
import java.io.DataOutputStream

class MediaCacheStore(context: Context) {
    private val file = java.io.File(context.filesDir, "media-index-v1.bin")

    fun read(): List<MediaItem> = synchronized(LOCK) { runCatching {
        if (!file.exists()) return emptyList()
        DataInputStream(file.inputStream().buffered()).use { input ->
            val count = input.readInt().coerceIn(0, MAX_ITEMS)
            buildList(count) {
                repeat(count) {
                    val id = input.readLong()
                    val uri = input.readUTF()
                    val name = input.readUTF()
                    val mime = input.readUTF().ifEmpty { null }
                    val date = input.readLong()
                    val size = input.readLong()
                    val duration = input.readLong()
                    add(MediaItem(id, uri, name, mime, date, size, duration))
                }
            }
        }
    }.getOrDefault(emptyList())

    fun write(items: List<MediaItem>) = synchronized(LOCK) {
        runCatching {
            val temporary = java.io.File(file.parentFile, file.name + ".tmp")
            DataOutputStream(temporary.outputStream().buffered()).use { output ->
                val limited = items.distinctBy { it.id }.take(MAX_ITEMS)
                output.writeInt(limited.size)
                limited.forEach { item ->
                    output.writeLong(item.id)
                    output.writeUTF(item.uri.toString())
                    output.writeUTF(item.name.take(4096))
                    output.writeUTF(item.mimeType.orEmpty().take(256))
                    output.writeLong(item.dateAddedSeconds)
                    output.writeLong(item.sizeBytes)
                    output.writeLong(item.durationMs)
                }
            }
            if (!temporary.renameTo(file)) {
                temporary.copyTo(file, overwrite = true)
                temporary.delete()
            }
        }
    }

    companion object {
        private const val MAX_ITEMS = 600
        private val LOCK = Any()
    }
}
