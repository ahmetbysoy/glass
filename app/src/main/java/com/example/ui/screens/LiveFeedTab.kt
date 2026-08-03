package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiquidationEvent
import com.example.data.model.LiquidationSide
import com.example.data.model.MarketStats
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveFeedTab(
    liquidations: List<LiquidationEvent>,
    marketStats: MarketStats,
    minThreshold: Double,
    excludeBtcEth: Boolean,
    isLiveStreaming: Boolean,
    onThresholdSelected: (Double) -> Unit,
    onToggleExcludeBtcEth: (Boolean) -> Unit,
    onToggleLiveStreaming: (Boolean) -> Unit,
    onTriggerManualAnalysis: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Live Header Banner with Short vs Long Ratio
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_summary_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isLiveStreaming) NeonGreen else NeonRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLiveStreaming) "CANLI AKIŞ AKTİF" else "AKIŞ DURDURULDU",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveStreaming) NeonGreen else NeonRed,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(
                        onClick = { onToggleLiveStreaming(!isLiveStreaming) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_stream_button")
                    ) {
                        Icon(
                            imageVector = if (isLiveStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Stream",
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Kısa Tasfiye (Short)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "$${String.format("%,.0f", marketStats.totalShortUsd)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Uzun Tasfiye (Long)", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "$${String.format("%,.0f", marketStats.totalLongUsd)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Short vs Long Volume Bar
                val totalVol = maxOf(1.0, marketStats.totalShortUsd + marketStats.totalLongUsd)
                val shortRatio = (marketStats.totalShortUsd / totalVol).toFloat().coerceIn(0.05f, 0.95f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SlateBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(shortRatio)
                            .fillMaxHeight()
                            .background(NeonRed)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - shortRatio)
                            .fillMaxHeight()
                            .background(NeonGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Threshold Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tetikleyici Eşik:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1000.0, 5000.0, 10000.0, 25000.0).forEach { threshold ->
                    val isSelected = minThreshold == threshold
                    val label = when (threshold) {
                        1000.0 -> "$1K"
                        5000.0 -> "$5K"
                        10000.0 -> "$10K"
                        25000.0 -> "$25K"
                        else -> "$${threshold.toInt()}"
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onThresholdSelected(threshold) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyanBg,
                            selectedLabelColor = ElectricCyan,
                            containerColor = SlateCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = SlateBorder,
                            selectedBorderColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("threshold_chip_$label")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Canlı Likidasyon Akışı (${liquidations.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExcludeBtcEth(!excludeBtcEth) }
                    .background(if (excludeBtcEth) ElectricCyanBg else SlateCard)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("exclude_btc_eth_chip")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (excludeBtcEth) ElectricCyan else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (excludeBtcEth) "Sadece Altcoinler" else "BTC/ETH Dahil",
                    fontSize = 11.sp,
                    color = if (excludeBtcEth) ElectricCyan else TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Liquidations List
        if (liquidations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "No data",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Canlı likidasyon verisi bekleniyor...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(liquidations, key = { it.id }) { item ->
                    LiquidationItemRow(
                        item = item,
                        minThreshold = minThreshold,
                        dateFormat = dateFormat,
                        onTriggerManualAnalysis = onTriggerManualAnalysis
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidationItemRow(
    item: LiquidationEvent,
    minThreshold: Double,
    dateFormat: SimpleDateFormat,
    onTriggerManualAnalysis: (String) -> Unit
) {
    val isShort = item.side == LiquidationSide.KISA
    val isHighValue = isShort && item.volUsd >= minThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("liquidation_row_${item.symbol}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighValue) NeonRedBg else SlateCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isHighValue) NeonRed else SlateBorder
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Side Icon Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isShort) NeonRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isShort) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = item.side.name,
                        tint = if (isShort) NeonRed else NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.symbol,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Exchange Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SlateSurface
                        ) {
                            Text(
                                text = item.exchangeName,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        if (isHighValue) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonRed
                            ) {
                                Text(
                                    text = "ALARM ≥$${(minThreshold/1000).toInt()}K",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Fiyat: $${String.format("%.6g", item.price)} • ${dateFormat.format(Date(item.timestamp))}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%,.0f", item.volUsd)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isShort) NeonRed else NeonGreen
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Instant Analyze Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ElectricCyanBg)
                        .clickable { onTriggerManualAnalysis(item.symbol) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("analyze_btn_${item.symbol}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analyze",
                        tint = ElectricCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Analiz Et",
                        fontSize = 10.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
