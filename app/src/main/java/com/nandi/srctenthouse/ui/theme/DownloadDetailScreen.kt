package com.nandi.srctenthouse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nandi.srctenthouse.data.LocalImageStore
import kotlinx.coroutines.launch
import java.io.File

private val InkBackground = Color(0xFF14141F)
private val MarigoldGold = Color(0xFFD4A24E)

@Composable
fun DownloadDetailScreen(startIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf(LocalImageStore.listDownloadedImages(context)) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    if (files.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, files.size - 1),
        pageCount = { files.size }
    )
    val scope = rememberCoroutineScope()

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Remove from Downloads?") },
            text = { Text("This only removes it from your device, not from the app's photo collection.") },
            confirmButton = {
                TextButton(onClick = {
                    LocalImageStore.deleteImage(file)
                    val currentPage = pagerState.currentPage
                    files = LocalImageStore.listDownloadedImages(context)
                    fileToDelete = null
                    if (files.isEmpty()) {
                        onBack()
                    } else {
                        scope.launch {
                            pagerState.scrollToPage(currentPage.coerceIn(0, files.size - 1))
                        }
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(InkBackground)) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val file = files.getOrNull(page) ?: return@VerticalPager
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = file,
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        IconButton(
            onClick = { files.getOrNull(pagerState.currentPage)?.let { fileToDelete = it } },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MarigoldGold)
        }
    }
}