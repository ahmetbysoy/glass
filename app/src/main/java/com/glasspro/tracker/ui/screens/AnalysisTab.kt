package com.glasspro.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasspro.tracker.core.model.AnalysisResult
import com.glasspro.tracker.core.model.Direction
import com.glasspro.tracker.core.model.SignalStatus
import com.glasspro.tracker.ui.theme.ElectricCyan
import com.glasspro.tracker.ui.theme.ElectricCyanBg
import com.glasspro.tracker.ui.theme.NeonAmber
import com.glasspro.tracker.ui.theme.NeonAmberBg
import com.glasspro.tracker.ui.theme.NeonGreen
import com.glasspro.tracker.ui.theme.NeonGreenBg
import com.glasspro.tracker.ui.theme.NeonRed
import com.glasspro.tracker.ui.theme.NeonRedBg
import com.glasspro.tracker.ui.theme.SlateBorder
import com.glasspro.tracker.ui.theme.SlateCard
import com.glasspro.tracker.ui.theme.SlateDark
import com.glasspro.tracker.ui.theme.SlateSurface
import com.glasspro.tracker.ui.theme.TextMuted
import com.glasspro.tracker.ui.theme.TextPrimary
import com.glasspro.tracker.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun AnalysisTab(
    analyses: List<AnalysisResult>,
    onTriggerManualAnalysis: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero metrics
        val verified = analyses.filter { it.status != SignalStatus.PENDING }
        val hits = verified.count { it.status == SignalStatus.HIT }
        val hitRate = if (verified.isNotEmpty()) hits.toDouble() / verified.size * 100.0 else 0.0

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBlock("Toplam Analiz", analyses.size.toString(), TextPrimary)
                MetricBlock("60sn/1S İsabet", String.format("%.1f%%", hitRate), if (hitRate >= 50.0) NeonGreen else NeonRed)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sonuçlar", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = "✔$hits ✘${verified.size - hits} ⌛${analyses.count { it.status == SignalStatus.PENDING }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Profesyonel Nicel Analizler (${analyses.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (analyses.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Analiz yok",
                        tint = TextMuted,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Eşiği geçen gerçek bir kısa tasfiye olmadı.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Canlı tasfiye akışından eşik üstü olay gelince otomatik analiz üretilir.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
            ) {
                items(analyses, key = { it.id }) { item ->
                    AnalysisCard(analysis = item)
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AnalysisCard(analysis: AnalysisResult) {
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    val isPending = analysis.status == SignalStatus.PENDING

    LaunchedEffect(analysis.verifyAtMs, analysis.status) {
        if (isPending) {
            while (true) {
                val rem = maxOf(0L, (analysis.verifyAtMs - System.currentTimeMillis()) / 1000L)
                remainingSeconds = rem
                if (rem <= 0L) break
                delay(1000L)
            }
        }
    }

    val dirColor = when (analysis.direction) {
        Direction.LONG -> NeonGreen
        Direction.SHORT -> NeonRed
        Direction.NEUTRAL -> NeonAmber
    }
    val dirBg = when (analysis.direction) {
        Direction.LONG -> NeonGreenBg
        Direction.SHORT -> NeonRedBg
        Direction.NEUTRAL -> NeonAmberBg
    }
    val statusColor = when (analysis.status) {
        SignalStatus.HIT -> NeonGreen
        SignalStatus.MISS -> NeonRed
        SignalStatus.PENDING -> SlateBorder
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = analysis.symbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = SlateSurface) {
                        Text(
                            text = analysis.horizonLabel,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = dirBg,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${analysis.direction.label} ${analysis.direction.symbol}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = dirColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%${analysis.confidence.toInt()}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Fiyat: $${String.format("%.6g", analysis.price)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                when (analysis.status) {
                    SignalStatus.PENDING -> Text(
                        text = "Doğrulama: ${remainingSeconds}sn",
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    else -> Text(
                        text = "${analysis.status.label} (${String.format("%+.2f%%", analysis.priceChangePct ?: 0.0)})",
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // VG Matrix Quantitative Setup Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = ElectricCyanBg,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VG MATRIX QUANTITATIVE SCORE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricCyan
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonAmberBg
                        ) {
                            Text(
                                text = "🪤 TAZE BOĞA TUZAĞI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "True Gaussian Kernel • John Ehlers N-Pole Filter • Scalp Acceleration: +2.8%",
                        fontSize = 10.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alerts / Strategy List
            analysis.strategy.alerts.forEach { alertText ->
                Text(
                    text = "• $alertText",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
