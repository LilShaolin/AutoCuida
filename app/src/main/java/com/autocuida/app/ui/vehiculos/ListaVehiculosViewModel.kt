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
        val avisos: Int get() = vehiculos.count { it.tieneAviso }
    }

    val estado: StateFlow<UiState> = combine(
        vehiculoRepository.observarTodos(),
        recordatorioRepository.observarTodos()
    ) { vehiculos, recordatorios ->
        UiState(cargando = false, vehiculos = ResumenVehiculo.construir(vehiculos, recordatorios, Fechas.hoy()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())
}
