package com.autocuida.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.Vehiculo
import kotlinx.coroutines.flow.Flow

class VehiculoRepository(
    private val db: AppDatabase,
    private val recordatorios: RecordatorioRepository
) {
    private val vehiculoDao = db.vehiculoDao()
    private val mantenimientoDao = db.mantenimientoDao()

    sealed interface ResultadoGuardado {
        data class Guardado(val id: Long) : ResultadoGuardado
        data object MatriculaDuplicada : ResultadoGuardado
    }

    fun observarTodos(): Flow<List<Vehiculo>> = vehiculoDao.observarTodos()

    fun observarPorId(id: Long): Flow<Vehiculo?> = vehiculoDao.observarPorId(id)

    suspend fun obtenerPorId(id: Long): Vehiculo? = vehiculoDao.obtenerPorId(id)

    suspend fun kmMaximoHistorial(vehiculoId: Long): Int? = mantenimientoDao.kmMaximoRegistrado(vehiculoId)

    /** Inserta o actualiza el vehículo y recalcula sus recordatorios (la ITV puede haber cambiado). */
    suspend fun guardar(vehiculo: Vehiculo): ResultadoGuardado {
        if (vehiculoDao.existeMatricula(vehiculo.matricula, vehiculo.id)) {
            return ResultadoGuardado.MatriculaDuplicada
        }
        return try {
            db.withTransaction {
                val id = if (vehiculo.id == 0L) {
                    vehiculoDao.insertar(vehiculo)
                } else {
                    vehiculoDao.actualizar(vehiculo)
                    vehiculo.id
                }
                recordatorios.recalcularVehiculo(id)
                ResultadoGuardado.Guardado(id)
            }
        } catch (e: SQLiteConstraintException) {
            // Condición de carrera improbable: otra operación insertó la misma matrícula.
            ResultadoGuardado.MatriculaDuplicada
        }
    }

    suspend fun actualizarKm(vehiculoId: Long, km: Int) = db.withTransaction {
        vehiculoDao.actualizarKm(vehiculoId, km)
        recordatorios.recalcularVehiculo(vehiculoId)
    }

    /** Elimina el vehículo; mantenimientos y recordatorios se eliminan en cascada. */
    suspend fun eliminar(vehiculo: Vehiculo) = vehiculoDao.eliminar(vehiculo)
}
