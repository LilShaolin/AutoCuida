package com.autocuida.app.data.repository

import com.autocuida.app.data.entity.RecordatorioDetalle
import com.autocuida.app.domain.CalculadoraMantenimiento
import com.autocuida.app.domain.EvaluacionVencimiento
import com.autocuida.app.domain.ProximoVencimiento
import java.time.LocalDate

/** Evalúa un recordatorio almacenado con los kilómetros actuales del vehículo y la fecha indicada. */
fun RecordatorioDetalle.evaluar(hoy: LocalDate): EvaluacionVencimiento {
    val proximo = ProximoVencimiento(
        kmLimite = recordatorio.kmLimite,
        fechaLimite = recordatorio.fechaLimite?.let(LocalDate::ofEpochDay)
    )
    return CalculadoraMantenimiento.evaluar(proximo, kmActuales, hoy, intervaloKm, intervaloMeses)
}
