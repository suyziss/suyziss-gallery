package com.suyziss.gallery.domain

import com.suyziss.gallery.data.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterMediaUseCaseTest {
    private val useCase = FilterMediaUseCase()
    private val fixtures = listOf(
        MediaItem(1, "content://media/1", "zebra.jpg", "image/jpeg", 100, 900, 0),
        MediaItem(2, "content://media/2", "clip.mp4", "video/mp4", 300, 5000, 12500),
        MediaItem(3, "content://media/3", "Alpha.png", "image/png", 200, 1200, 0)
    )

    @Test
    fun filtersOnlyPhotos() {
        val result = useCase(fixtures, "", MediaFilter.PHOTOS, MediaSort.DATE_NEWEST)
        assertEquals(listOf(3L, 1L), result.map { it.id })
    }

    @Test
    fun filtersOnlyVideos() {
        val result = useCase(fixtures, "", MediaFilter.VIDEOS, MediaSort.DATE_NEWEST)
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun searchesNamesIgnoringCase() {
        val result = useCase(fixtures, "ALPHA", MediaFilter.ALL, MediaSort.DATE_NEWEST)
        assertEquals(listOf("Alpha.png"), result.map { it.name })
    }

    @Test
    fun sortsByNameAndSize() {
        val byName = useCase(fixtures, "", MediaFilter.ALL, MediaSort.NAME_ASCENDING)
        val bySize = useCase(fixtures, "", MediaFilter.ALL, MediaSort.SIZE_LARGEST)
        assertEquals(listOf("Alpha.png", "clip.mp4", "zebra.jpg"), byName.map { it.name })
        assertEquals(listOf(2L, 3L, 1L), bySize.map { it.id })
    }
}
