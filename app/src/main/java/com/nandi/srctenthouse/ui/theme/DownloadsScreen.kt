package com.nandi.srctenthouse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import java.io.File

private val InkBackground = Color(0xFF14141F)
private val MutedGoldBeige = Color(0xFFC9B79C)

@Composable
fun DownloadsScreen(onImageClick: (Int) -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) {
        files = LocalImageStore.listDownloadedImages(context)
    }

    Box(modifier = Modifier.fillMaxSize().background(InkBackground)) {
        if (files.isEmpty()) {
            Text(
                "No downloads yet",
                color = MutedGoldBeige,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp)
        ) {
            itemsIndexed(files) { index, file ->
                AsyncImage(
                    model = file,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onImageClick(index) }
                )
            }
        }
    }
}