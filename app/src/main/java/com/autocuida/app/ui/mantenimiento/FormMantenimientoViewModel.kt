package com.autocuida.app.ui.mantenimiento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocuida.app.ProgramadorAvisos
import com.autocuida.app.R
import com.autocuida.app.data.entity.Mantenimiento
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.data.repository.MantenimientoRepository
import com.autocuida.app.data.repository.TipoMantenimientoRepository
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.domain.validacion.ErrorValidacion
import com.autocuida.app.domain.validacion.ValidadorMantenimiento
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.util.Fechas
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class FormMantenimientoViewModel(
    estadoGuardado: SavedStateHandle,
    private val mantenimientoRepository: MantenimientoRepository,
    private val vehiculoRepository: VehiculoRepository,
    private val tipoRepository: TipoMantenimientoRepository,
    private val avisos: ProgramadorAvisos
) : ViewModel() {

    private val vehiculoId: Long = requireNotNull(estadoGuardado[FormMantenimientoFragment.ARG_VEHICULO_ID])
    private val mantenimientoId: Long = estadoGuardado[FormMantenimientoFragment.ARG_MANTENIMIENTO_ID] ?: 0L
    private val tipoInicial: Long = estadoGuardado[FormMantenimientoFragment.ARG_TIPO_ID] ?: 0L
    val esEdicion: Boolean get() = mantenimientoId != 0L

    data class UiState(
        val cargado: Boolean = false,
        val vehiculo: Vehiculo? = null,
        val tipos: List<TipoMantenimiento> = emptyList(),
        val original: Mantenimiento? = null,
        val tipoId: Long? = null,
        val fecha: Long? = null,
        val proximaItv: Long? = null,
        val errores: Map<ValidadorMantenimiento.Campo, ErrorValidacion> = emptyMap(),
        /** Kilómetros de referencia del error de coherencia, para mostrarlos en el mensaje. */
        val kmReferenciaError: Int? = null,
        val guardando: Boolean = false
    ) {
        val tipoSeleccionado: TipoMantenimiento? get() = tipos.firstOrNull { it.id == tipoId }
        val esItv: Boolean get() = tipoSeleccionado?.esItv == true
    }

    private val _estado = MutableStateFlow(UiState())
    val estado: StateFlow<UiState> = _estado.asStateFlow()

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    var camposRellenados = false

    init {
        viewModelScope.launch {
            val vehiculo = vehiculoRepository.obtenerPorId(vehiculoId)
            val tipos = tipoRepository.obtenerTodos()
            val original = if (esEdicion) mantenimientoRepository.obtenerPorId(mantenimientoId) else null
            val tipoId = original?.tipoId ?: tipoInicial.takeIf { id -> tipos.any { it.id == id } }
            _estado.update {
                it.copy(
                    cargado = true,
                    vehiculo = vehiculo,
                    tipos = tipos,
                    original = original,
                    tipoId = tipoId,
                    fecha = original?.fecha ?: Fechas.hoyEpochDay()
                )
            }
            sugerirProximaItv()
        }
    }

    fun seleccionarTipo(id: Long) {
        _estado.update { it.copy(tipoId = id, errores = it.errores - ValidadorMantenimiento.Campo.TIPO) }
        sugerirProximaItv()
    }

    fun seleccionarFecha(epochDay: Long) {
        _estado.update { it.copy(fecha = epochDay, errores = it.errores - ValidadorMantenimiento.Campo.FECHA) }
        sugerirProximaItv(forzar = true)
    }

    fun seleccionarProximaItv(epochDay: Long?) = _estado.update {
        it.copy(proximaItv = epochDay, errores = it.errores - ValidadorMantenimiento.Campo.FECHA_ITV)
    }

    /**
     * Para una ITV nueva propone la siguiente inspección sumando el intervalo configurado.
     * Es solo una sugerencia editable: la periodicidad real depende de cada vehículo.
     */
    private fun sugerirProximaItv(forzar: Boolean = false) {
        val e = _estado.value
        if (!e.esItv || esEdicion) {
            if (!e.esItv) _estado.update { it.copy(proximaItv = null) }
            return
        }
        if (e.proximaItv != null && !forzar) return
        val meses = e.tipoSeleccionado?.intervaloMeses ?: return
        val fecha = e.fecha ?: return
        _estado.update { it.copy(proximaItv = LocalDate.ofEpochDay(fecha).plusMonths(meses.toLong()).toEpochDay()) }
    }

    fun guardar(km: String, coste: String, taller: String, notas: String) {
        val e = _estado.value
        if (e.guardando || !e.cargado) return
        viewModelScope.launch {
            val entrada = ValidadorMantenimiento.Entrada(
                tipoId = e.tipoId, fechaEpochDay = e.fecha, km = km, coste = coste,
                taller = taller, notas = notas, proximaItvEpochDay = e.proximaItv, esItv = e.esItv
            )
            val limites = e.fecha?.let { mantenimientoRepository.limitesKm(vehiculoId, it, mantenimientoId) }
            val contexto = ValidadorMantenimiento.Contexto(
                hoyEpochDay = Fechas.hoyEpochDay(),
                kmMaximoAnterior = limites?.maximoAnterior,
                kmMinimoPosterior = limites?.minimoPosterior
            )
            val resultado = ValidadorMantenimiento.validar(entrada, contexto)
            if (!resultado.esValido) {
                val referencia = when (resultado.error(ValidadorMantenimiento.Campo.KM)) {
                    ErrorValidacion.KM_INFERIORES_A_ANTERIOR -> limites?.maximoAnterior
                    ErrorValidacion.KM_SUPERIORES_A_POSTERIOR -> limites?.minimoPosterior
                    else -> null
                }
                _estado.update { it.copy(errores = resultado.errores, kmReferenciaError = referencia) }
                return@launch
            }
            val mantenimiento = Mantenimiento(
                id = mantenimientoId,
                vehiculoId = vehiculoId,
                tipoId = requireNotNull(e.tipoId),
                fecha = requireNotNull(e.fecha),
                km = km.trim().toInt(),
                coste = requireNotNull(ValidadorMantenimiento.parsearCoste(coste)),
                taller = taller.trim().ifEmpty { null },
                notas = notas.trim().ifEmpty { null }
            )
            _estado.update { it.copy(guardando = true, errores = emptyMap()) }
            try {
                mantenimientoRepository.guardar(mantenimiento, nuevaFechaItv = if (e.esItv) e.proximaItv else null)
                avisos.revisarAhora()
                _eventos.send(EventoUi.Guardado(R.string.mantenimiento_guardado))
            } catch (ex: Exception) {
                _eventos.send(EventoUi.Mensaje(R.string.error_generico))
            } finally {
                _estado.update { it.copy(guardando = false) }
            }
        }
    }

    fun eliminar() {
        val original = _estado.value.original ?: return
        viewModelScope.launch {
            try {
                mantenimientoRepository.eliminar(original)
                avisos.revisarAhora()
                _eventos.send(EventoUi.Eliminado(R.string.mantenimiento_eliminado))
            } catch (ex: Exception) {
                _eventos.send(EventoUi.Mensaje(R.string.error_generico))
            }
        }
    }
}
