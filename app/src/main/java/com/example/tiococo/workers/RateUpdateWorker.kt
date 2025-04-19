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

            if (rateResult.isFromCache) {
                Log.w(TAG, "Se usó tasa en caché - Reintentando en 1 hora")
                // Crear un nuevo WorkRequest con backoff configurado
                val retryRequest = OneTimeWorkRequest.Builder(RateUpdateWorker::class.java)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        RETRY_DELAY_HOURS,
                        TimeUnit.HOURS
                    )
                    .build()

                WorkManager.getInstance(applicationContext).enqueue(retryRequest)
                return@withContext Result.failure()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar tasa: ${e.message}")
            Result.retry()
        }
    }

    private fun notifyRateChange(newRate: Double) {
        // Aquí puedes implementar la notificación a tu ViewModel
        // o actualizar directamente la base de datos
        Log.d(TAG, "Notificando cambio de tasa: $newRate")
    }

    companion object {
        const val WORK_TAG = "rateUpdate"
        private const val TAG = "RateUpdateWorker"
    }
}