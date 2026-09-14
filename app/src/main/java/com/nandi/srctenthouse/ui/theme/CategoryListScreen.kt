package com.nandi.srctenthouse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import coil.compose.AsyncImage
import com.nandi.srctenthouse.BuildConfig
import com.nandi.srctenthouse.data.AppConfigRepository
import com.nandi.srctenthouse.data.EventCategory
import com.nandi.srctenthouse.data.NetworkObserver
import com.nandi.srctenthouse.data.PhotoRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val InkBackground = Color(0xFF14141F)
private val IvoryText = Color(0xFFF5EFE6)
private val MarigoldGold = Color(0xFFD4A24E)
private val MutedGoldBeige = Color(0xFFC9B79C)
private const val PREFS_NAME = "decor_album_prefs"
private const val KEY_DISMISSED_UPDATE_VERSION = "dismissed_update_version"

@Composable
fun CategoryListScreen(
    onCategoryClick: (EventCategory) -> Unit,
    onPhotoFound: (eventId: String, order: Int) -> Unit
) {
    val repository = remember { PhotoRepository() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    var categories by remember { mutableStateOf<List<EventCategory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    var showUpdateBanner by remember { mutableStateOf(false) }
    var softUpdateMessage by remember { mutableStateOf("") }
    var latestVersionForBanner by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        NetworkObserver.isOnlineFlow(context).collectLatest { online -> isOnline = online }
    }

    LaunchedEffect(Unit) {
        val config = AppConfigRepository().getUpdateConfig() ?: return@LaunchedEffect
        val dismissedVersion = prefs.getLong(KEY_DISMISSED_UPDATE_VERSION, 0)
        if (config.latestVersionCode > BuildConfig.VERSION_CODE &&
            config.latestVersionCode > dismissedVersion
        ) {
            softUpdateMessage = config.softMessage
            latestVersionForBanner = config.latestVersionCode
            showUpdateBanner = true
        }
    }

    LaunchedEffect(reloadKey) {
        loading = true
        loadFailed = false
        try {
            repository.getCategoriesFlow().collectLatest { result ->
                categories = result
                loading = false
            }
        } catch (e: Exception) {
            loading = false
            loadFailed = true
        }
    }

    fun runSearch() {
        val number = searchText.trim().removePrefix("#").toLongOrNull()
        if (number == null) {
            Toast.makeText(context, "Enter a valid number", Toast.LENGTH_SHORT).show()
            return
        }
        searching = true
        scope.launch {
            val photo = repository.findPhotoByDisplayNumber(number)
            searching = false
            if (photo != null) {
                onPhotoFound(photo.eventId, photo.order)
            } else {
                Toast.makeText(context, "No photo found with #$number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(InkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showUpdateBanner) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MarigoldGold)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = InkBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        softUpdateMessage,
                        color = InkBackground,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.nandi.srctenthouse")
                        )
                        context.startActivity(intent)
                    }) {
                        Text("Update", color = InkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(
                        onClick = {
                            prefs.edit().putLong(KEY_DISMISSED_UPDATE_VERSION, latestVersionForBanner).apply()
                            showUpdateBanner = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = InkBackground, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (!isOnline && categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A3D))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MutedGoldBeige, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("You're offline — showing saved content", color = MutedGoldBeige, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search by number, e.g. 47", color = MutedGoldBeige) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IvoryText,
                        unfocusedTextColor = IvoryText,
                        focusedBorderColor = MarigoldGold,
                        unfocusedBorderColor = MutedGoldBeige,
                        cursorColor = MarigoldGold
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { runSearch() },
                    enabled = !searching,
                    modifier = Modifier.clip(CircleShape).background(MarigoldGold)
                ) {
                    if (searching) {
                        CircularProgressIndicator(color = InkBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = InkBackground)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    loading -> {
                        CircularProgressIndicator(color = MarigoldGold, modifier = Modifier.align(Alignment.Center))
                    }
                    loadFailed -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MutedGoldBeige, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No internet connection", color = IvoryText, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Connect to the internet to browse events", color = MutedGoldBeige, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { reloadKey++ },
                                colors = ButtonDefaults.buttonColors(containerColor = MarigoldGold, contentColor = InkBackground)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    categories.isEmpty() -> {
                        Text("No events yet", color = MutedGoldBeige, modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            itemsIndexed(
                                items = categories,
                                key = { _, category -> category.id }
                            ) { index, category ->
                                var visible by remember(category.id) { mutableStateOf(false) }
                                LaunchedEffect(category.id) { visible = true }

                                androidx.compose.animation.AnimatedVisibility(
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