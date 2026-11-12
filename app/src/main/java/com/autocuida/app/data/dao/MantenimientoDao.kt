package com.autocuida.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.autocuida.app.data.entity.GastoAgrupado
import com.autocuida.app.data.entity.GastoPorPeriodo
import com.autocuida.app.data.entity.Mantenimiento
import com.autocuida.app.data.entity.MantenimientoDetalle
import com.autocuida.app.data.entity.UltimoMantenimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface MantenimientoDao {

    @Insert
    suspend fun insertar(mantenimiento: Mantenimiento): Long

    @Update
    suspend fun actualizar(mantenimiento: Mantenimiento)

    @Delete
    suspend fun eliminar(mantenimiento: Mantenimiento)

    @Query("SELECT * FROM mantenimientos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Mantenimiento?

    @Query(
        """
        SELECT m.*, t.nombre AS tipo_nombre,
               v.marca || ' ' || v.modelo AS vehiculo_descripcion,
               v.matricula AS vehiculo_matricula
        FROM mantenimientos m
        INNER JOIN tipos_mantenimiento t ON t.id = m.tipo_id
        INNER JOIN vehiculos v ON v.id = m.vehiculo_id
        WHERE m.vehiculo_id = :vehiculoId
        ORDER BY m.fecha DESC, m.km DESC, m.id DESC
        """
    )
    fun observarPorVehiculo(vehiculoId: Long): Flow<List<MantenimientoDetalle>>

    /** Último mantenimiento de cada tipo para un vehículo (el más reciente por fecha y km). */
    @Query(
        """
        SELECT m.tipo_id, m.fecha, m.km FROM mantenimientos m
        WHERE m.vehiculo_id = :vehiculoId AND m.id = (
            SELECT m2.id FROM mantenimientos m2
            WHERE m2.vehiculo_id = m.vehiculo_id AND m2.tipo_id = m.tipo_id
            ORDER BY m2.fecha DESC, m2.km DESC, m2.id DESC LIMIT 1
        )
        """
    )
    suspend fun obtenerUltimosPorTipo(vehiculoId: Long): List<UltimoMantenimiento>

    /** Mayor kilometraje registrado antes de una fecha, excluyendo el registro en edición. */
    @Query("SELECT MAX(km) FROM mantenimientos WHERE vehiculo_id = :vehiculoId AND fecha < :fecha AND id <> :excluirId")
    suspend fun kmMaximoAntesDe(vehiculoId: Long, fecha: Long, excluirId: Long): Int?

    /** Menor kilometraje registrado después de una fecha, excluyendo el registro en edición. */
    @Query("SELECT MIN(km) FROM mantenimientos WHERE vehiculo_id = :vehiculoId AND fecha > :fecha AND id <> :excluirId")
    suspend fun kmMinimoDespuesDe(vehiculoId: Long, fecha: Long, excluirId: Long): Int?

    @Query("SELECT MAX(km) FROM mantenimientos WHERE vehiculo_id = :vehiculoId")
    suspend fun kmMaximoRegistrado(vehiculoId: Long): Int?

    // ---------- Gastos ----------
    // vehiculoId admite null para agregar todos los vehículos.

    @Query(
        """
        SELECT COALESCE(SUM(coste), 0) FROM mantenimientos
        WHERE fecha BETWEEN :desde AND :hasta
          AND (:vehiculoId IS NULL OR vehiculo_id = :vehiculoId)
        """
    )
    fun observarGastoTotal(desde: Long, hasta: Long, vehiculoId: Long?): Flow<Double>

    @Query(
        """
        SELECT t.id AS id, t.nombre AS nombre, SUM(m.coste) AS total, COUNT(*) AS cantidad
        FROM mantenimientos m INNER JOIN tipos_mantenimiento t ON t.id = m.tipo_id
        WHERE m.fecha BETWEEN :desde AND :hasta
          AND (:vehiculoId IS NULL OR m.vehiculo_id = :vehiculoId)
        GROUP BY t.id ORDER BY total DESC
        """
    )
    fun observarGastosPorTipo(desde: Long, hasta: Long, vehiculoId: Long?): Flow<List<GastoAgrupado>>

    @Query(
        """
        SELECT v.id AS id, v.marca || ' ' || v.modelo || ' · ' || v.matricula AS nombre,
               SUM(m.coste) AS total, COUNT(*) AS cantidad
        FROM mantenimientos m INNER JOIN vehiculos v ON v.id = m.vehiculo_id
        WHERE m.fecha BETWEEN :desde AND :hasta
          AND (:vehiculoId IS NULL OR m.vehiculo_id = :vehiculoId)
        GROUP BY v.id ORDER BY total DESC
        """
    )
    fun observarGastosPorVehiculo(desde: Long, hasta: Long, vehiculoId: Long?): Flow<List<GastoAgrupado>>

    /** Gasto por mes natural; `fecha` es un día epoch, por eso se multiplica por 86 400 s. */
    @Query(
        """
        SELECT CAST(strftime('%m', fecha * 86400, 'unixepoch') AS INTEGER) AS periodo,
               SUM(coste) AS total
        FROM mantenimientos
        WHERE fecha BETWEEN :desde AND :hasta
          AND (:vehiculoId IS NULL OR vehiculo_id = :vehiculoId)
        GROUP BY periodo ORDER BY periodo
        """
    )
    fun observarGastosMensuales(desde: Long, hasta: Long, vehiculoId: Long?): Flow<List<GastoPorPeriodo>>

    /** Gasto por año natural, para la vista de todo el historial. */
    @Query(
        """
        SELECT CAST(strftime('%Y', fecha * 86400, 'unixepoch') AS INTEGER) AS periodo,
               SUM(coste) AS total
        FROM mantenimientos
        WHERE fecha BETWEEN :desde AND :hasta
          AND (:vehiculoId IS NULL OR vehiculo_id = :vehiculoId)
        GROUP BY periodo ORDER BY periodo
        """
    )
    fun observarGastosAnuales(desde: Long, hasta: Long, vehiculoId: Long?): Flow<List<GastoPorPeriodo>>

    @Query(
        """
        SELECT m.*, t.nombre AS tipo_nombre,
               v.marca || ' ' || v.modelo AS vehiculo_descripcion,
               v.matricula AS vehiculo_matricula
        FROM mantenimientos m
        INNER JOIN tipos_mantenimiento t ON t.id = m.tipo_id
        INNER JOIN vehiculos v ON v.id = m.vehiculo_id
        WHERE m.fecha BETWEEN :desde AND :hasta
          AND (:vehiculoId IS NULL OR m.vehiculo_id = :vehiculoId)
        ORDER BY m.fecha DESC, m.id DESC
        """
    )
    fun observarEnPeriodo(desde: Long, hasta: Long, vehiculoId: Long?): Flow<List<MantenimientoDetalle>>

    /** Años con mantenimientos registrados, del más reciente al más antiguo. */
    @Query(
        """
        SELECT DISTINCT CAST(strftime('%Y', fecha * 86400, 'unixepoch') AS INTEGER) AS anio
        FROM mantenimientos ORDER BY anio DESC
        """
    )
    fun observarAniosConGastos(): Flow<List<Int>>
}
