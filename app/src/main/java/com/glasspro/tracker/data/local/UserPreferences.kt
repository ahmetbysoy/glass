package com.glasspro.tracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "glass_settings")

class UserPreferences(private val context: Context) {
    companion object {
        val MIN_THRESHOLD = doublePreferencesKey("min_threshold")
        val EXCLUDE_BTC_ETH = booleanPreferencesKey("exclude_btc_eth")
    }

    val minThresholdFlow: Flow<Double> = context.dataStore.data.map { preferences: Preferences ->
        preferences[MIN_THRESHOLD] ?: 10000.0
    }

    val excludeBtcEthFlow: Flow<Boolean> = context.dataStore.data.map { preferences: Preferences ->
        preferences[EXCLUDE_BTC_ETH] ?: false
    }

    suspend fun setMinThreshold(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[MIN_THRESHOLD] = value
        }
    }

    suspend fun setExcludeBtcEth(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXCLUDE_BTC_ETH] = value
        }
    }
}
