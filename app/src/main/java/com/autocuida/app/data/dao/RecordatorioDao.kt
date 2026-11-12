package com.autocuida.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autocuida.app.data.entity.Recordatorio
import com.autocuida.app.data.entity.RecordatorioDetalle
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {

    /** Lanza `SQLiteConstraintException` si ya existe un recordatorio para el par vehículo-tipo. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(recordatorio: Recordatorio): Long

    @Update
    suspend fun actualizar(recordatorio: Recordatorio)

    @Query("SELECT * FROM recordatorios WHERE vehiculo_id = :vehiculoId")
    suspend fun obtenerPorVehiculo(vehiculoId: Long): List<Recordatorio>

    @Query("SELECT * FROM recordatorios WHERE vehiculo_id = :vehiculoId AND tipo_id = :tipoId")
    suspend fun obtener(vehiculoId: Long, tipoId: Long): Recordatorio?

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    @Query("UPDATE recordatorios SET avisado = :nivel WHERE id = :id")
    suspend fun marcarAvisado(id: Long, nivel: Int)

    @Query("$DETALLE ORDER BY r.vehiculo_id, t.id")
    fun observarTodosConDetalle(): Flow<List<RecordatorioDetalle>>

    @Query("$DETALLE WHERE r.vehiculo_id = :vehiculoId ORDER BY t.id")
    fun observarPorVehiculo(vehiculoId: Long): Flow<List<RecordatorioDetalle>>

    @Query(DETALLE)
    suspend fun obtenerTodosConDetalle(): List<RecordatorioDetalle>

    companion object {
        private const val DETALLE = """
            SELECT r.*, t.nombre AS tipo_nombre, t.codigo AS tipo_codigo,
                   t.intervalo_km AS intervalo_km, t.intervalo_meses AS intervalo_meses,
                   v.marca || ' ' || v.modelo AS vehiculo_descripcion,
                   v.matricula AS vehiculo_matricula, v.km_actuales AS km_actuales
            FROM recordatorios r
            INNER JOIN tipos_mantenimiento t ON t.id = r.tipo_id
            INNER JOIN vehiculos v ON v.id = r.vehiculo_id
        """
    }
}
