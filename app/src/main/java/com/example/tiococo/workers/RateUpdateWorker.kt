package com.example.tiococo.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
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
            val previousRate = ExchangeRateManager.currentRate

            if (newRate != previousRate) {
                notifyRateChange(newRate)
                Log.i(TAG, "Tasa actualizada de $previousRate a $newRate")
            } else {
                Log.i(TAG, "La tasa no ha cambiado ($newRate)")
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