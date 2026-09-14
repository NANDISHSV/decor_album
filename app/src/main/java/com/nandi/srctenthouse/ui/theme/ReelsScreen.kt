package com.nandi.srctenthouse.ui

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.google.firebase.firestore.DocumentSnapshot
import com.nandi.srctenthouse.ads.InterstitialAdManager
import com.nandi.srctenthouse.data.LocalImageStore
import com.nandi.srctenthouse.data.Photo
import com.nandi.srctenthouse.data.PhotoRepository
import kotlinx.coroutines.launch

private val InkBackground = Color(0xFF14141F)
private val IvoryText = Color(0xFFF5EFE6)
private val MarigoldGold = Color(0xFFD4A24E)
private val MutedGoldBeige = Color(0xFFC9B79C)

private const val PREFETCH_AHEAD = 3
private const val PAGE_SIZE = 10L
private const val LOAD_MORE_THRESHOLD = 3

@Composable
fun ReelsScreen(eventId: String, targetOrder: Int? = null) {
    val repository = remember { PhotoRepository() }
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var lastDocument by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLastPage by remember { mutableStateOf(false) }
    var hasMoreBefore by remember { mutableStateOf(targetOrder != null) }
    var loadingFirstPage by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadingBefore by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun loadNextPage() {
        if (loadingMore || isLastPage) return
        loadingMore = true
        try {
            val page = repository.getPhotosPage(eventId, PAGE_SIZE, lastDocument)
            Log.d("PERF", "loadNextPage eventId=$eventId requested=$PAGE_SIZE got=${page.photos.size} isLastPage=${page.isLastPage} totalNow=${photos.size + page.photos.size}")
            photos = photos + page.photos
            lastDocument = page.lastDocument
            isLastPage = page.isLastPage
        } finally {
            loadingMore = false // always reset, even if cancelled mid-flight
        }
    }

    LaunchedEffect(eventId, targetOrder) {
        photos = emptyList()
        lastDocument = null
        isLastPage = false
        hasMoreBefore = targetOrder != null
        loadingFirstPage = true

        val firstPage = repository.getPhotosPage(eventId, PAGE_SIZE, null, startAtOrder = targetOrder)
        photos = firstPage.photos
        lastDocument = firstPage.lastDocument
        isLastPage = firstPage.isLastPage

        loadingFirstPage = false
    }

    if (loadingFirstPage) {
        Box(Modifier.fillMaxSize().background(InkBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MarigoldGold)
        }
        return
    }

    if (photos.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().background(InkBackground).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No photos found", color = MutedGoldBeige, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "This may be because you're offline and haven't viewed this event before",
                color = MutedGoldBeige,
                fontSize = 12.sp
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { photos.size })
    val activity = context as? Activity

    var lastSeenPage by remember { mutableStateOf(pagerState.currentPage) }
    var swipesSinceLastAd by remember { mutableStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != lastSeenPage) {
            lastSeenPage = pagerState.currentPage
            swipesSinceLastAd++
            if (swipesSinceLastAd >= 5) {
                swipesSinceLastAd = 0
                activity?.let { InterstitialAdManager.showIfAvailable(it) {} }
            }
        }
    }

    // Forward: load more once nearing the end of what's loaded.
    LaunchedEffect(pagerState.currentPage, photos.size, isLastPage) {
        if (!isLastPage && pagerState.currentPage >= photos.size - LOAD_MORE_THRESHOLD) {
            loadNextPage()
        }
    }

    // Backward: only relevant when we jumped in via search. Loads earlier photos
    // just before the current window, on demand, as the user nears the start.
    LaunchedEffect(pagerState.currentPage, photos, hasMoreBefore) {
        if (hasMoreBefore && !loadingBefore && pagerState.currentPage <= LOAD_MORE_THRESHOLD) {
            loadingBefore = true
            val firstOrder = photos.firstOrNull()?.order
            if (firstOrder != null) {
                val before = repository.getPhotosPageBefore(eventId, firstOrder, PAGE_SIZE)
                if (before.photos.isNotEmpty()) {
                    val prependCount = before.photos.size
                    photos = before.photos + photos
                    pagerState.scrollToPage(pagerState.currentPage + prependCount)
                }
                hasMoreBefore = before.hasMoreBefore
            }
            loadingBefore = false
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = PREFETCH_AHEAD,
        key = { page -> photos.getOrNull(page)?.id ?: page }
    ) { page ->
        val photo = photos[page]
        val fileName = "decor_${photo.id}.jpg"
        var downloaded by remember(photo.id) {
            mutableStateOf(LocalImageStore.isDownloaded(context, fileName))
        }
        var sharing by remember(photo.id) { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize().background(InkBackground)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.imageUrl)
                    .precision(Precision.INEXACT)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = photo.caption,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            )

            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
                    .clip(RoundedCornerShape(10.dp)).background(MarigoldGold)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("#${photo.displayNumber}", color = InkBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Text(
                text = "${page + 1} / ${photos.size}${if (!isLastPage) "+" else ""}",
                color = MutedGoldBeige,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 18.dp).fillMaxWidth(0.75f)
            ) {
                if (photo.caption.isNotBlank()) {
                    Text(photo.caption, color = IvoryText, fontFamily = FontFamily.Serif, fontSize = 17.sp)
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        if (!downloaded) {
                            scope.launch {
                                LocalImageStore.downloadImage(context, photo.imageUrl, fileName)
                                downloaded = true
                                Toast.makeText(context, "Saved — check Downloads", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.clip(CircleShape).background(if (downloaded) MarigoldGold else Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        if (downloaded) Icons.Filled.Check else Icons.Filled.Download,
                        contentDescription = if (downloaded) "Downloaded" else "Download",
                        tint = if (downloaded) InkBackground else Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(
                    onClick = {
                        if (!sharing) {
                            sharing = true
                            scope.launch {
                                try {
                                    val file = LocalImageStore.getShareableFile(context, photo.imageUrl, photo.id)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareText = buildString {
                                        append("Check out decor #${photo.displayNumber} on Decor Album!\n\n")
                                        append("Don't have the app? Get it here and search #${photo.displayNumber} to see it:\n")
                                        append("https://play.google.com/store/apps/details?id=com.nandi.srctenthouse")
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share photo"))
                                } catch (e: Exception) {
                                    Log.e("ShareDebug", "Share failed", e)
                                    Toast.makeText(context, "Couldn't share photo: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    sharing = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                ) {
                    if (sharing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Share, contentDescription = "Share photo", tint = Color.White)
                    }
                }
            }
        }
    }
}