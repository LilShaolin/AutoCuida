package com.autocuida.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/** Mantenimiento con el nombre de su tipo y la matrícula del vehículo, para listados. */
data class MantenimientoDetalle(
    @Embedded val mantenimiento: Mantenimiento,
    @ColumnInfo(name = "tipo_nombre") val tipoNombre: String,
    @ColumnInfo(name = "vehiculo_descripcion") val vehiculoDescripcion: String,
    @ColumnInfo(name = "vehiculo_matricula") val vehiculoMatricula: String
)

/** Recordatorio acompañado de los datos necesarios para evaluarlo y notificarlo. */
data class RecordatorioDetalle(
    @Embedded val recordatorio: Recordatorio,
    @ColumnInfo(name = "tipo_nombre") val tipoNombre: String,
    @ColumnInfo(name = "tipo_codigo") val tipoCodigo: String?,
    @ColumnInfo(name = "intervalo_km") val intervaloKm: Int?,
    @ColumnInfo(name = "intervalo_meses") val intervaloMeses: Int?,
    @ColumnInfo(name = "vehiculo_descripcion") val vehiculoDescripcion: String,
    @ColumnInfo(name = "vehiculo_matricula") val vehiculoMatricula: String,
    @ColumnInfo(name = "km_actuales") val kmActuales: Int
)

/** Resultado de una agregación de gastos (por tipo o por vehículo). */
data class GastoAgrupado(
    val id: Long,
    val nombre: String,
    val total: Double,
    val cantidad: Int
)

/** Total de gasto de un periodo: mes (1-12) o año (yyyy), según la consulta. */
data class GastoPorPeriodo(
    val periodo: Int,
    val total: Double
)

/** Referencia del último mantenimiento de un tipo para un vehículo. */
data class UltimoMantenimiento(
    @ColumnInfo(name = "tipo_id") val tipoId: Long,
    val fecha: Long,
    val km: Int
)
