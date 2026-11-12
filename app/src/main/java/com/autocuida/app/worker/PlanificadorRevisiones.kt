package com.autocuida.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Programa la revisión periódica de vencimientos con WorkManager. */
object PlanificadorRevisiones {

    const val TRABAJO_DIARIO = "revision_vencimientos_diaria"
    const val TRABAJO_INMEDIATO = "revision_vencimientos_inmediata"

    /** Hora local preferente para la revisión diaria. WorkManager puede retrasarla por optimización de batería. */
    private val HORA_REVISION: LocalTime = LocalTime.of(9, 0)

    /**
     * Encola la revisión diaria. Con KEEP no se reprograma si ya existe, por lo que se puede
     * llamar en cada arranque. WorkManager mantiene el trabajo aunque la aplicación esté cerrada
     * y lo restaura tras reiniciar el dispositivo.
     */
    fun programarRevisionDiaria(context: Context) {
        val peticion = PeriodicWorkRequestBuilder<RevisionVencimientosWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(retrasoHastaProximaRevision(LocalDateTime.now()).toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TRABAJO_DIARIO, ExistingPeriodicWorkPolicy.KEEP, peticion)
    }

    /** Revisión puntual tras guardar datos, para avisar sin esperar al ciclo diario. */
    fun revisarAhora(context: Context) {
        val peticion = OneTimeWorkRequestBuilder<RevisionVencimientosWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(TRABAJO_INMEDIATO, ExistingWorkPolicy.REPLACE, peticion)
    }

    internal fun retrasoHastaProximaRevision(ahora: LocalDateTime): Duration {
        var siguiente = ahora.toLocalDate().atTime(HORA_REVISION)
        if (!siguiente.isAfter(ahora)) siguiente = siguiente.plusDays(1)
        return Duration.between(ahora, siguiente)
    }
}
