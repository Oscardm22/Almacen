package com.example.tiococo.data.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.Connection.Response

object BcvScraper {
    private const val BCV_URL = "https://www.bcv.org.ve"
    private const val TAG = "BcvScraper"
    private const val TIMEOUT_MS = 30000

    private val mutex = Mutex() // Usaremos solo este mecanismo de bloqueo
    private var cachedRate: Double? = null
    private var isRequestInProgress = false

    suspend fun getDollarRate(context: Context): Double? = withContext(Dispatchers.IO) {
        // Verificación rápida antes del bloqueo
        if (isRequestInProgress) {
            Log.d(TAG, "⏸️ Solicitud en curso (verificación rápida), retornando caché")
            return@withContext cachedRate
        }

        mutex.withLock {  // Cambiado de requestMutex a mutex
            try {
                if (isRequestInProgress) {
                    Log.d(TAG, "⚠️ Solicitud en curso (verificación segura), evitando duplicado")
                    return@withContext cachedRate
                }

                isRequestInProgress = true

                if (!isOnline(context)) {
                    Log.w(TAG, "📵 Sin conexión - usando caché")
                    return@withContext cachedRate
                }

                Log.d(TAG, "🔵 Iniciando nueva solicitud al BCV...")

                val rate = try {
                    val response = Jsoup.connect(BCV_URL)
                        .timeout(TIMEOUT_MS)
                        .userAgent(getRandomUserAgent())
                        .execute()

                    parseResponse(response)
                } catch (e: Exception) {
                    Log.e(TAG, "🔴 Error en la solicitud:", e)
                    cachedRate
                }

                rate?.also { cachedRate = it }
                rate
            } finally {
                isRequestInProgress = false
            }
        }
    }

    private fun parseResponse(response: Response): Double? {
        return try {
            val document = response.parse()
            val rateText = document.selectFirst("div#dolar div.centrado strong")
                ?.text()
                ?.trim()
                ?: throw Exception("Elemento no encontrado")

            Log.d(TAG, "✅ Tasa obtenida: $rateText")

            rateText.replace("[^\\d,]".toRegex(), "")
                .replace(",", ".")
                .toDoubleOrNull()
                ?.takeIf { it > 0 }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando respuesta", e)
            null
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }

    private fun getRandomUserAgent(): String {
        val agents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            "Mozilla/5.0 (Linux; Android 10; SM-A205U)",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X)"
        )
        return agents.random()
    }
}