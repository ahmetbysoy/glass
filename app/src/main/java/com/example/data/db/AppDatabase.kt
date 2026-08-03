package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.model.LiquidationSide
import com.example.data.model.PredictionDirection
import com.example.data.model.PredictionStatus
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Entity(tableName = "liquidations")
data class LiquidationEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val originalSymbol: String,
    val exchangeName: String,
    val price: Double,
    val side: String, // KISA, UZUN
    val volUsd: Double,
    val timestamp: Long,
    val isAltcoin: Boolean
)

@Entity(tableName = "analyses")
data class AnalysisEntity(
    @PrimaryKey val id: String,
    val liquidationId: String,
    val symbol: String,
    val originalSymbol: String,
    val exchangeName: String,
    val triggerPrice: Double,
    val triggerVolUsd: Double,
    val isCascade: Boolean,
    val direction: String, // YUKARI, ASAGI, YATAY
    val confidence: Int,
    val score: Double,
    val reasonsJson: String, // JSON Array string
    val supportPrice: Double,
    val resistancePrice: Double,
    val currentPriceAtAnalysis: Double,
    val status: String, // PENDING, HIT, MISS
    val actualPrice: Double?,
    val priceChangePct: Double?,
    val createdAt: Long,
    val targetVerifyAt: Long,
    val providerUsed: String,
    val cascadeShortVol3m: Double,
    val cascadeShortCount3m: Int
)

class Converters {
    @TypeConverter
    fun fromReasonsList(reasons: List<String>): String {
        val jsonArray = JSONArray()
        reasons.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toReasonsList(jsonStr: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}

@Dao
interface LiquidationDao {
    @Query("SELECT * FROM liquidations ORDER BY timestamp DESC LIMIT 200")
    fun getAllLiquidations(): Flow<List<LiquidationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiquidation(event: LiquidationEntity)

    @Query("DELETE FROM liquidations")
    suspend fun clearAll()
}

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analyses ORDER BY createdAt DESC")
    fun getAllAnalyses(): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingAnalyses(): List<AnalysisEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity)

    @Query("UPDATE analyses SET status = :status, actualPrice = :actualPrice, priceChangePct = :priceChangePct WHERE id = :id")
    suspend fun updateVerification(id: String, status: String, actualPrice: Double, priceChangePct: Double)

    @Query("DELETE FROM analyses")
    suspend fun clearAll()
}

@Database(
    entities = [LiquidationEntity::class, AnalysisEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun liquidationDao(): LiquidationDao
    abstract fun analysisDao(): AnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coinglass_liquidations.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
