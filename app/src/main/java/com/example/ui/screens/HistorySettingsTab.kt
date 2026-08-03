package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
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
import com.example.ui.theme.*

@Composable
fun HistorySettingsTab(
    analyses: List<LiquidationAnalysis>,
    searchQuery: String,
    statusFilter: String,
    minThreshold: Double,
    excludeBtcEth: Boolean,
    isLiveStreaming: Boolean,
    soundAlerts: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onThresholdChange: (Double) -> Unit,
    onToggleExcludeBtcEth: (Boolean) -> Unit,
    onToggleLiveStreaming: (Boolean) -> Unit,
    onToggleSoundAlerts: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Settings Section Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ajan & Filtre Ayarları",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Minimum USD Threshold Slider
                    Text(
                        text = "Kısa Tasfiye Tetikleyici Eşik: $${String.format("%,.0f", minThreshold)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = minThreshold.toFloat(),
                        onValueChange = { onThresholdChange(it.toDouble()) },
                        valueRange = 1000f..25000f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = SlateBorder
                        ),
                        modifier = Modifier.testTag("threshold_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$1,000", fontSize = 10.sp, color = TextMuted)
                        Text("$5,000 (Varsayılan)", fontSize = 10.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                        Text("$25,000", fontSize = 10.sp, color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SlateBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle: Exclude BTC/ETH (Altcoin focus)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sadece Altcoinler (Alt-coin Odaklı)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("BTC ve ETH tasfiyelerini göz ardı et", fontSize = 11.sp, color = TextSecondary)
                        }

                        Switch(
                            checked = excludeBtcEth,
                            onCheckedChange = onToggleExcludeBtcEth,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SlateDark,
                                checkedTrackColor = ElectricCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateBorder
                            ),
                            modifier = Modifier.testTag("exclude_btc_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle: Live Streaming
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Canlı Piyasa Akışı (Live Stream)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Otomatik tasfiye akışını başlat/durdur", fontSize = 11.sp, color = TextSecondary)
                        }

                        Switch(
                            checked = isLiveStreaming,
                            onCheckedChange = onToggleLiveStreaming,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SlateDark,
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateBorder
                            ),
                            modifier = Modifier.testTag("live_stream_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle: Sound & Vibration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sesli / Görsel Anlık Popup Bildirimi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Kısa tasfiye tetiklendiğinde banner göster", fontSize = 11.sp, color = TextSecondary)
                        }

                        Switch(
                            checked = soundAlerts,
                            onCheckedChange = onToggleSoundAlerts,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SlateDark,
                                checkedTrackColor = ElectricCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateBorder
                            ),
                            modifier = Modifier.testTag("sound_alerts_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clear Database Action Button
                    OutlinedButton(
                        onClick = onClearHistory,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                        border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonRed)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_history_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Veritabanı Geçmişini Temizle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Gemini AI Panel Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_ai_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Gemini AI",
                        tint = PurpleAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Gemini AI Entegrasyon Durumu", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Sunucu taraflı Gemini AI destekli nicel model aktiftir. Secrets panelinden veya BuildConfig üzerinden ek Gemini modelleri otomatik devreye girer.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Search & History Log Section
        item {
            Column {
                Text(
                    text = "Geçmiş Tahmin Günlüğü & Arama",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Sembol ara (örn: SOL, ZEC, HOME)...", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateCard,
                        unfocusedContainerColor = SlateCard,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("TÜMÜ", "HIT", "MISS", "BEKLİYOR").forEach { filter ->
                        val isSelected = statusFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStatusFilterChange(filter) },
                            label = { Text(filter, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                            modifier = Modifier.testTag("status_chip_$filter")
                        )
                    }
                }
            }
        }

        // Render List of Matches
        if (analyses.isEmpty()) {
            item {
                Text(
                    text = "Arama kriterine uygun tahmin bulunamadı.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(analyses, key = { "hist_${it.id}" }) { item ->
                PredictionCard(analysis = item)
            }
        }
    }
}
