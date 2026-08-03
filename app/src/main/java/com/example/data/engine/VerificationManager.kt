package com.example.data.engine

import android.util.Log
import com.example.data.db.AnalysisDao
import com.example.data.model.PredictionDirection
import com.example.data.model.PredictionStatus
import com.example.data.remote.ExchangeDataService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class VerificationManager(
    private val analysisDao: AnalysisDao,
    private val exchangeService: ExchangeDataService,
    private val scope: CoroutineScope
) {

    fun startVerificationLoop() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    checkPendingVerifications()
                } catch (e: Exception) {
                    Log.e("VerificationManager", "Error in verification loop: ${e.message}")
                }
                delay(3000L) // Check every 3 seconds
            }
        }
    }

    private suspend fun checkPendingVerifications() {
        val pending = analysisDao.getPendingAnalyses()
        val now = System.currentTimeMillis()

        for (item in pending) {
            if (now >= item.targetVerifyAt) {
                verifyAnalysis(item)
            }
        }
    }

    private suspend fun verifyAnalysis(item: com.example.data.db.AnalysisEntity) {
        val marketData = exchangeService.fetchMarketData(item.symbol)
        val actualPx = marketData.price ?: item.triggerPrice
        val triggerPx = item.triggerPrice

        val chgPct = if (triggerPx > 0) ((actualPx - triggerPx) / triggerPx) * 100.0 else 0.0

        val hit = when (item.direction) {
            PredictionDirection.YUKARI.name -> chgPct >= 0.15
            PredictionDirection.ASAGI.name -> chgPct <= -0.15
            PredictionDirection.YATAY.name -> abs(chgPct) < 0.15
            else -> false
        }

        val status = if (hit) PredictionStatus.HIT.name else PredictionStatus.MISS.name

        analysisDao.updateVerification(
            id = item.id,
            status = status,
            actualPrice = actualPx,
            priceChangePct = chgPct
        )

        Log.d("VerificationManager", "Verified ${item.symbol}: $status (change: String.format('%.2f', chgPct)%)")
    }
}
