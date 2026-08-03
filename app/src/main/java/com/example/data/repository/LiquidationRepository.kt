package com.example.data.repository

import com.example.data.db.AnalysisDao
import com.example.data.db.Converters
import com.example.data.db.LiquidationDao
import com.example.data.engine.VerificationManager
import com.example.data.model.LiquidationAnalysis
import com.example.data.model.LiquidationEvent
import com.example.data.model.LiquidationSide
import com.example.data.model.MarketStats
import com.example.data.model.PredictionDirection
import com.example.data.model.PredictionStatus
import com.example.data.remote.LiquidationStreamManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LiquidationRepository(
    private val liquidationDao: LiquidationDao,
    private val analysisDao: AnalysisDao,
    val streamManager: LiquidationStreamManager,
    val verificationManager: VerificationManager
) {
    private val converters = Converters()

    val allLiquidations: Flow<List<LiquidationEvent>> = liquidationDao.getAllLiquidations().map { list ->
        list.map { entity ->
            LiquidationEvent(
                id = entity.id,
                symbol = entity.symbol,
                originalSymbol = entity.originalSymbol,
                exchangeName = entity.exchangeName,
                price = entity.price,
                side = if (entity.side == LiquidationSide.KISA.name) LiquidationSide.KISA else LiquidationSide.UZUN,
                volUsd = entity.volUsd,
                timestamp = entity.timestamp,
                isAltcoin = entity.isAltcoin
            )
        }
    }

    val allAnalyses: Flow<List<LiquidationAnalysis>> = analysisDao.getAllAnalyses().map { list ->
        list.map { entity ->
            LiquidationAnalysis(
                id = entity.id,
                liquidationId = entity.liquidationId,
                symbol = entity.symbol,
                originalSymbol = entity.originalSymbol,
                exchangeName = entity.exchangeName,
                triggerPrice = entity.triggerPrice,
                triggerVolUsd = entity.triggerVolUsd,
                isCascade = entity.isCascade,
                direction = try { PredictionDirection.valueOf(entity.direction) } catch (e: Exception) { PredictionDirection.YUKARI },
                confidence = entity.confidence,
                score = entity.score,
                reasons = converters.toReasonsList(entity.reasonsJson),
                supportPrice = entity.supportPrice,
                resistancePrice = entity.resistancePrice,
                currentPriceAtAnalysis = entity.currentPriceAtAnalysis,
                status = try { PredictionStatus.valueOf(entity.status) } catch (e: Exception) { PredictionStatus.PENDING },
                actualPrice = entity.actualPrice,
                priceChangePct = entity.priceChangePct,
                createdAt = entity.createdAt,
                targetVerifyAt = entity.targetVerifyAt,
                providerUsed = entity.providerUsed,
                cascadeShortVol3m = entity.cascadeShortVol3m,
                cascadeShortCount3m = entity.cascadeShortCount3m
            )
        }
    }

    val marketStats: Flow<MarketStats> = allLiquidations.map { liqs ->
        val totalCount = liqs.size
        val shorts = liqs.filter { it.side == LiquidationSide.KISA }
        val longs = liqs.filter { it.side == LiquidationSide.UZUN }
        val shortVol = shorts.sumOf { it.volUsd }
        val longVol = longs.sumOf { it.volUsd }

        val topAltcoins = shorts.groupBy { it.symbol }
            .mapValues { entry -> entry.value.sumOf { it.volUsd } }
            .toList()
            .sortedByDescending { it.second }
            .take(6)

        MarketStats(
            totalLiquidationsCount = totalCount,
            shortCount = shorts.size,
            longCount = longs.size,
            totalShortUsd = shortVol,
            totalLongUsd = longVol,
            topLiquidatedAltcoins = topAltcoins
        )
    }

    fun startEngine() {
        streamManager.startStream()
        verificationManager.startVerificationLoop()
    }

    suspend fun triggerManualAnalysis(symbol: String) {
        streamManager.triggerManualAnalysis(symbol)
    }

    suspend fun clearHistory() {
        liquidationDao.clearAll()
        analysisDao.clearAll()
    }
}
