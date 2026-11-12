package com.autocuida.app.ui.vehiculos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocuida.app.ProgramadorAvisos
import com.autocuida.app.R
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.domain.validacion.ErrorValidacion
import com.autocuida.app.domain.validacion.ValidadorKilometros
import com.autocuida.app.ui.comun.EventoUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActualizarKmViewModel(
    estadoGuardado: SavedStateHandle,
    private val repositorio: VehiculoRepository,
    private val avisos: ProgramadorAvisos
) : ViewModel() {

    private val vehiculoId: Long = requireNotNull(estadoGuardado[DetalleVehiculoFragment.ARG_VEHICULO_ID])

    data class UiState(
        val vehiculo: Vehiculo? = null,
        val kmMaximoHistorial: Int? = null,
        val error: ErrorValidacion? = null
    )

    private val _estado = MutableStateFlow(UiState())
    val estado: StateFlow<UiState> = _estado.asStateFlow()

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            _estado.update {
                it.copy(
                    vehiculo = repositorio.obtenerPorId(vehiculoId),
                    kmMaximoHistorial = repositorio.kmMaximoHistorial(vehiculoId)
                )
            }
        }
    }

    fun guardar(texto: String, esCorreccion: Boolean) {
        val actual = _estado.value
        val vehiculo = actual.vehiculo ?: return
        when (val r = ValidadorKilometros.validar(texto, vehiculo.kmActuales, actual.kmMaximoHistorial, esCorreccion)) {
            is ValidadorKilometros.Resultado.Invalido -> _estado.update { it.copy(error = r.error) }
            is ValidadorKilometros.Resultado.Valido -> viewModelScope.launch {
                try {
                    repositorio.actualizarKm(vehiculoId, r.km)
                    // Los estados dependen de los kilómetros: se revisan los avisos de inmediato.
                    avisos.revisarAhora()
                    _eventos.send(EventoUi.Guardado(R.string.km_actualizados))
                } catch (e: Exception) {
                    _eventos.send(EventoUi.Mensaje(R.string.error_generico))
                }
            }
        }
    }
}
