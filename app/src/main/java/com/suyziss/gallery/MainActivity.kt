package com.suyziss.gallery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.suyziss.gallery.data.MediaIndexWorker
import com.suyziss.gallery.data.MediaItem
import com.suyziss.gallery.domain.MediaFilter
import com.suyziss.gallery.domain.MediaSort
import com.suyziss.gallery.ui.GalleryUiState
import com.suyziss.gallery.ui.GalleryViewModel
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.absoluteValue


@Composable
private fun GalleryTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = if (Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) darkColorScheme() else lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GalleryTheme { GalleryRoot() } }
    }
}

@Composable
private fun GalleryRoot() {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val galleryViewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModel.Factory(application)
    )
    val state by galleryViewModel.state.collectAsState()
    val navController = rememberNavController()
    var hasAccess by remember { mutableStateOf(hasMediaPermission(context)) }
    var partialAccess by remember { mutableStateOf(isPartialMediaAccess(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasAccess = hasMediaPermission(context)
        partialAccess = isPartialMediaAccess(context)
        if (hasAccess) {
            galleryViewModel.refresh()
            scheduleMediaIndex(context)
        }
    }

    LaunchedEffect(hasAccess) {
        if (hasAccess) scheduleMediaIndex(context)
    }

    val visibleMedia by remember(state.media, state.query, state.filter, state.sort) {
        derivedStateOf {
            galleryViewModel.visibleMedia(state)
        }
    }

    NavHost(navController = navController, startDestination = "gallery") {
        composable("gallery") {
            GalleryScreen(
                state = state,
                visibleMedia = visibleMedia,
                hasAccess = hasAccess,
                partialAccess = partialAccess,
                onRequestAccess = {
                    val permissions = when {
                        Build.VERSION.SDK_INT >= 34 -> arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        )
                        Build.VERSION.SDK_INT >= 33 -> arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                        )
                        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(permissions)
                },
                onQueryChange = galleryViewModel::setQuery,
                onFilterChange = galleryViewModel::setFilter,
                onSortChange = galleryViewModel::setSort,
                onRetry = galleryViewModel::refresh,
                onLoadMore = galleryViewModel::loadNextPage,
                onOpen = { item -> navController.navigate("viewer/${item.id}") }
            )
        }
        composable(
            route = "viewer/{mediaId}",
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { entry ->
            val mediaId = entry.arguments?.getLong("mediaId") ?: -1L
            val selected = state.media.firstOrNull { it.id == mediaId }
            if (selected == null) {
                EmptyState(
                    title = "Файл больше недоступен",
                    subtitle = "Возможно, он был удалён или доступ к нему отозван.",
                    action = "Назад",
                    onAction = { navController.popBackStack() }
                )
            } else {
                MediaViewerScreen(
                    item = selected,
                    neighbors = visibleMedia,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryScreen(
    state: GalleryUiState,
    visibleMedia: List<MediaItem>,
    hasAccess: Boolean,
    partialAccess: Boolean,
    onRequestAccess: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onSortChange: (MediaSort) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (MediaItem) -> Unit
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, visibleMedia.size, state.hasMore, state.isLoadingMore) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { lastVisible ->
                if (lastVisible != null && lastVisible >= visibleMedia.size - 8 && state.hasMore) {
                    onLoadMore()
                }
            }
    }

    LaunchedEffect(visibleMedia.size, state.media.size, state.query, state.filter, state.hasMore, state.isLoading, state.isLoadingMore) {
        if (hasAccess && visibleMedia.isEmpty() && state.media.isNotEmpty() &&
            state.hasMore && !state.isLoading && !state.isLoadingMore
        ) {
            onLoadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Suyziss Gallery", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Локальная медиатека",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Поиск по названию") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Поиск") },
                shape = RoundedCornerShape(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (state.isLoading && state.media.isEmpty()) "Загрузка…" else if (state.hasMore) "Показано: ${visibleMedia.size}" else "Найдено: ${visibleMedia.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SortMenu(current = state.sort, onSelected = onSortChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filter == MediaFilter.ALL,
                    onClick = { onFilterChange(MediaFilter.ALL) },
                    label = { Text("Всё") }
                )
                FilterChip(
                    selected = state.filter == MediaFilter.PHOTOS,
                    onClick = { onFilterChange(MediaFilter.PHOTOS) },
                    label = { Text("Только фото") }
                )
                FilterChip(
                    selected = state.filter == MediaFilter.VIDEOS,
                    onClick = { onFilterChange(MediaFilter.VIDEOS) },
                    label = { Text("Только видео") }
                )
            }

            if (partialAccess && hasAccess) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        "Вы выбрали только часть фото и видео. Чтобы увидеть остальные файлы, измените доступ в настройках разрешений.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (state.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.error, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = onRetry) { Text("Повторить") }
                    }
                }
            }

            when {
                !hasAccess -> EmptyState(
                    title = "Разрешите доступ к медиатеке",
                    subtitle = "Можно выбрать все фото и видео или предоставить доступ только к отдельным файлам.",
                    action = "Выбрать доступ",
                    onAction = onRequestAccess
                )
                state.isLoading && state.media.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                state.media.isEmpty() && state.error == null -> EmptyState(
                    title = "Медиатека пуста",
                    subtitle = "На устройстве не найдено доступных фотографий или видео."
                )
                visibleMedia.isEmpty() && state.isLoadingMore -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                visibleMedia.isEmpty() -> EmptyState(
                    title = if (state.query.isNotBlank()) "Ничего не найдено по запросу «${state.query}»" else "Нет файлов этого типа",
                    subtitle = if (state.query.isNotBlank()) "Попробуйте изменить поисковый запрос." else "Выберите другой фильтр."
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    state = gridState,
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleMedia, key = { it.id }) { item ->
                        MediaThumbnail(item = item, onClick = { onOpen(item) })
                    }
                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(current: MediaSort, onSelected: (MediaSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (current) {
        MediaSort.DATE_NEWEST -> "По дате"
        MediaSort.NAME_ASCENDING -> "По имени"
        MediaSort.SIZE_LARGEST -> "По размеру"
    }
    Box {
        androidx.compose.material3.TextButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.Sort, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                MediaSort.DATE_NEWEST to "По дате",
                MediaSort.NAME_ASCENDING to "По имени",
                MediaSort.SIZE_LARGEST to "По размеру"
            ).forEach { (sort, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(sort)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MediaThumbnail(item: MediaItem, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageRequest = remember(item.id, context) {
        ImageRequest.Builder(context).data(Uri.parse(item.uri))
            .apply { if (item.isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
            .build()
    }
    val painter = rememberAsyncImagePainter(model = imageRequest)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
    ) {
        if (painter.state !is coil.compose.AsyncImagePainter.State.Success) {
            ShimmerPlaceholder(Modifier.fillMaxSize())
        }
        Image(
            painter = painter,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.VideoLibrary,
                    contentDescription = "Видео",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    formatDuration(item.durationMs),
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thumbnail-shimmer")
    val shift by transition.animateFloat(
        initialValue = -320f,
        targetValue = 520f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-shift"
    )
    val colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant
    )
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = colors,
                start = Offset(shift, 0f),
                end = Offset(shift + 260f, 260f)
            )
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaViewerScreen(
    item: MediaItem,
    neighbors: List<MediaItem>,
    onBack: () -> Unit
) {
    val pages = remember(item.id, neighbors) {
        if (neighbors.isEmpty()) listOf(item) else neighbors
    }
    val initialPage = remember(item.id, pages) {
        pages.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pages.size }
    )
    val currentItem = pages.getOrNull(pagerState.currentPage) ?: item
    var currentPageZoomed by remember(currentItem.id) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentItem.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (pages.isNotEmpty()) "${pagerState.currentPage + 1} из ${pages.size}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                userScrollEnabled = !currentPageZoomed,
                key = { page -> pages[page].id },
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val pageItem = pages[page]
                val pageOffset = (
                    pagerState.currentPage - page + pagerState.currentPageOffsetFraction
                ).absoluteValue.coerceIn(0f, 1f)
                ViewerMediaPage(
                    item = pageItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - (0.12f * pageOffset)
                            val pageScale = 1f - (0.035f * pageOffset)
                            scaleX = pageScale
                            scaleY = pageScale
                        },
                    onZoomChanged = { zoomed ->
                        if (pagerState.currentPage == page) {
                            currentPageZoomed = zoomed
                        }
                    }
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${formatBytes(currentItem.sizeBytes)} • ${currentItem.mimeType ?: "Файл"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${pagerState.currentPage + 1} / ${pages.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ViewerMediaPage(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onZoomChanged: (Boolean) -> Unit
) {
    if (item.isVideo) {
        VideoPlayer(
            item = item,
            modifier = modifier.background(androidx.compose.ui.graphics.Color.Black)
        )
    } else {
        var scale by remember(item.id) { mutableFloatStateOf(1f) }
        var translationX by remember(item.id) { mutableFloatStateOf(0f) }
        var translationY by remember(item.id) { mutableFloatStateOf(0f) }
        val transformState = rememberTransformableState { zoom, pan, _ ->
            scale = (scale * zoom).coerceIn(1f, 6f)
            translationX += pan.x
            translationY += pan.y
            if (scale <= 1.001f) {
                scale = 1f
                translationX = 0f
                translationY = 0f
            }
            onZoomChanged(scale > 1.001f)
        }
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .clip(RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(Uri.parse(item.uri)),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.translationX = translationX
                        this.translationY = translationY
                    }
                    .transformable(
                        state = transformState,
                        canPan = { scale > 1.001f }
                    )
            )
        }
    }
}

@Composable
private fun VideoPlayer(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(Uri.parse(item.uri)))
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = modifier.background(androidx.compose.ui.graphics.Color.Black),
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { it.player = player }
    )
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (action != null && onAction != null) {
                Spacer(Modifier.height(18.dp))
                Button(onClick = onAction) { Text(action) }
            }
        }
    }
}

private fun hasMediaPermission(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= 34) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else if (Build.VERSION.SDK_INT >= 33) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return permissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun isPartialMediaAccess(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 34) return false
    val selectedPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    ) == PackageManager.PERMISSION_GRANTED
    val fullImages = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_IMAGES
    ) == PackageManager.PERMISSION_GRANTED
    val fullVideos = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_VIDEO
    ) == PackageManager.PERMISSION_GRANTED
    return selectedPermission && (!fullImages || !fullVideos)
}

private fun scheduleMediaIndex(context: Context) {
    val workManager = WorkManager.getInstance(context)
    workManager.enqueueUniqueWork(
        "suyziss-gallery-index-now",
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<MediaIndexWorker>().build()
    )
    val periodic = PeriodicWorkRequestBuilder<MediaIndexWorker>(12, TimeUnit.HOURS).build()
    workManager.enqueueUniquePeriodicWork(
        "suyziss-gallery-index-periodic",
        ExistingPeriodicWorkPolicy.KEEP,
        periodic
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
