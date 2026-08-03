package com.example

import com.example.data.engine.LiquidationAnalysisEngine
import com.example.data.model.LiquidationEvent
import com.example.data.model.LiquidationSide
import com.example.data.model.PredictionDirection
import com.example.data.remote.CandleData
import com.example.data.remote.OrderBookLevel
import com.example.data.remote.RawMarketData
import com.example.data.remote.TradeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LiquidationAnalysisEngineTest {

    @Test
    fun testShortLiquidationTriggerGeneratesAnalysis() {
        val engine = LiquidationAnalysisEngine()

        val event = LiquidationEvent(
            id = "test-1",
            symbol = "HOME",
            originalSymbol = "HOMEUSDT",
            exchangeName = "Binance",
            price = 0.0083,
            side = LiquidationSide.KISA,
            volUsd = 8500.0,
            timestamp = System.currentTimeMillis(),
            isAltcoin = true
        )

        val candles = listOf(
            CandleData(1000L, 0.0080, 0.0081, 0.0079, 0.0081, 1000.0),
            CandleData(2000L, 0.0081, 0.0082, 0.0080, 0.0082, 1200.0),
            CandleData(3000L, 0.0082, 0.0083, 0.0081, 0.0083, 1500.0),
            CandleData(4000L, 0.0083, 0.0084, 0.0082, 0.0084, 2500.0),
            CandleData(5000L, 0.0084, 0.0085, 0.0083, 0.0085, 4000.0)
        )

        val trades = listOf(
            TradeItem("buy", 0.0085, 50000.0),
            TradeItem("buy", 0.0085, 40000.0),
            TradeItem("sell", 0.0085, 5000.0)
        )

        val bids = listOf(OrderBookLevel(0.0084, 100000.0))
        val asks = listOf(OrderBookLevel(0.0086, 20000.0))

        val marketData = RawMarketData(
            symbol = "HOME",
            provider = "okx",
            price = 0.0085,
            high24h = 0.0090,
            low24h = 0.0075,
            fundingRate = -0.00045, // negative = short squeeze fuel
            candles1m = candles,
            trades = trades,
            bids = bids,
            asks = asks,
            isSuccess = true
        )

        val analysis = engine.analyze(
            event = event,
            marketData = marketData,
            rollingShortVol3m = 18000.0,
            rollingLongVol3m = 1200.0,
            rollingShortCount3m = 4,
            rollingLongCount3m = 1
        )

        assertNotNull(analysis)
        assertEquals("HOME", analysis.symbol)
        assertEquals(PredictionDirection.YUKARI, analysis.direction)
    }
}
