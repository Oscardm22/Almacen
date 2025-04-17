package com.example.tiococo

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.work.*
import com.example.tiococo.workers.RateUpdateWorker
import java.util.concurrent.TimeUnit
import android.util.Log
import com.example.tiococo.data.managers.ExchangeRateManager

class TioCocoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ExchangeRateManager.initialize(applicationContext)
        scheduleRateUpdates()

        if (applicationContext.applicationInfo != null &&
            (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            setupWorkManagerDebugging()
        }
    }

    private fun scheduleRateUpdates() {
        Log.d("TioCocoApp", "Configurando WorkManager para actualizaciones de tasa")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<RateUpdateWorker>(
            1, TimeUnit.HOURS, // Intervalo
            15, TimeUnit.MINUTES // Flexibilidad
        ).setConstraints(constraints)
            .addTag(RateUpdateWorker.WORK_TAG)
            .build()

        WorkManager.getInstance(this).apply {
            // Cancelar trabajos previos para evitar duplicados
            cancelAllWorkByTag(RateUpdateWorker.WORK_TAG)

            // Programar nuevo trabajo
            enqueueUniquePeriodicWork(
                "rateUpdateUniqueWork",
                ExistingPeriodicWorkPolicy.REPLACE, // Usar REPLACE en lugar de UPDATE
                request
            )

            // Verificar programación
            getWorkInfosByTagLiveData(RateUpdateWorker.WORK_TAG)
                .observeForever { workInfos ->
                    workInfos?.forEach { info ->
                        Log.d("WorkManager", "Estado del trabajo: ${info.state}")
                    }
                }
        }
    }

    // Función separada para el debug (opcional)
    private fun setupWorkManagerDebugging() {
        WorkManager.getInstance(this)
            .getWorkInfosByTagLiveData(RateUpdateWorker.WORK_TAG)  // Usando el tag
            .observeForever { workInfos ->
                workInfos?.forEach { info ->
                    Log.d("WorkManagerDebug", "Estado: ${info.state}")
                }
            }
    }
}