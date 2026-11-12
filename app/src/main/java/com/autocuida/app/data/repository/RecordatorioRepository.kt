package com.autocuida.app.data.repository

import androidx.room.withTransaction
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.Recordatorio
import com.autocuida.app.data.entity.RecordatorioDetalle
import com.autocuida.app.domain.CalculadoraMantenimiento
import com.autocuida.app.domain.EvaluadorAvisos
import com.autocuida.app.domain.ProximoVencimiento
import com.autocuida.app.domain.ReferenciaMantenimiento
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Mantiene sincronizada la tabla de recordatorios con el historial, los intervalos y la ITV.
 *
 * Para cada vehículo y tipo:
 * - ITV: el límite es la fecha de ITV del vehículo.
 * - Resto: el límite se calcula con [CalculadoraMantenimiento] a partir del último mantenimiento.
 * - Sin referencia: no existe recordatorio (no se inventan fechas).
 *
 * Si los límites cambian, el nivel avisado se reinicia para que el nuevo vencimiento pueda notificarse.
 */
class RecordatorioRepository(private val db: AppDatabase) {

    private val recordatorioDao = db.recordatorioDao()
    private val vehiculoDao = db.vehiculoDao()
    private val tipoDao = db.tipoMantenimientoDao()
    private val mantenimientoDao = db.mantenimientoDao()

    fun observarTodos(): Flow<List<RecordatorioDetalle>> = recordatorioDao.observarTodosConDetalle()

    fun observarPorVehiculo(vehiculoId: Long): Flow<List<RecordatorioDetalle>> =
        recordatorioDao.observarPorVehiculo(vehiculoId)

    suspend fun obtenerTodosConDetalle(): List<RecordatorioDetalle> = recordatorioDao.obtenerTodosConDetalle()

    suspend fun marcarAvisado(id: Long, nivel: Int) = recordatorioDao.marcarAvisado(id, nivel)

    suspend fun recalcularTodos() = db.withTransaction {
        vehiculoDao.obtenerTodos().forEach { recalcularVehiculo(it.id) }
    }

    suspend fun recalcularVehiculo(vehiculoId: Long) = db.withTransaction {
        val vehiculo = vehiculoDao.obtenerPorId(vehiculoId) ?: return@withTransaction
        val tipos = tipoDao.obtenerTodos()
        val ultimos = mantenimientoDao.obtenerUltimosPorTipo(vehiculoId).associateBy { it.tipoId }
        val existentes = recordatorioDao.obtenerPorVehiculo(vehiculoId).associateBy { it.tipoId }

        for (tipo in tipos) {
            val proximo: ProximoVencimiento? = if (tipo.esItv) {
                ProximoVencimiento(kmLimite = null, fechaLimite = LocalDate.ofEpochDay(vehiculo.fechaItv))
            } else {
                val referencia = ultimos[tipo.id]?.let {
                    ReferenciaMantenimiento(LocalDate.ofEpochDay(it.fecha), it.km)
                }
                CalculadoraMantenimiento.calcularProximo(referencia, tipo.intervaloKm, tipo.intervaloMeses)
            }
            val existente = existentes[tipo.id]
            val nuevaFecha = proximo?.fechaLimite?.toEpochDay()
            val nuevoKm = proximo?.kmLimite

            when {
                proximo == null -> existente?.let { recordatorioDao.eliminarPorId(it.id) }
                existente == null -> recordatorioDao.insertar(
                    Recordatorio(vehiculoId = vehiculoId, tipoId = tipo.id, fechaLimite = nuevaFecha, kmLimite = nuevoKm)
                )
                existente.fechaLimite != nuevaFecha || existente.kmLimite != nuevoKm -> recordatorioDao.actualizar(
                    existente.copy(fechaLimite = nuevaFecha, kmLimite = nuevoKm, avisado = EvaluadorAvisos.NIVEL_NINGUNO)
                )
            }
        }
    }
}
