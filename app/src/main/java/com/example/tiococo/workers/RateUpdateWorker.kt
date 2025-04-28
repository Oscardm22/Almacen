package com.example.tiococo.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.tiococo.data.managers.ExchangeRateManager
import com.example.tiococo.data.managers.NoInternetException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RateUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando actualización de tasa...")
            val rateResult = ExchangeRateManager.getCurrentRateWithStatus(applicationContext, true)

            when {
                rateResult.error is NoInternetException -> {
                    Log.w(TAG, "Sin conexión a internet")
                    Result.retry()
                }
                rateResult.isFromCache -> {
                    Log.w(TAG, "Se usó tasa en caché - Reintentando en 1 hora")
                    scheduleRetry(applicationContext)
                    Result.failure()
                }
                else -> {
                    Log.i(TAG, "Tasa actualizada correctamente: ${rateResult.rate}")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico", e)
            scheduleRetry(applicationContext)
            Result.failure()
        }
    }

    private fun scheduleRetry(context: Context) {
        val retryRequest = OneTimeWorkRequest.Builder(RateUpdateWorker::class.java)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                RETRY_DELAY_HOURS,
                TimeUnit.HOURS
            )
            .build()

        WorkManager.getInstance(context).enqueue(retryRequest)
    }

    companion object {
        const val WORK_TAG = "rateUpdate"
        private const val TAG = "RateUpdateWorker"
        private const val RETRY_DELAY_HOURS = 1L

        // Método mejorado para programar el Worker
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<RateUpdateWorker>(
                4, // Cada 4 horas
                TimeUnit.HOURS
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).addTag(WORK_TAG).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "rateUpdateWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}