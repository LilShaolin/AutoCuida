package com.autocuida.app.data.repository

import androidx.room.withTransaction
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.Mantenimiento
import com.autocuida.app.data.entity.MantenimientoDetalle
import kotlinx.coroutines.flow.Flow

class MantenimientoRepository(
    private val db: AppDatabase,
    private val recordatorios: RecordatorioRepository
) {
    private val mantenimientoDao = db.mantenimientoDao()
    private val vehiculoDao = db.vehiculoDao()

    /** Kilómetros que delimitan un registro en una fecha según el resto del historial. */
    data class LimitesKm(val maximoAnterior: Int?, val minimoPosterior: Int?)

    fun observarPorVehiculo(vehiculoId: Long): Flow<List<MantenimientoDetalle>> =
        mantenimientoDao.observarPorVehiculo(vehiculoId)

    suspend fun obtenerPorId(id: Long): Mantenimiento? = mantenimientoDao.obtenerPorId(id)

    suspend fun limitesKm(vehiculoId: Long, fecha: Long, excluirId: Long): LimitesKm = LimitesKm(
        maximoAnterior = mantenimientoDao.kmMaximoAntesDe(vehiculoId, fecha, excluirId),
        minimoPosterior = mantenimientoDao.kmMinimoDespuesDe(vehiculoId, fecha, excluirId)
    )

    /**
     * Guarda el mantenimiento en una transacción:
     * - Si sus kilómetros superan los actuales del vehículo, se actualiza el cuentakilómetros.
     * - Si es una ITV y se indica la fecha de la siguiente inspección, se actualiza la del vehículo
     *   solo cuando es posterior a la actual: registrar una inspección antigua no adelanta el vencimiento.
     * - Se recalculan los recordatorios del vehículo.
     */
    suspend fun guardar(mantenimiento: Mantenimiento, nuevaFechaItv: Long? = null): Long = db.withTransaction {
        val id = if (mantenimiento.id == 0L) {
            mantenimientoDao.insertar(mantenimiento)
        } else {
            mantenimientoDao.actualizar(mantenimiento)
            mantenimiento.id
        }
        val vehiculo = requireNotNull(vehiculoDao.obtenerPorId(mantenimiento.vehiculoId)) { "Vehículo inexistente" }
        if (mantenimiento.km > vehiculo.kmActuales) {
            vehiculoDao.actualizarKm(vehiculo.id, mantenimiento.km)
        }
        if (nuevaFechaItv != null && nuevaFechaItv > vehiculo.fechaItv) {
            vehiculoDao.actualizarFechaItv(vehiculo.id, nuevaFechaItv)
        }
        recordatorios.recalcularVehiculo(vehiculo.id)
        id
    }

    suspend fun eliminar(mantenimiento: Mantenimiento) = db.withTransaction {
        mantenimientoDao.eliminar(mantenimiento)
        recordatorios.recalcularVehiculo(mantenimiento.vehiculoId)
    }
}
