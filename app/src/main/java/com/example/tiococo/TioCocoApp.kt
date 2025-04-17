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
    }

    private fun scheduleRateUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<RateUpdateWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        ).addTag(RateUpdateWorker.WORK_TAG)  // Usando el tag
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "rateUpdateUniqueWork",  // Nombre único diferente al tag
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        setupWorkManagerDebugging()
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