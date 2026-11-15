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
import kotlinx.coroutines.CancellationException

/**
 * Revisa todos los recordatorios y notifica los mantenimientos e ITV próximos o vencidos.
 *
 * Para evitar duplicados, cada recordatorio guarda el último nivel avisado y solo se notifica
 * cuando el estado empeora. El nivel se marca únicamente si la notificación pudo mostrarse,
 * de modo que, si el usuario concede el permiso más tarde, recibirá los avisos pendientes.
 * Al terminar se retiran los avisos de recordatorios eliminados o que vuelven a estar al día.
 */
class RevisionVencimientosWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repositorio = (applicationContext as AutoCuidaApp).contenedor.recordatorioRepository
        return try {
            val hoy = Fechas.hoy()
            // Recordatorios próximos o vencidos: sus avisos se mantienen; el resto se retiran al final.
            val vigentes = mutableSetOf<Long>()
            for (detalle in repositorio.obtenerTodosConDetalle()) {
                val evaluacion = detalle.evaluar(hoy)
                val decision = EvaluadorAvisos.decidir(evaluacion.estado, detalle.recordatorio.avisado)
                if (decision.nuevoNivel != EvaluadorAvisos.NIVEL_NINGUNO) vigentes += detalle.recordatorio.id
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
            Notificaciones.retirarObsoletos(applicationContext, vigentes)
            Result.success()
        } catch (e: CancellationException) {
            // La cancelación (por ejemplo, al sustituir la revisión inmediata) no es un error.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al revisar vencimientos", e)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "RevisionVencimientos"
    }
}
