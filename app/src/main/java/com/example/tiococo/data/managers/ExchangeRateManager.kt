package com.example.tiococo.data.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.tiococo.utils.ConnectivityChecker

// Excepciones personalizadas
class NoInternetException(message: String = "No hay conexión a internet") : Exception(message)
class RateFetchException(message: String = "Error al obtener la tasa de cambio") : Exception(message)

object ExchangeRateManager {
    private const val CACHE_DURATION = 30 * 60 * 1000 // 30 minutos
    private const val TAG = "ExchangeRateManager"
    private const val MINIMUM_RATE = 1.0 // Tasa mínima válida

    data class RateResult(
        val rate: Double,
        val isFromCache: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val error: Exception? = null
    )

    @Volatile private var currentRate: Double = 36.0
    private var lastUpdateTime: Long = 0
    private lateinit var sharedPrefs: SharedPreferences

    fun initialize(context: Context) {
        sharedPrefs = context.getSharedPreferences("ExchangeRatePrefs", Context.MODE_PRIVATE)
        currentRate = sharedPrefs.getFloat("last_rate", currentRate.toFloat()).toDouble()
        lastUpdateTime = sharedPrefs.getLong("last_update", 0)
    }

    suspend fun getCurrentRateWithStatus(context: Context, forceUpdate: Boolean = false): RateResult {
        return withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()

                if (forceUpdate) {
                    Log.d(TAG, "Actualización forzada solicitada")
                    if (!ConnectivityChecker(context).hasInternet()) {
                        return@withContext RateResult(
                            rate = currentRate,
                            isFromCache = true,
                            error = NoInternetException()
                        )
                    }

                    val newRate = fetchFromBcv(context)
                    if (newRate < MINIMUM_RATE) {
                        throw RateFetchException("Tasa obtenida no válida: $newRate")
                    }

                    currentRate = newRate
                    lastUpdateTime = currentTime
                    saveToPreferences()
                    return@withContext RateResult(newRate, false)
                }

                if (!shouldUpdate(currentTime)) {
                    return@withContext RateResult(currentRate, true)
                }

                if (!ConnectivityChecker(context).hasInternet()) {
                    return@withContext RateResult(currentRate, true)
                }

                val newRate = fetchFromBcv(context)
                if (newRate < MINIMUM_RATE) {
                    throw RateFetchException("Tasa obtenida no válida: $newRate")
                }

                currentRate = newRate
                lastUpdateTime = currentTime
                saveToPreferences()

                RateResult(newRate, false)
            } catch (e: Exception) {
                Log.e(TAG, "Error en getCurrentRateWithStatus", e)
                RateResult(currentRate, true, error = e)
            }
        }
    }

    private suspend fun fetchFromBcv(context: Context): Double {
        return BcvScraper.getDollarRate(context)?.takeIf { it > 0 }
            ?: throw RateFetchException("No se pudo obtener tasa válida del BCV")
    }

    private fun shouldUpdate(currentTime: Long): Boolean {
        return currentTime - lastUpdateTime >= CACHE_DURATION
    }

    private fun saveToPreferences() {
        sharedPrefs.edit().apply {
            putFloat("last_rate", currentRate.toFloat())
            putLong("last_update", lastUpdateTime)
            apply()
        }
    }
}