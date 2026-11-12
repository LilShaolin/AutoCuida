package com.autocuida.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Vehículo registrado por el usuario.
 *
 * Las fechas se almacenan como día epoch (`LocalDate.toEpochDay()`), lo que evita
 * desfases por zona horaria y permite filtrar por rangos con enteros.
 *
 * @property fechaItv fecha de la próxima inspección técnica (vencimiento de la ITV vigente).
 */
@Entity(
    tableName = "vehiculos",
    indices = [Index(value = ["matricula"], unique = true)]
)
data class Vehiculo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val marca: String,
    val modelo: String,
    val matricula: String,
    val anio: Int,
    val combustible: String,
    @ColumnInfo(name = "km_actuales")
    val kmActuales: Int,
    @ColumnInfo(name = "fecha_itv")
    val fechaItv: Long
)
