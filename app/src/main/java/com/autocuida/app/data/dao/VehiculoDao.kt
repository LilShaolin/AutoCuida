package com.autocuida.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autocuida.app.data.entity.Vehiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiculoDao {

    @Query("SELECT * FROM vehiculos ORDER BY marca COLLATE NOCASE, modelo COLLATE NOCASE")
    fun observarTodos(): Flow<List<Vehiculo>>

    @Query("SELECT * FROM vehiculos WHERE id = :id")
    fun observarPorId(id: Long): Flow<Vehiculo?>

    @Query("SELECT * FROM vehiculos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Vehiculo?

    @Query("SELECT * FROM vehiculos")
    suspend fun obtenerTodos(): List<Vehiculo>

    @Query("SELECT EXISTS(SELECT 1 FROM vehiculos WHERE matricula = :matricula AND id <> :excluirId)")
    suspend fun existeMatricula(matricula: String, excluirId: Long = 0): Boolean

    /** Lanza `SQLiteConstraintException` si la matrícula ya existe. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(vehiculo: Vehiculo): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun actualizar(vehiculo: Vehiculo)

    @Query("UPDATE vehiculos SET km_actuales = :km WHERE id = :id")
    suspend fun actualizarKm(id: Long, km: Int)

    @Query("UPDATE vehiculos SET fecha_itv = :fechaItv WHERE id = :id")
    suspend fun actualizarFechaItv(id: Long, fechaItv: Long)

    @Delete
    suspend fun eliminar(vehiculo: Vehiculo)
}
