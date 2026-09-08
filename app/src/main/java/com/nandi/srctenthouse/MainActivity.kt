package com.nandi.srctenthouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.nandi.srctenthouse.ads.BannerAdView
import com.nandi.srctenthouse.ads.InterstitialAdManager
import com.nandi.srctenthouse.ui.CategoryListScreen
import com.nandi.srctenthouse.ui.DownloadDetailScreen
import com.nandi.srctenthouse.ui.DownloadsScreen
import com.nandi.srctenthouse.ui.ReelsScreen

private val InkBackground = Color(0xFF14141F)
private val IvoryText = Color(0xFFF5EFE6)
private val MarigoldGold = Color(0xFFD4A24E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.google.android.gms.ads.MobileAds.initialize(this) {
            InterstitialAdManager.preload(this)
        }

        setContent {
            val darkColors = darkColorScheme(
                background = InkBackground,
                surface = InkBackground,
                primary = MarigoldGold,
                onPrimary = InkBackground,
                onBackground = IvoryText,
                onSurface = IvoryText
            )

            MaterialTheme(colorScheme = darkColors) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    containerColor = InkBackground,
                    bottomBar = {
                        Column (modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                            if (currentRoute == "categories" || currentRoute == "downloads") {
                                NavigationBar(containerColor = Color(0xFF1E1E2E),windowInsets = WindowInsets(0, 0, 0, 0)) {
                                    NavigationBarItem(
                                        selected = currentRoute == "categories",
                                        onClick = {
                                            navController.navigate("categories") {
                                                popUpTo(navController.graph.findStartDestination().id)
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = { Icon(Icons.Filled.Home, contentDescription = "Browse") },
                                        label = { Text("Browse") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MarigoldGold,
                                            selectedTextColor = MarigoldGold,
                                            indicatorColor = Color(0xFF2A2A3D)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "downloads",
                                        onClick = {
                                            navController.navigate("downloads") {
                                                popUpTo(navController.graph.findStartDestination().id)
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = { Icon(Icons.Filled.Download, contentDescription = "Downloads") },
                                        label = { Text("Downloads") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MarigoldGold,
                                            selectedTextColor = MarigoldGold,
                                            indicatorColor = Color(0xFF2A2A3D)
                                        )
                                    )
                                }
                            }

                            BannerAdView(modifier = Modifier.fillMaxWidth())
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "categories",
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    ) {
                        composable("categories") {
                            CategoryListScreen(onCategoryClick = { category ->
                                InterstitialAdManager.showIfAvailable(this@MainActivity) {
                                    navController.navigate("photos/${category.id}")
                                }
                            })
                        }
                        composable(
                            "photos/{eventId}",
                            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                            ReelsScreen(eventId = eventId)
                        }
                        composable("downloads") {
                            DownloadsScreen(onImageClick = { index ->
                                navController.navigate("downloadDetail/$index")
                            })
                        }
                        composable(
                            "downloadDetail/{index}",
                            arguments = listOf(navArgument("index") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val index = backStackEntry.arguments?.getInt("index") ?: 0
                            DownloadDetailScreen(startIndex = index, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}