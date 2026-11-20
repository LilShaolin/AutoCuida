package com.autocuida.app.ui.vehiculos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocuida.app.data.repository.RecordatorioRepository
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.util.Fechas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ListaVehiculosViewModel(
    vehiculoRepository: VehiculoRepository,
    recordatorioRepository: RecordatorioRepository
) : ViewModel() {

    data class UiState(
        val cargando: Boolean = true,
        val vehiculos: List<ResumenVehiculo> = emptyList()
    ) {
        /** Vencimientos próximos o vencidos de todos los vehículos, incluida la ITV. */
        val avisos: Int get() = vehiculos.sumOf { it.avisos }
    }

    // La fecha forma parte del cálculo: al cambiar de día los estados se recalculan.
    val estado: StateFlow<UiState> = combine(
        vehiculoRepository.observarTodos(),
        recordatorioRepository.observarTodos(),
        Fechas.flujoHoy()
    ) { vehiculos, recordatorios, hoy ->
        UiState(cargando = false, vehiculos = ResumenVehiculo.construir(vehiculos, recordatorios, hoy))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())
}
