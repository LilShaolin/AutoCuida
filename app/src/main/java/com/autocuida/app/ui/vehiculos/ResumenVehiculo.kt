package com.autocuida.app.ui.vehiculos

import com.autocuida.app.data.entity.RecordatorioDetalle
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.data.repository.evaluar
import com.autocuida.app.domain.CalculadoraMantenimiento
import com.autocuida.app.domain.EstadoMantenimiento
import com.autocuida.app.domain.EvaluacionVencimiento
import java.time.LocalDate

/** Vencimiento evaluado con el nombre del tipo al que pertenece. */
data class VencimientoEvaluado(
    val tipoId: Long,
    val tipoNombre: String,
    val esItv: Boolean,
    val evaluacion: EvaluacionVencimiento
)

/** Datos de una tarjeta de la lista de vehículos. */
data class ResumenVehiculo(
    val vehiculo: Vehiculo,
    val estado: EstadoMantenimiento,
    val itv: EvaluacionVencimiento,
    /** Vencimiento más urgente, o `null` si no hay ninguno calculado. */
    val masUrgente: VencimientoEvaluado?,
    /** Vencimientos próximos o vencidos, incluida la ITV. */
    val avisos: Int = 0
) {

    companion object {

        /**
         * Orden de urgencia: primero el estado más grave y, dentro de él, lo que antes vence. Los km y
         * los días se comparan en la misma escala con [CalculadoraMantenimiento.margenRelativo].
         */
        val ORDEN_URGENCIA: Comparator<VencimientoEvaluado> =
            compareByDescending<VencimientoEvaluado> { it.evaluacion.estado }
                .thenBy { CalculadoraMantenimiento.margenRelativo(it.evaluacion) ?: Double.MAX_VALUE }

        fun construir(
            vehiculos: List<Vehiculo>,
            recordatorios: List<RecordatorioDetalle>,
            hoy: LocalDate
        ): List<ResumenVehiculo> {
            val porVehiculo = recordatorios.groupBy { it.recordatorio.vehiculoId }
            return vehiculos.map { vehiculo ->
                val evaluados = porVehiculo[vehiculo.id].orEmpty().map { it.aVencimiento(hoy) }
                // La ITV se evalúa aparte con la fecha del vehículo; el resto son los mantenimientos.
                val mantenimientos = evaluados.filterNot { it.esItv }.map { it.evaluacion }
                val itv = CalculadoraMantenimiento.evaluarItv(LocalDate.ofEpochDay(vehiculo.fechaItv), hoy)
                ResumenVehiculo(
                    vehiculo = vehiculo,
                    estado = CalculadoraMantenimiento.estadoGeneralVehiculo(mantenimientos, itv),
                    itv = itv,
                    masUrgente = evaluados.minWithOrNull(ORDEN_URGENCIA),
                    avisos = CalculadoraMantenimiento.contarAvisos(mantenimientos + itv)
                )
            }
        }
    }
}

fun RecordatorioDetalle.aVencimiento(hoy: LocalDate) = VencimientoEvaluado(
    tipoId = recordatorio.tipoId,
    tipoNombre = tipoNombre,
    esItv = tipoCodigo == com.autocuida.app.data.entity.TipoMantenimiento.CODIGO_ITV,
    evaluacion = evaluar(hoy)
)
