package com.example.tiococo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class ConnectivityChecker(private val context: Context) {
    fun hasInternet(): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Verificar red activa
            val network: Network? = connectivityManager.activeNetwork
            Log.d("Connectivity", "Red activa: ${network?.toString() ?: "NULL"}")

            if (network == null) {
                Log.d("Connectivity", "No hay red activa disponible")
                return false
            }

            // Obtener capacidades de la red
            val capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
            Log.d("Connectivity", "Capacidades de red: ${capabilities?.toString() ?: "NULL"}")

            if (capabilities == null) {
                Log.d("Connectivity", "No se pudieron obtener capacidades de red")
                return false
            }

            // Verificar capacidades individuales
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val hasEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val hasVPN = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            Log.d("Connectivity", """
                Detalles de conexión:
                - Tiene Internet: $hasInternet
                - Red validada: $isValidated
                - WiFi: $hasWifi
                - Datos móviles: $hasCellular
                - Ethernet: $hasEthernet
                - VPN: $hasVPN
            """.trimIndent())

            // Resultado final
            val result = hasInternet && isValidated &&
                    (hasWifi || hasCellular || hasEthernet || hasVPN)

            Log.d("Connectivity", "Conectividad total: $result")
            return result

        } catch (e: Exception) {
            Log.e("Connectivity", "Error al verificar conectividad", e)
            return false
        }
    }
}