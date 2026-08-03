package com.example.data.model

enum class LiquidationSide {
    KISA, // Short Liquidation (Red)
    UZUN  // Long Liquidation (Green)
}

enum class PredictionDirection(val label: String, val symbol: String) {
    YUKARI("YUKARI", "▲"),
    ASAGI("AŞAĞI", "▼"),
    YATAY("YATAY", "◆")
}

enum class PredictionStatus(val label: String) {
    PENDING("BEKLİYOR"),
    HIT("HİT"),
    MISS("MİSS")
}

data class LiquidationEvent(
    val id: String,
    val symbol: String,
    val originalSymbol: String,
    val exchangeName: String, // Binance, Bybit, OKX, Bitget
    val price: Double,
    val side: LiquidationSide,
    val volUsd: Double,
    val timestamp: Long,
    val isAltcoin: Boolean = true
)

data class LiquidationAnalysis(
    val id: String,
    val liquidationId: String,
    val symbol: String,
    val originalSymbol: String,
    val exchangeName: String,
    val triggerPrice: Double,
    val triggerVolUsd: Double,
    val isCascade: Boolean,
    val direction: PredictionDirection,
    val confidence: Int, // e.g. 75 (%)
    val score: Double,   // e.g. +0.42
    val reasons: List<String>,
    val supportPrice: Double,
    val resistancePrice: Double,
    val currentPriceAtAnalysis: Double,
    val status: PredictionStatus = PredictionStatus.PENDING,
    val actualPrice: Double? = null,
    val priceChangePct: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val targetVerifyAt: Long = System.currentTimeMillis() + 60_000L,
    val providerUsed: String = "okx",
    val cascadeShortVol3m: Double = 0.0,
    val cascadeShortCount3m: Int = 0
)

data class MarketStats(
    val totalLiquidationsCount: Int = 0,
    val shortCount: Int = 0,
    val longCount: Int = 0,
    val totalShortUsd: Double = 0.0,
    val totalLongUsd: Double = 0.0,
    val totalAnalysesCount: Int = 0,
    val verifiedCount: Int = 0,
    val hitCount: Int = 0,
    val missCount: Int = 0,
    val hitRatePct: Double = 0.0,
    val topLiquidatedAltcoins: List<Pair<String, Double>> = emptyList()
)
