@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.tiococo.data.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.tiococo.utils.ConnectivityChecker
import javax.net.ssl.SSLContext
import javax.net.ssl.HostnameVerifier

// Excepciones personalizadas
class NoInternetException(message: String = "No hay conexión a internet") : Exception(message)
class RateFetchException(message: String = "Error al obtener la tasa de cambio") : Exception(message)

@Serializable
data class DolarResponse(
    val fuente: String? = null,
    val nombre: String? = null,
    val compra: Double? = null,
    val venta: Double? = null,
    val promedio: Double? = null,
    val fechaActualizacion: String? = null
)

object ExchangeRateManager {
    private const val API_URL = "https://ve.dolarapi.com/v1/dolares/oficial"
    private const val CACHE_DURATION = 30 * 60 * 1000 // 30 minutos
    // Agrega esto al inicio del objeto ExchangeRateManager
    private const val TAG = "ExchangeRateManager"

    data class RateResult(
        val rate: Double,
        val isFromCache: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        var error: Exception? = null
    )

    @Volatile var currentRate: Double = 36.0
        internal set
    private var lastUpdateTime: Long = 0
    private lateinit var sharedPrefs: SharedPreferences

    private val httpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000L
                connectTimeoutMillis = 15_000L
                socketTimeoutMillis = 15_000L
            }

            // Configuración de seguridad simplificada
            engine {
                // Configuración SSL para Android
                sslManager = { httpsURLConnection ->
                    httpsURLConnection.sslSocketFactory = SSLContext.getDefault().socketFactory
                    httpsURLConnection.hostnameVerifier = HostnameVerifier { hostname, _ ->
                        hostname.equals("ve.dolarapi.com", ignoreCase = true) ||
                                hostname.endsWith(".ve.dolarapi.com", ignoreCase = true)
                    }
                }
            }
        }
    }

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

                    val newRate = fetchFromApi()
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

                val newRate = fetchFromApi()
                currentRate = newRate
                lastUpdateTime = currentTime
                saveToPreferences()

                RateResult(newRate, false)
            } catch (e: Exception) {
                RateResult(currentRate, true, error = e)
            }
        }
    }

    private suspend fun fetchFromApi(): Double {
        try {
            Log.d(TAG, "Preparando request a $API_URL")
            val response = httpClient.get(API_URL)
            Log.d(TAG, "Response status: ${response.status}")

            val body = response.body<DolarResponse>().also {
                Log.d(TAG, "Respuesta parseada: $it")
            }

            return body.venta ?: body.promedio ?: body.compra
            ?: throw RateFetchException("La respuesta no contiene valores de tasa válidos")
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener tasa", e)
            throw e
        }
    }

    private fun shouldUpdate(currentTime: Long): Boolean {
        return currentTime - lastUpdateTime >= CACHE_DURATION
    }

    private fun saveToPreferences() {
        sharedPrefs.edit().run {
            putFloat("last_rate", currentRate.toFloat())
            putLong("last_update", lastUpdateTime)
            // Guardar histórico
            putFloat("rate_${System.currentTimeMillis()}", currentRate.toFloat())
            apply()
        }
    }
}