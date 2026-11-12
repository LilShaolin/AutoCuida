package com.autocuida.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autocuida.app.data.entity.TipoMantenimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface TipoMantenimientoDao {

    @Query("SELECT * FROM tipos_mantenimiento ORDER BY id")
    fun observarTodos(): Flow<List<TipoMantenimiento>>

    @Query("SELECT * FROM tipos_mantenimiento ORDER BY id")
    suspend fun obtenerTodos(): List<TipoMantenimiento>

    @Query("SELECT * FROM tipos_mantenimiento WHERE id = :id")
    suspend fun obtenerPorId(id: Long): TipoMantenimiento?

    @Query("SELECT * FROM tipos_mantenimiento WHERE codigo = :codigo")
    suspend fun obtenerPorCodigo(codigo: String): TipoMantenimiento?

    @Query("SELECT EXISTS(SELECT 1 FROM tipos_mantenimiento WHERE nombre = :nombre COLLATE NOCASE AND id <> :excluirId)")
    suspend fun existeNombre(nombre: String, excluirId: Long = 0): Boolean

    @Query("SELECT COUNT(*) FROM mantenimientos WHERE tipo_id = :tipoId")
    suspend fun contarMantenimientos(tipoId: Long): Int

    /** Número de mantenimientos registrados por cada tipo, para la pantalla de tipos. */
    @Query("SELECT tipo_id AS id, COUNT(*) AS cantidad FROM mantenimientos GROUP BY tipo_id")
    fun observarUsoPorTipo(): Flow<List<UsoTipo>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(tipo: TipoMantenimiento): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun actualizar(tipo: TipoMantenimiento)

    /** Lanza `SQLiteConstraintException` si el tipo tiene mantenimientos (RESTRICT). */
    @Delete
    suspend fun eliminar(tipo: TipoMantenimiento)

    data class UsoTipo(val id: Long, val cantidad: Int)
}
