package com.autocuida.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.autocuida.app.domain.EvaluadorAvisos

/**
 * Próximo vencimiento calculado para un vehículo y un tipo de mantenimiento.
 *
 * Existe como máximo un recordatorio por combinación vehículo-tipo. Se recalcula
 * automáticamente tras cualquier cambio en mantenimientos, intervalos, kilómetros o ITV.
 *
 * @property avisado nivel ya notificado (ver [EvaluadorAvisos]): 0 ninguno, 1 próximo, 2 vencido.
 * Permite avisar una sola vez de cada cambio de estado.
 */
@Entity(
    tableName = "recordatorios",
    foreignKeys = [
        ForeignKey(
            entity = Vehiculo::class,
            parentColumns = ["id"],
            childColumns = ["vehiculo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TipoMantenimiento::class,
            parentColumns = ["id"],
            childColumns = ["tipo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vehiculo_id", "tipo_id"], unique = true),
        Index(value = ["tipo_id"])
    ]
)
data class Recordatorio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vehiculo_id")
    val vehiculoId: Long,
    @ColumnInfo(name = "tipo_id")
    val tipoId: Long,
    /** Día epoch límite, o `null` si el tipo no tiene intervalo temporal. */
    @ColumnInfo(name = "fecha_limite")
    val fechaLimite: Long?,
    /** Kilómetros límite, o `null` si el tipo no tiene intervalo por distancia. */
    @ColumnInfo(name = "km_limite")
    val kmLimite: Int?,
    val avisado: Int = EvaluadorAvisos.NIVEL_NINGUNO
)
