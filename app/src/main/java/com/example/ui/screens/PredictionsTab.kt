package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
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
import com.example.data.model.LiquidationAnalysis
import com.example.data.model.PredictionDirection
import com.example.data.model.PredictionStatus
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PredictionsTab(
    analyses: List<LiquidationAnalysis>,
    onTriggerManualAnalysis: (String) -> Unit
) {
    val totalCount = analyses.size
    val verifiedList = analyses.filter { it.status != PredictionStatus.PENDING }
    val hitCount = verifiedList.count { it.status == PredictionStatus.HIT }
    val missCount = verifiedList.count { it.status == PredictionStatus.MISS }
    val pendingCount = analyses.count { it.status == PredictionStatus.PENDING }
    val hitRate = if (verifiedList.isNotEmpty()) (hitCount.toDouble() / verifiedList.size) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Metric Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("predictions_hero_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Toplam Tahmin", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = "$totalCount",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp),
                    color = SlateBorder
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("60sn İsabet Oranı", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = String.format("%.1f%%", hitRate),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hitRate >= 50) NeonGreen else NeonRed
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp),
                    color = SlateBorder
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text("Sonuçlar", fontSize = 11.sp, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✔$hitCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✘$missCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⌛$pendingCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "1 Dakikalık Yön Tahminleri & Analizler (${analyses.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (analyses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "No predictions",
                        tint = TextMuted,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Henüz eşiği geçen bir kısa tasfiye olmadı.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bir altcoinde short tasfiye > $5,000 olduğunda otomatik analiz üretilir.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(analyses, key = { it.id }) { item ->
                    PredictionCard(analysis = item)
                }
            }
        }
    }
}

@Composable
fun PredictionCard(analysis: LiquidationAnalysis) {
    var expanded by remember { mutableStateOf(true) }
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // 60-second Live Countdown timer
    var remainingSeconds by remember { mutableStateOf(0L) }
    val isPending = analysis.status == PredictionStatus.PENDING

    LaunchedEffect(analysis.targetVerifyAt, analysis.status) {
        if (isPending) {
            while (true) {
                val now = System.currentTimeMillis()
                val rem = maxOf(0L, (analysis.targetVerifyAt - now) / 1000L)
                remainingSeconds = rem
                if (rem <= 0) break
                delay(1000L)
            }
        }
    }

    val dirColor = when (analysis.direction) {
        PredictionDirection.YUKARI -> NeonGreen
        PredictionDirection.ASAGI -> NeonRed
        PredictionDirection.YATAY -> NeonAmber
    }

    val dirBg = when (analysis.direction) {
        PredictionDirection.YUKARI -> NeonGreenBg
        PredictionDirection.ASAGI -> NeonRedBg
        PredictionDirection.YATAY -> NeonAmberBg
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prediction_card_${analysis.symbol}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (analysis.status == PredictionStatus.HIT) NeonGreen
                else if (analysis.status == PredictionStatus.MISS) NeonRed
                else SlateBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Direction Header Badge & Symbol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Symbol badge
                    Text(
                        text = analysis.symbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SlateSurface
                    ) {
                        Text(
                            text = analysis.exchangeName,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (analysis.isCascade) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PurpleAccent
                        ) {
                            Text(
                                text = "KASKAD",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Direction Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = dirBg,
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(dirColor))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1DK: ${analysis.direction.label} ${analysis.direction.symbol}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = dirColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%${analysis.confidence}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trigger details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tetikleyici Tasfiye: $${String.format("%,.0f", analysis.triggerVolUsd)}",
                    fontSize = 12.sp,
                    color = NeonRed,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Tetik Fiyatı: $${String.format("%.6g", analysis.triggerPrice)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live 60-second Countdown Progress Bar or Completed Verification
            if (isPending) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "Countdown",
                                tint = NeonAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Doğrulama sayacı:",
                                fontSize = 11.sp,
                                color = NeonAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "${remainingSeconds}sn",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { (remainingSeconds.toFloat() / 60f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonAmber,
                        trackColor = SlateSurface
                    )
                }
            } else {
                // Completed Verification Badge
                val isHit = analysis.status == PredictionStatus.HIT
                val statusColor = if (isHit) NeonGreen else NeonRed
                val chgPct = analysis.priceChangePct ?: 0.0
                val chgFormatted = String.format("%+.2f%%", chgPct)
                val actualPxFormatted = String.format("%.6g", analysis.actualPrice ?: analysis.triggerPrice)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isHit) NeonGreenBg else NeonRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isHit) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = "Result",
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHit) "✔ HİT DOĞRULANDI" else "✘ MİSS TAHMİN SAPTI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Text(
                            text = "60sn Sonra: $$actualPxFormatted ($chgFormatted)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Support & Resistance Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SlateSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Destek: $${String.format("%.6g", analysis.supportPrice)}",
                        fontSize = 11.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SlateSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Direnç: $${String.format("%.6g", analysis.resistancePrice)}",
                        fontSize = 11.sp,
                        color = NeonRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Reasons Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gerekçeler & Analiz Sebepleri (${analysis.reasons.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    analysis.reasons.forEach { reason ->
                        Text(
                            text = "• $reason",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
