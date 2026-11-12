package com.autocuida.app.ui.vehiculos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocuida.app.ProgramadorAvisos
import com.autocuida.app.R
import com.autocuida.app.data.entity.MantenimientoDetalle
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.data.repository.MantenimientoRepository
import com.autocuida.app.data.repository.RecordatorioRepository
import com.autocuida.app.data.repository.TipoMantenimientoRepository
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.domain.CalculadoraMantenimiento
import com.autocuida.app.domain.EstadoMantenimiento
import com.autocuida.app.domain.EvaluacionVencimiento
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.util.Fechas
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DetalleVehiculoViewModel(
    estadoGuardado: SavedStateHandle,
    private val vehiculoRepository: VehiculoRepository,
    recordatorioRepository: RecordatorioRepository,
    tipoRepository: TipoMantenimientoRepository,
    mantenimientoRepository: MantenimientoRepository,
    private val avisos: ProgramadorAvisos
) : ViewModel() {

    val vehiculoId: Long = requireNotNull(estadoGuardado[DetalleVehiculoFragment.ARG_VEHICULO_ID])

    data class UiState(
        val cargando: Boolean = true,
        val vehiculo: Vehiculo? = null,
        val estadoGeneral: EstadoMantenimiento = EstadoMantenimiento.SIN_REFERENCIA,
        val itv: EvaluacionVencimiento? = null,
        val vencimientos: List<VencimientoEvaluado> = emptyList(),
        val tiposFiltro: List<TipoMantenimiento> = emptyList(),
        val filtroTipoId: Long? = null,
        val historial: List<MantenimientoDetalle> = emptyList(),
        val historialTotal: Int = 0
    )

    private val filtroTipo = MutableStateFlow<Long?>(null)

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    val estado: StateFlow<UiState> = combine(
        vehiculoRepository.observarPorId(vehiculoId),
        recordatorioRepository.observarPorVehiculo(vehiculoId),
        tipoRepository.observarTodos(),
        mantenimientoRepository.observarPorVehiculo(vehiculoId),
        filtroTipo
    ) { vehiculo, recordatorios, tipos, historial, filtro ->
        if (vehiculo == null) return@combine UiState(cargando = false)
        val hoy = Fechas.hoy()
        val itv = CalculadoraMantenimiento.evaluarItv(LocalDate.ofEpochDay(vehiculo.fechaItv), hoy)
        val porTipo = recordatorios.associateBy { it.recordatorio.tipoId }

        // Un elemento por tipo (excepto ITV, que tiene su propia tarjeta), incluidos los que no tienen historial.
        val vencimientos = tipos.filterNot { it.esItv }.map { tipo ->
            porTipo[tipo.id]?.aVencimiento(hoy) ?: VencimientoEvaluado(
                tipoId = tipo.id,
                tipoNombre = tipo.nombre,
                esItv = false,
                evaluacion = EvaluacionVencimiento(EstadoMantenimiento.SIN_REFERENCIA)
            )
        }.sortedWith(ResumenVehiculo.ORDEN_URGENCIA)

        val tiposUsados = historial.map { it.mantenimiento.tipoId }.toSet()
        val filtroValido = filtro?.takeIf { it in tiposUsados }
        UiState(
            cargando = false,
            vehiculo = vehiculo,
            estadoGeneral = CalculadoraMantenimiento.estadoGeneral(vencimientos.map { it.evaluacion } + itv),
            itv = itv,
            vencimientos = vencimientos,
            tiposFiltro = tipos.filter { it.id in tiposUsados },
            filtroTipoId = filtroValido,
            historial = if (filtroValido == null) historial else historial.filter { it.mantenimiento.tipoId == filtroValido },
            historialTotal = historial.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun filtrar(tipoId: Long?) {
        filtroTipo.value = tipoId
    }

    /** Actualiza la fecha de ITV tras una nueva inspección. */
    fun actualizarFechaItv(epochDay: Long) {
        val vehiculo = estado.value.vehiculo ?: return
        viewModelScope.launch {
            try {
                vehiculoRepository.guardar(vehiculo.copy(fechaItv = epochDay))
                avisos.revisarAhora()
                _eventos.send(EventoUi.Mensaje(R.string.fecha_itv_actualizada))
            } catch (e: Exception) {
                _eventos.send(EventoUi.Mensaje(R.string.error_generico))
            }
        }
    }

    fun eliminarVehiculo() {
        val vehiculo = estado.value.vehiculo ?: return
        viewModelScope.launch {
            try {
                vehiculoRepository.eliminar(vehiculo)
                _eventos.send(EventoUi.Eliminado(R.string.vehiculo_eliminado))
            } catch (e: Exception) {
                _eventos.send(EventoUi.Mensaje(R.string.error_generico))
            }
        }
    }
}
