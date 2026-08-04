package com.glasspro.tracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.glasspro.tracker.ui.screens.MainScreen
import com.glasspro.tracker.ui.viewmodel.MarketViewModel

@Composable
fun UserCenteredMain(viewModel: MarketViewModel) {
    Column {
        MarketHealthBanner()
        MainScreen(viewModel = viewModel)
    }
}
