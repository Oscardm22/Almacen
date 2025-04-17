package com.example.tiococo.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tiococo.data.managers.ExchangeRateManager
import com.example.tiococo.utils.ConnectivityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RateUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Worker de actualización de tasa iniciado")

        try {
            if (!ConnectivityChecker(applicationContext).hasInternet()) {
                Log.w(TAG, "Sin conexión a internet - Reintentando")
                return@withContext Result.retry()
            }

            val newRate = ExchangeRateManager.getCurrentRate(applicationContext, true)

            when {
                newRate != previousRate -> {
                    Log.i(TAG, "Tasa actualizada: $previousRate → $newRate")
                    Result.success()
                }
                else -> {
                    Log.w(TAG, "La tasa no cambió ($newRate)")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en la actualización", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_TAG = "rateUpdate"  // Cambiado de WORK_NAME a WORK_TAG
        private const val TAG = "RateUpdateWorker"
    }
}