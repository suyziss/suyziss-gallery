package com.suyziss.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suyziss.gallery.data.MediaCacheStore
import com.suyziss.gallery.data.MediaCursor
import com.suyziss.gallery.data.MediaItem
import com.suyziss.gallery.data.MediaStoreRepository
import com.suyziss.gallery.domain.FilterMediaUseCase
import com.suyziss.gallery.domain.MediaFilter
import com.suyziss.gallery.domain.MediaSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryUiState(
    val media: List<MediaItem> = emptyList(),
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    val sort: MediaSort = MediaSort.DATE_NEWEST,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application.contentResolver)
    private val cache = MediaCacheStore(application)
    private val filterMedia = FilterMediaUseCase()
    private val mutableState = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = mutableState.asStateFlow()

    private var nextCursor: MediaCursor? = null
    private var requestInFlight = false

    init { refresh() }

    fun setQuery(value: String) { mutableState.update { it.copy(query = value) } }
    fun setFilter(value: MediaFilter) { mutableState.update { it.copy(filter = value) } }
    fun setSort(value: MediaSort) { mutableState.update { it.copy(sort = value) } }

    fun visibleMedia(current: GalleryUiState = state.value): List<MediaItem> =
        filterMedia(current.media, current.query, current.filter, current.sort)

    fun findMedia(id: Long): MediaItem? = state.value.media.firstOrNull { it.id == id }

    fun refresh() {
        if (requestInFlight) return
        viewModelScope.launch {
            requestInFlight = true
            mutableState.update { it.copy(isLoading = true, error = null, hasMore = true) }
            try {
                val cached = withContext(Dispatchers.IO) { cache.read() }
                if (cached.isNotEmpty()) {
                    mutableState.update { it.copy(media = cached) }
                }
                val firstPage = repository.loadPage(null, PAGE_SIZE)
                nextCursor = firstPage.nextCursor
                mutableState.update {
                    it.copy(
                        media = firstPage.items,
                        isLoading = false,
                        hasMore = firstPage.hasMore,
                        error = null
                    )
                }
                withContext(Dispatchers.IO) { cache.write(firstPage.items) }
            } catch (security: SecurityException) {
                mutableState.update { it.copy(isLoading = false, error = "Нет доступа к выбранным фото и видео. Проверьте разрешения.") }
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(isLoading = false, error = "Не удалось прочитать медиатеку: ${error.localizedMessage ?: "неизвестная ошибка"}")
                }
            } finally {
                requestInFlight = false
            }
        }
    }

    fun loadNextPage() {
        val current = state.value
        if (requestInFlight || !current.hasMore || current.isLoadingMore || current.isLoading) return
        viewModelScope.launch {
            requestInFlight = true
            mutableState.update { it.copy(isLoadingMore = true, error = null) }
            try {
                val page = repository.loadPage(nextCursor, PAGE_SIZE)
                nextCursor = page.nextCursor
                mutableState.update { old ->
                    old.copy(
                        media = (old.media + page.items).distinctBy { it.id },
                        isLoadingMore = false,
                        hasMore = page.hasMore
                    )
                }
                val snapshot = state.value.media.take(CACHE_LIMIT)
                withContext(Dispatchers.IO) { cache.write(snapshot) }
            } catch (security: SecurityException) {
                mutableState.update { it.copy(isLoadingMore = false, error = "Доступ к медиатеке изменился. Обновите разрешения.") }
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(isLoadingMore = false, error = "Ошибка загрузки следующей страницы: ${error.localizedMessage ?: "неизвестная ошибка"}")
                }
            } finally {
                requestInFlight = false
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
                return GalleryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val PAGE_SIZE = 80
        private const val CACHE_LIMIT = 600
    }
}
