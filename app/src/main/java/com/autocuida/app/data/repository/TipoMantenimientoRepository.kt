package com.autocuida.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.dao.TipoMantenimientoDao
import com.autocuida.app.data.entity.TipoMantenimiento
import kotlinx.coroutines.flow.Flow

class TipoMantenimientoRepository(
    private val db: AppDatabase,
    private val recordatorios: RecordatorioRepository
) {
    private val tipoDao = db.tipoMantenimientoDao()

    sealed interface ResultadoGuardado {
        data object Guardado : ResultadoGuardado
        data object NombreDuplicado : ResultadoGuardado
    }

    sealed interface ResultadoEliminacion {
        data object Eliminado : ResultadoEliminacion
        data class EnUso(val mantenimientos: Int) : ResultadoEliminacion
        data object Protegido : ResultadoEliminacion
    }

    fun observarTodos(): Flow<List<TipoMantenimiento>> = tipoDao.observarTodos()

    fun observarUso(): Flow<List<TipoMantenimientoDao.UsoTipo>> = tipoDao.observarUsoPorTipo()

    suspend fun obtenerTodos(): List<TipoMantenimiento> = tipoDao.obtenerTodos()

    suspend fun obtenerPorId(id: Long): TipoMantenimiento? = tipoDao.obtenerPorId(id)

    /** Guarda el tipo y recalcula todos los recordatorios, ya que los intervalos pueden haber cambiado. */
    suspend fun guardar(tipo: TipoMantenimiento): ResultadoGuardado {
        if (tipoDao.existeNombre(tipo.nombre, tipo.id)) return ResultadoGuardado.NombreDuplicado
        return try {
            db.withTransaction {
                if (tipo.id == 0L) tipoDao.insertar(tipo) else tipoDao.actualizar(tipo)
                recordatorios.recalcularTodos()
            }
            ResultadoGuardado.Guardado
        } catch (e: SQLiteConstraintException) {
            ResultadoGuardado.NombreDuplicado
        }
    }

    /**
     * Elimina un tipo solo si no tiene mantenimientos. La ITV está protegida porque
     * su vencimiento depende de la fecha guardada en cada vehículo.
     */
    suspend fun eliminar(tipo: TipoMantenimiento): ResultadoEliminacion {
        if (tipo.esItv) return ResultadoEliminacion.Protegido
        val enUso = tipoDao.contarMantenimientos(tipo.id)
        if (enUso > 0) return ResultadoEliminacion.EnUso(enUso)
        return try {
            tipoDao.eliminar(tipo)
            ResultadoEliminacion.Eliminado
        } catch (e: SQLiteConstraintException) {
            // La clave foránea RESTRICT impide el borrado si se registró un mantenimiento entretanto.
            ResultadoEliminacion.EnUso(tipoDao.contarMantenimientos(tipo.id))
        }
    }
}
