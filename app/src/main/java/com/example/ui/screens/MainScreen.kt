package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val minThreshold by viewModel.minUsdThreshold.collectAsStateWithLifecycle()
    val excludeBtcEth by viewModel.excludeBtcEth.collectAsStateWithLifecycle()
    val isLiveStreaming by viewModel.isLiveStreaming.collectAsStateWithLifecycle()
    val soundAlerts by viewModel.soundAlerts.collectAsStateWithLifecycle()

    val liquidations by viewModel.liquidations.collectAsStateWithLifecycle()
    val rawAnalyses by viewModel.rawAnalyses.collectAsStateWithLifecycle()
    val filteredAnalyses by viewModel.filteredAnalyses.collectAsStateWithLifecycle()
    val marketStats by viewModel.marketStats.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val bannerAlert by viewModel.bannerAlert.collectAsStateWithLifecycle()

    val verifiedList = rawAnalyses.filter { it.status != com.example.data.model.PredictionStatus.PENDING }
    val hitCount = verifiedList.count { it.status == com.example.data.model.PredictionStatus.HIT }
    val hitRate = if (verifiedList.isNotEmpty()) (hitCount.toDouble() / verifiedList.size) * 100.0 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logo Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "CoinGlass Liq",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Short Takip & 1dk Yön Tahmini",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Hit Rate Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hitRate >= 50) NeonGreenBg else NeonRedBg,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (hitRate >= 50) NeonGreen else NeonRed
                            )
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = String.format("%%%.0f HİT", hitRate),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hitRate >= 50) NeonGreen else NeonRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Threshold Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricCyanBg,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "≥$${(minThreshold / 1000).toInt()}K",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurface,
                contentColor = TextPrimary,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "Canlı Akış") },
                    label = { Text("Canlı Akış", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyanBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_0")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "Yön Tahminleri") },
                    label = { Text("Tahminler", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyanBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_1")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "İstatistik") },
                    label = { Text("İstatistik", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyanBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_2")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar") },
                    label = { Text("Ayarlar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyanBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_3")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LiveFeedTab(
                    liquidations = liquidations,
                    marketStats = marketStats,
                    minThreshold = minThreshold,
                    excludeBtcEth = excludeBtcEth,
                    isLiveStreaming = isLiveStreaming,
                    onThresholdSelected = { viewModel.setMinUsdThreshold(it) },
                    onToggleExcludeBtcEth = { viewModel.setExcludeBtcEth(it) },
                    onToggleLiveStreaming = { viewModel.setLiveStreaming(it) },
                    onTriggerManualAnalysis = {
                        viewModel.triggerManualAnalysis(it)
                        viewModel.setSelectedTab(1)
                    }
                )

                1 -> PredictionsTab(
                    analyses = rawAnalyses,
                    onTriggerManualAnalysis = { viewModel.triggerManualAnalysis(it) }
                )

                2 -> AnalyticsTab(
                    marketStats = marketStats,
                    analyses = rawAnalyses
                )

                3 -> HistorySettingsTab(
                    analyses = filteredAnalyses,
                    searchQuery = searchQuery,
                    statusFilter = statusFilter,
                    minThreshold = minThreshold,
                    excludeBtcEth = excludeBtcEth,
                    isLiveStreaming = isLiveStreaming,
                    soundAlerts = soundAlerts,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onStatusFilterChange = { viewModel.setStatusFilter(it) },
                    onThresholdChange = { viewModel.setMinUsdThreshold(it) },
                    onToggleExcludeBtcEth = { viewModel.setExcludeBtcEth(it) },
                    onToggleLiveStreaming = { viewModel.setLiveStreaming(it) },
                    onToggleSoundAlerts = { viewModel.setSoundAlerts(it) },
                    onClearHistory = { viewModel.clearHistory() }
                )
            }

            // Instant Popup Trigger Banner
            bannerAlert?.let { (event, dirSym) ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeonRed),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.dismissBanner()
                                viewModel.setSelectedTab(1)
                            }
                            .testTag("trigger_popup_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Trigger",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "🔴 KISA TASFİYE TESPİT EDİLDİ!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${event.symbol} ($${String.format("%,.0f", event.volUsd)}) -> 1DK TAHMİN: $dirSym",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White
                                ) {
                                    Text(
                                        text = "GÖR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.dismissBanner() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
