package com.autocuida.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autocuida.app.AutoCuidaApp
import com.autocuida.app.data.repository.evaluar
import com.autocuida.app.domain.EvaluadorAvisos
import com.autocuida.app.util.Fechas
import com.autocuida.app.util.Notificaciones

/**
 * Revisa todos los recordatorios y notifica los mantenimientos e ITV próximos o vencidos.
 *
 * Para evitar duplicados, cada recordatorio guarda el último nivel avisado y solo se notifica
 * cuando el estado empeora. El nivel se marca únicamente si la notificación pudo mostrarse,
 * de modo que, si el usuario concede el permiso más tarde, recibirá los avisos pendientes.
 */
class RevisionVencimientosWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repositorio = (applicationContext as AutoCuidaApp).contenedor.recordatorioRepository
        return try {
            val hoy = Fechas.hoy()
            for (detalle in repositorio.obtenerTodosConDetalle()) {
                val evaluacion = detalle.evaluar(hoy)
                val decision = EvaluadorAvisos.decidir(evaluacion.estado, detalle.recordatorio.avisado)
                when {
                    decision.notificar -> {
                        if (Notificaciones.mostrarVencimiento(applicationContext, detalle, evaluacion)) {
                            repositorio.marcarAvisado(detalle.recordatorio.id, decision.nuevoNivel)
                        }
                    }
                    decision.nuevoNivel != detalle.recordatorio.avisado ->
                        repositorio.marcarAvisado(detalle.recordatorio.id, decision.nuevoNivel)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error al revisar vencimientos", e)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "RevisionVencimientos"
    }
}
