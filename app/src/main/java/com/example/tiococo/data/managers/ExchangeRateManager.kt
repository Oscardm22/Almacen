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
import io.ktor.client.statement.HttpResponse
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

    suspend fun getCurrentRate(context: Context, forceUpdate: Boolean = false): Double {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ExchangeRate", "Iniciando getCurrentRate, forceUpdate=$forceUpdate")
                val currentTime = System.currentTimeMillis()

                if (!shouldUpdate(forceUpdate, currentTime)) {
                    return@withContext currentRate
                }

                if (!ConnectivityChecker(context).hasInternet()) {
                    Log.w("ExchangeRate", "No hay conexión a internet")
                    return@withContext currentRate
                }

                fetchAndUpdateRate(currentTime)
            } catch (e: Exception) {
                Log.e("ExchangeRate", "Error crítico", e)
                currentRate
            }
        }
    }

    private fun shouldUpdate(forceUpdate: Boolean, currentTime: Long): Boolean {
        return forceUpdate || currentTime - lastUpdateTime > CACHE_DURATION
    }

    private suspend fun fetchAndUpdateRate(currentTime: Long): Double {
        val response = httpClient.get(API_URL).body<DolarResponse>()
        response.venta?.let { newRate ->
            currentRate = newRate
            lastUpdateTime = currentTime
            saveToPreferences()
            Log.d("ExchangeRate", "Nueva tasa: $newRate")
        }
        return currentRate
    }

    private fun saveToPreferences() {
        sharedPrefs.edit().run {
            putFloat("last_rate", currentRate.toFloat())
            putLong("last_update", lastUpdateTime)
            apply()
        }
    }
}