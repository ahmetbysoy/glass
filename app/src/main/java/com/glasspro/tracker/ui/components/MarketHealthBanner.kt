package com.glasspro.tracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glasspro.tracker.ui.theme.NeonGreen

@Composable
fun MarketHealthBanner(isHealthy: Boolean = true) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isHealthy) NeonGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isHealthy) "Piyasa Sağlıklı — Yeni fırsatlar bekleniyor" else "Yüksek Volatilite — Dikkatli ol",
                fontWeight = FontWeight.Bold,
                color = if (isHealthy) NeonGreen else Color.Red
            )
        }
    }
}
