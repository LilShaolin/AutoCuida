package com.autocuida.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mantenimiento realizado sobre un vehículo.
 *
 * - Al eliminar el vehículo se eliminan sus mantenimientos (CASCADE).
 * - No se puede eliminar un tipo que tenga mantenimientos asociados (RESTRICT).
 */
@Entity(
    tableName = "mantenimientos",
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
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["vehiculo_id", "fecha"]),
        Index(value = ["tipo_id"]),
        Index(value = ["fecha"])
    ]
)
data class Mantenimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vehiculo_id")
    val vehiculoId: Long,
    @ColumnInfo(name = "tipo_id")
    val tipoId: Long,
    /** Día epoch en que se realizó el mantenimiento. */
    val fecha: Long,
    val km: Int,
    /** Importe en euros (≥ 0). */
    val coste: Double,
    val taller: String?,
    val notas: String?
)
