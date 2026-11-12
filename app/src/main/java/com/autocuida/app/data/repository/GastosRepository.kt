package com.autocuida.app.data.repository

import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.GastoAgrupado
import com.autocuida.app.data.entity.GastoPorPeriodo
import com.autocuida.app.data.entity.MantenimientoDetalle
import com.autocuida.app.domain.PeriodoGastos
import kotlinx.coroutines.flow.Flow

/** Consultas de estadísticas económicas calculadas sobre los mantenimientos registrados. */
class GastosRepository(db: AppDatabase) {

    private val dao = db.mantenimientoDao()

    fun observarAnios(): Flow<List<Int>> = dao.observarAniosConGastos()

    fun observarTotal(periodo: PeriodoGastos, vehiculoId: Long?): Flow<Double> =
        dao.observarGastoTotal(periodo.desde.toEpochDay(), periodo.hasta.toEpochDay(), vehiculoId)

    fun observarPorTipo(periodo: PeriodoGastos, vehiculoId: Long?): Flow<List<GastoAgrupado>> =
        dao.observarGastosPorTipo(periodo.desde.toEpochDay(), periodo.hasta.toEpochDay(), vehiculoId)

    fun observarPorVehiculo(periodo: PeriodoGastos, vehiculoId: Long?): Flow<List<GastoAgrupado>> =
        dao.observarGastosPorVehiculo(periodo.desde.toEpochDay(), periodo.hasta.toEpochDay(), vehiculoId)

    /** Serie de la gráfica: por meses si el periodo es un año, por años si es todo el historial. */
    fun observarSerie(periodo: PeriodoGastos, vehiculoId: Long?): Flow<List<GastoPorPeriodo>> {
        val desde = periodo.desde.toEpochDay()
        val hasta = periodo.hasta.toEpochDay()
        return when (periodo) {
            is PeriodoGastos.Anio -> dao.observarGastosMensuales(desde, hasta, vehiculoId)
            PeriodoGastos.Todo -> dao.observarGastosAnuales(desde, hasta, vehiculoId)
        }
    }

    fun observarHistorial(periodo: PeriodoGastos, vehiculoId: Long?): Flow<List<MantenimientoDetalle>> =
        dao.observarEnPeriodo(periodo.desde.toEpochDay(), periodo.hasta.toEpochDay(), vehiculoId)
}
