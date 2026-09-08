package com.nandi.srctenthouse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import coil.compose.AsyncImage
import com.nandi.srctenthouse.data.LocalImageStore
import com.nandi.srctenthouse.data.Photo
import com.nandi.srctenthouse.data.PhotoRepository
import kotlinx.coroutines.launch
import android.widget.Toast

private val InkBackground = Color(0xFF14141F)
private val IvoryText = Color(0xFFF5EFE6)
private val MarigoldGold = Color(0xFFD4A24E)
private val MutedGoldBeige = Color(0xFFC9B79C)

@Composable
fun ReelsScreen(eventId: String) {
    val repository = remember { PhotoRepository() }
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(eventId) {
        photos = repository.getPhotosForEvent(eventId)
        loading = false
    }

    if (loading) {
        Box(
            Modifier.fillMaxSize().background(InkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MarigoldGold)
        }
        return
    }

    if (photos.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(InkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("No photos in this event yet", color = MutedGoldBeige)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { photos.size })

    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val photo = photos[page]
        val fileName = "decor_${photo.id}.jpg"
        var downloaded by remember(photo.id) {
            mutableStateOf(LocalImageStore.isDownloaded(context, fileName))
        }

        Box(modifier = Modifier.fillMaxSize().background(InkBackground)) {
            AsyncImage(
                model = photo.imageUrl,
                contentDescription = photo.caption,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top scrim, keeps the number badge legible over any photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            )

            // Bottom scrim, keeps caption + actions legible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            // Reference number badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MarigoldGold)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "#${photo.displayNumber}",
                    color = InkBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Position indicator (e.g. 3 / 12)
            Text(
                text = "${page + 1} / ${photos.size}",
                color = MutedGoldBeige,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .fillMaxWidth(0.75f)
            ) {
                if (photo.caption.isNotBlank()) {
                    Text(
                        text = photo.caption,
                        color = IvoryText,
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
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
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (downloaded) MarigoldGold else Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (downloaded) Icons.Filled.Check else Icons.Filled.Download,
                        contentDescription = if (downloaded) "Downloaded" else "Download",
                        tint = if (downloaded) InkBackground else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Check out decor #${photo.displayNumber}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share reference"))
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share reference", tint = Color.White)
                }
            }
        }
    }
}