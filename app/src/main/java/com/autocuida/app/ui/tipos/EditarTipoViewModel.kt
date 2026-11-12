package com.autocuida.app.ui.tipos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocuida.app.ProgramadorAvisos
import com.autocuida.app.R
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.repository.TipoMantenimientoRepository
import com.autocuida.app.domain.validacion.ErrorValidacion
import com.autocuida.app.domain.validacion.ValidadorTipo
import com.autocuida.app.ui.comun.EventoUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditarTipoViewModel(
    estadoGuardado: SavedStateHandle,
    private val repositorio: TipoMantenimientoRepository,
    private val avisos: ProgramadorAvisos
) : ViewModel() {

    private val tipoId: Long = estadoGuardado[EditarTipoDialogFragment.ARG_TIPO_ID] ?: 0L
    val esEdicion: Boolean get() = tipoId != 0L

    data class UiState(
        val original: TipoMantenimiento? = null,
        val errores: Map<ValidadorTipo.Campo, ErrorValidacion> = emptyMap()
    )

    private val _estado = MutableStateFlow(UiState())
    val estado: StateFlow<UiState> = _estado.asStateFlow()

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    var camposRellenados = false

    init {
        if (esEdicion) viewModelScope.launch {
            _estado.update { it.copy(original = repositorio.obtenerPorId(tipoId)) }
        }
    }

    fun guardar(entrada: ValidadorTipo.Entrada) {
        val resultado = ValidadorTipo.validar(entrada)
        if (!resultado.esValido) {
            _estado.update { it.copy(errores = resultado.errores) }
            return
        }
        val tipo = (_estado.value.original ?: TipoMantenimiento(nombre = "", intervaloKm = null, intervaloMeses = null)).copy(
            nombre = entrada.nombre.trim(),
            intervaloKm = entrada.intervaloKm.trim().toIntOrNull(),
            intervaloMeses = entrada.intervaloMeses.trim().toIntOrNull()
        )
        viewModelScope.launch {
            try {
                when (repositorio.guardar(tipo)) {
                    TipoMantenimientoRepository.ResultadoGuardado.Guardado -> {
                        // Los intervalos influyen en todos los vencimientos: se revisan los avisos.
                        avisos.revisarAhora()
                        _eventos.send(EventoUi.Guardado(R.string.tipo_guardado))
                    }
                    TipoMantenimientoRepository.ResultadoGuardado.NombreDuplicado -> _estado.update {
                        it.copy(errores = mapOf(ValidadorTipo.Campo.NOMBRE to ErrorValidacion.NOMBRE_DUPLICADO))
                    }
                }
            } catch (e: Exception) {
                _eventos.send(EventoUi.Mensaje(R.string.error_generico))
            }
        }
    }
}
