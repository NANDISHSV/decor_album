package com.nandi.srctenthouse.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nandi.srctenthouse.data.EventCategory
import com.nandi.srctenthouse.data.PhotoRepository
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

// ---- Signature palette (decor / marigold-and-brass) ----
private val InkBackground = Color(0xFF14141F)
private val IvoryText = Color(0xFFF5EFE6)
private val MarigoldGold = Color(0xFFD4A24E)
private val MutedGoldBeige = Color(0xFFC9B79C)

@Composable
fun CategoryListScreen(onCategoryClick: (EventCategory) -> Unit) {
    val repository = remember { PhotoRepository() }
    var categories by remember { mutableStateOf<List<EventCategory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        categories = repository.getCategories()
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBackground)
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MarigoldGold,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        if (categories.isEmpty()) {
            Text(
                text = "No events yet",
                color = MutedGoldBeige,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(categories) { index, category ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(category.id) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, delayMillis = index * 60)) +
                            slideInVertically(tween(400, delayMillis = index * 60)) { it / 6 }
                ) {
                    EventCard(category = category, onClick = { onCategoryClick(category) })
                }
            }
        }
    }
}

@Composable
private fun EventCard(category: EventCategory, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = category.coverImage,
            contentDescription = category.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Scrim so text stays legible over any photo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY = 100f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = category.name,
                color = IvoryText,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Signature element: marigold garland dots
            GarlandDivider()
        }
    }
}

@Composable
private fun GarlandDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(7) { i ->
            Box(
                modifier = Modifier
                    .size(if (i % 2 == 0) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(MarigoldGold.copy(alpha = 0.9f))
            )
            if (i != 6) Spacer(modifier = Modifier.width(6.dp))
        }
    }
}