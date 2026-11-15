package com.autocuida.app.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocuida.app.data.entity.GastoAgrupado
import com.autocuida.app.data.entity.GastoPorPeriodo
import com.autocuida.app.data.entity.MantenimientoDetalle
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.data.repository.GastosRepository
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.domain.PeriodoGastos
import com.autocuida.app.util.Fechas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class GastosViewModel(
    private val gastosRepository: GastosRepository,
    vehiculoRepository: VehiculoRepository
) : ViewModel() {

    data class Filtros(
        val periodo: PeriodoGastos = PeriodoGastos.Anio(Fechas.hoy().year),
        val vehiculoId: Long? = null
    ) {
        /** Si el vehículo filtrado ya no existe (se ha eliminado), se vuelve a «Todos los vehículos». */
        fun ajustadosA(vehiculosExistentes: Collection<Long>): Filtros =
            if (vehiculoId != null && vehiculoId !in vehiculosExistentes) copy(vehiculoId = null) else this
    }

    data class Datos(
        val total: Double = 0.0,
        val porTipo: List<GastoAgrupado> = emptyList(),
        val porVehiculo: List<GastoAgrupado> = emptyList(),
        val serie: List<GastoPorPeriodo> = emptyList(),
        val historial: List<MantenimientoDetalle> = emptyList()
    )

    data class UiState(
        val cargando: Boolean = true,
        val filtros: Filtros = Filtros(),
        val anios: List<Int> = emptyList(),
        val vehiculos: List<Vehiculo> = emptyList(),
        val datos: Datos = Datos()
    )

    private val filtros = MutableStateFlow(Filtros())

    private val vehiculos = vehiculoRepository.observarTodos()

    /** Filtros que se aplican realmente: descartan un vehículo que ya no existe. */
    private val filtrosAplicados = combine(filtros, vehiculos) { f, lista ->
        f.ajustadosA(lista.map { it.id })
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val datos = filtrosAplicados.flatMapLatest { f ->
        combine(
            gastosRepository.observarTotal(f.periodo, f.vehiculoId),
            gastosRepository.observarPorTipo(f.periodo, f.vehiculoId),
            gastosRepository.observarPorVehiculo(f.periodo, f.vehiculoId),
            gastosRepository.observarSerie(f.periodo, f.vehiculoId),
            gastosRepository.observarHistorial(f.periodo, f.vehiculoId)
        ) { total, porTipo, porVehiculo, serie, historial ->
            f to Datos(total, porTipo, porVehiculo, serie, historial)
        }
    }

    val estado: StateFlow<UiState> = combine(
        datos,
        gastosRepository.observarAnios(),
        vehiculos
    ) { (f, d), anios, listaVehiculos ->
        // El año en curso siempre está disponible aunque todavía no tenga gastos.
        val aniosDisponibles = (anios + Fechas.hoy().year).distinct().sortedDescending()
        UiState(cargando = false, filtros = f, anios = aniosDisponibles, vehiculos = listaVehiculos, datos = d)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun seleccionarPeriodo(periodo: PeriodoGastos) = filtros.update { it.copy(periodo = periodo) }

    fun seleccionarVehiculo(vehiculoId: Long?) = filtros.update { it.copy(vehiculoId = vehiculoId) }
}
