package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.engine.LiquidationAnalysisEngine
import com.example.data.engine.VerificationManager
import com.example.data.model.LiquidationAnalysis
import com.example.data.model.LiquidationEvent
import com.example.data.model.MarketStats
import com.example.data.model.PredictionStatus
import com.example.data.remote.ExchangeDataService
import com.example.data.remote.LiquidationStreamManager
import com.example.data.repository.LiquidationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val exchangeService = ExchangeDataService()
    private val analysisEngine = LiquidationAnalysisEngine()

    private val streamManager = LiquidationStreamManager(
        liquidationDao = db.liquidationDao(),
        analysisDao = db.analysisDao(),
        exchangeService = exchangeService,
        analysisEngine = analysisEngine,
        scope = viewModelScope
    )

    private val verificationManager = VerificationManager(
        analysisDao = db.analysisDao(),
        exchangeService = exchangeService,
        scope = viewModelScope
    )

    private val repository = LiquidationRepository(
        liquidationDao = db.liquidationDao(),
        analysisDao = db.analysisDao(),
        streamManager = streamManager,
        verificationManager = verificationManager
    )

    // UI State
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _minUsdThreshold = MutableStateFlow(5000.0)
    val minUsdThreshold: StateFlow<Double> = _minUsdThreshold.asStateFlow()

    private val _excludeBtcEth = MutableStateFlow(true)
    val excludeBtcEth: StateFlow<Boolean> = _excludeBtcEth.asStateFlow()

    private val _isLiveStreaming = MutableStateFlow(true)
    val isLiveStreaming: StateFlow<Boolean> = _isLiveStreaming.asStateFlow()

    private val _soundAlerts = MutableStateFlow(true)
    val soundAlerts: StateFlow<Boolean> = _soundAlerts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("TÜMÜ") // TÜMÜ, HIT, MISS, BEKLİYOR
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    // Trigger Popup Banner
    private val _bannerAlert = MutableStateFlow<Pair<LiquidationEvent, String>?>(null)
    val bannerAlert: StateFlow<Pair<LiquidationEvent, String>?> = _bannerAlert.asStateFlow()

    // Data Flows
    val liquidations: StateFlow<List<LiquidationEvent>> = repository.allLiquidations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val rawAnalyses: StateFlow<List<LiquidationAnalysis>> = repository.allAnalyses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val marketStats: StateFlow<MarketStats> = repository.marketStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), MarketStats())

    // Filtered Analyses
    val filteredAnalyses: StateFlow<List<LiquidationAnalysis>> = combine(
        rawAnalyses, searchQuery, statusFilter
    ) { list, query, filter ->
        list.filter { analysis ->
            val matchesQuery = query.isBlank() ||
                    analysis.symbol.contains(query, ignoreCase = true) ||
                    analysis.exchangeName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "HIT" -> analysis.status == PredictionStatus.HIT
                "MISS" -> analysis.status == PredictionStatus.MISS
                "BEKLİYOR" -> analysis.status == PredictionStatus.PENDING
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        repository.startEngine()

        // Listen for live trigger banner alerts
        viewModelScope.launch {
            streamManager.latestAlertEvent.collect { alert ->
                _bannerAlert.value = alert
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun setMinUsdThreshold(threshold: Double) {
        _minUsdThreshold.value = threshold
        streamManager.minUsdThreshold = threshold
    }

    fun setExcludeBtcEth(exclude: Boolean) {
        _excludeBtcEth.value = exclude
        streamManager.excludeBtcEth = exclude
    }

    fun setLiveStreaming(enabled: Boolean) {
        _isLiveStreaming.value = enabled
        streamManager.isLiveStreaming = enabled
    }

    fun setSoundAlerts(enabled: Boolean) {
        _soundAlerts.value = enabled
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun dismissBanner() {
        _bannerAlert.value = null
    }

    fun triggerManualAnalysis(symbol: String) {
        viewModelScope.launch {
            repository.triggerManualAnalysis(symbol)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
