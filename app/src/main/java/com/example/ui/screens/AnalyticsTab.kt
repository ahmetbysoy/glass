package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import com.example.data.model.LiquidationAnalysis
import com.example.data.model.MarketStats
import com.example.data.model.PredictionStatus
import com.example.ui.theme.*

@Composable
fun AnalyticsTab(
    marketStats: MarketStats,
    analyses: List<LiquidationAnalysis>
) {
    val verifiedList = analyses.filter { it.status != PredictionStatus.PENDING }
    val hitCount = verifiedList.count { it.status == PredictionStatus.HIT }
    val missCount = verifiedList.count { it.status == PredictionStatus.MISS }
    val hitRate = if (verifiedList.isNotEmpty()) (hitCount.toDouble() / verifiedList.size) * 100.0 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Performance Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_overview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Stats",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ajan İsabet & Başarı Metrikleri",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Toplam Analiz", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = "${analyses.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Doğrulanan", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = "${verifiedList.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("İsabet Oranı", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = String.format("%.1f%%", hitRate),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hitRate >= 50) NeonGreen else NeonRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hit Rate Visual Bar
                    val total = maxOf(1, verifiedList.size)
                    val hitRatio = (hitCount.toFloat() / total).coerceIn(0.02f, 0.98f)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Başarılı (HİT): $hitCount", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                            Text("Hatalı (MİSS): $missCount", fontSize = 11.sp, color = NeonRed, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SlateBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(hitRatio)
                                    .fillMaxHeight()
                                    .background(NeonGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f - hitRatio)
                                    .fillMaxHeight()
                                    .background(NeonRed)
                            )
                        }
                    }
                }
            }
        }

        // Top Liquidated Altcoins Leaderboard
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_leaderboard_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Leaderboard",
                            tint = NeonAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "En Çok Kısa Tasfiye Olan Altcoinler",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (marketStats.topLiquidatedAltcoins.isEmpty()) {
                        Text(
                            text = "Canlı veriler biriktikçe lider tablosu güncellenecektir.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    } else {
                        marketStats.topLiquidatedAltcoins.forEachIndexed { index, (symbol, vol) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (index) {
                                            0 -> NeonAmber
                                            1 -> TextSecondary
                                            2 -> ElectricCyan
                                            else -> SlateSurface
                                        }
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index < 3) SlateDark else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = symbol,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Text(
                                    text = "$${String.format("%,.0f", vol)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonRed
                                )
                            }

                            if (index < marketStats.topLiquidatedAltcoins.size - 1) {
                                Divider(color = SlateBorder, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // Multi-Factor Weighting Guide
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_factors_guide_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Model",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Çok Faktörlü Nicel Analiz Algoritması",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FactorWeightRow("📈 Kısa Tasfiye Dalgası (Cascade)", "%30", "3 dakikalık short vs long tasfiye hacmi payı")
                    FactorWeightRow("📊 Mum Momentumu (1m & 5m)", "%22", "Eğim, hız ve mum hacim patlaması")
                    FactorWeightRow("💵 Agresif Trade Akışı", "%18", "Son 100 işlemdeki alıcı/satıcı hacim oranı")
                    FactorWeightRow("📚 Emir Defteri Dengesi (Depth)", "%15", "Top 10 kademedeki alış/satış derinliği")
                    FactorWeightRow("💰 Funding Oranı (Squeeze)", "%10", "Negatif funding short sıkışmasını teyit eder")
                    FactorWeightRow("🌫️ Rejim & Uzama Filtresi", "%5", "Sakin piyasada YATAY'a çeken aşırı uzama kontrolü")
                }
            }
        }
    }
}

@Composable
fun FactorWeightRow(title: String, weight: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = weight, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
        }
        Text(text = description, fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = SlateBorder, thickness = 0.5.dp)
    }
}
