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
    private val estadoGuardado: SavedStateHandle,
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

    /** Indica que los campos ya se rellenaron; se conserva aunque se cierre el proceso. */
    var camposRellenados: Boolean
        get() = estadoGuardado[CLAVE_CAMPOS_RELLENADOS] ?: false
        set(valor) { estadoGuardado[CLAVE_CAMPOS_RELLENADOS] = valor }

    init {
        if (esEdicion) viewModelScope.launch {
            _estado.update { it.copy(original = repositorio.obtenerPorId(tipoId)) }
        }
    }

    fun guardar(entrada: ValidadorTipo.Entrada) {
        // En edición se espera a tener el tipo para saber si es la ITV.
        if (esEdicion && _estado.value.original == null) return
        val esItv = _estado.value.original?.esItv == true
        val resultado = ValidadorTipo.validar(entrada.copy(esItv = esItv))
        if (!resultado.esValido) {
            _estado.update { it.copy(errores = resultado.errores) }
            return
        }
        val tipo = (_estado.value.original ?: TipoMantenimiento(nombre = "", intervaloKm = null, intervaloMeses = null)).copy(
            nombre = entrada.nombre.trim(),
            // La ITV no usa intervalo por kilómetros.
            intervaloKm = if (esItv) null else entrada.intervaloKm.trim().toIntOrNull(),
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

    private companion object {
        const val CLAVE_CAMPOS_RELLENADOS = "editar_tipo_campos_rellenados"
    }
}
