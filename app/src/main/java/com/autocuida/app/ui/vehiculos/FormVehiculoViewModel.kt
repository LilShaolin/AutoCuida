package com.autocuida.app.ui.vehiculos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocuida.app.ProgramadorAvisos
import com.autocuida.app.R
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.domain.validacion.ErrorValidacion
import com.autocuida.app.domain.validacion.ValidadorVehiculo
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.util.Fechas
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FormVehiculoViewModel(
    private val estadoGuardado: SavedStateHandle,
    private val repositorio: VehiculoRepository,
    private val avisos: ProgramadorAvisos
) : ViewModel() {

    val vehiculoId: Long = estadoGuardado[DetalleVehiculoFragment.ARG_VEHICULO_ID] ?: 0L
    val esEdicion: Boolean get() = vehiculoId != 0L

    data class UiState(
        val original: Vehiculo? = null,
        val fechaItv: Long? = null,
        /** Mayor kilometraje del historial (solo en edición): límite inferior de los km. */
        val kmMaximoHistorial: Int? = null,
        val errores: Map<ValidadorVehiculo.Campo, ErrorValidacion> = emptyMap(),
        val guardando: Boolean = false
    )

    // La fecha elegida se conserva aunque el sistema cierre el proceso con el formulario abierto.
    private val _estado = MutableStateFlow(UiState(fechaItv = estadoGuardado[CLAVE_FECHA_ITV]))
    val estado: StateFlow<UiState> = _estado.asStateFlow()

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    /**
     * Evita sobrescribir lo que escribe el usuario al recrear la vista (al girar o tras cerrarse el
     * proceso, cuando Android restaura los textos).
     */
    var camposRellenados: Boolean
        get() = estadoGuardado[CLAVE_CAMPOS_RELLENADOS] ?: false
        set(valor) { estadoGuardado[CLAVE_CAMPOS_RELLENADOS] = valor }

    init {
        if (esEdicion) {
            viewModelScope.launch {
                val vehiculo = repositorio.obtenerPorId(vehiculoId)
                val kmMaximo = repositorio.kmMaximoHistorial(vehiculoId)
                _estado.update {
                    it.copy(original = vehiculo, fechaItv = it.fechaItv ?: vehiculo?.fechaItv, kmMaximoHistorial = kmMaximo)
                }
                if (vehiculo == null) _eventos.send(EventoUi.Mensaje(R.string.vehiculo_no_encontrado))
            }
        }
    }

    fun seleccionarFechaItv(epochDay: Long) {
        estadoGuardado[CLAVE_FECHA_ITV] = epochDay
        _estado.update { it.copy(fechaItv = epochDay, errores = it.errores - ValidadorVehiculo.Campo.FECHA_ITV) }
    }

    fun guardar(entrada: ValidadorVehiculo.Entrada) {
        val actual = _estado.value
        // En edición se espera a tener el vehículo y su historial para validar los kilómetros.
        if (actual.guardando || (esEdicion && actual.original == null)) return
        val resultado = ValidadorVehiculo.validar(
            entrada.copy(fechaItvEpochDay = actual.fechaItv), Fechas.hoy().year, actual.kmMaximoHistorial,
            kmActualesOriginal = actual.original?.kmActuales
        )
        if (!resultado.esValido) {
            _estado.update { it.copy(errores = resultado.errores) }
            return
        }
        val vehiculo = Vehiculo(
            id = vehiculoId,
            marca = entrada.marca.trim(),
            modelo = entrada.modelo.trim(),
            matricula = ValidadorVehiculo.normalizarMatricula(entrada.matricula),
            anio = entrada.anio.trim().toInt(),
            combustible = entrada.combustible.trim(),
            kmActuales = entrada.km.trim().toInt(),
            fechaItv = requireNotNull(_estado.value.fechaItv)
        )
        _estado.update { it.copy(guardando = true, errores = emptyMap()) }
        viewModelScope.launch {
            try {
                when (val r = repositorio.guardar(vehiculo)) {
                    is VehiculoRepository.ResultadoGuardado.Guardado -> {
                        avisos.revisarAhora()
                        _eventos.send(EventoUi.Guardado(R.string.vehiculo_guardado, r.id))
                    }
                    VehiculoRepository.ResultadoGuardado.MatriculaDuplicada -> _estado.update {
                        it.copy(errores = mapOf(ValidadorVehiculo.Campo.MATRICULA to ErrorValidacion.MATRICULA_DUPLICADA))
                    }
                }
            } catch (e: Exception) {
                _eventos.send(EventoUi.Mensaje(R.string.error_generico))
            } finally {
                _estado.update { it.copy(guardando = false) }
            }
        }
    }

    private companion object {
        const val CLAVE_FECHA_ITV = "form_vehiculo_fecha_itv"
        const val CLAVE_CAMPOS_RELLENADOS = "form_vehiculo_campos_rellenados"
    }
}
