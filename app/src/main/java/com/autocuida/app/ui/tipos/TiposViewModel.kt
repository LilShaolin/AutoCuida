package com.autocuida.app.ui.tipos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autocuida.app.R
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.repository.TipoMantenimientoRepository
import com.autocuida.app.data.repository.TipoMantenimientoRepository.ResultadoEliminacion
import com.autocuida.app.ui.comun.EventoUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TiposViewModel(private val repositorio: TipoMantenimientoRepository) : ViewModel() {

    data class TipoConUso(val tipo: TipoMantenimiento, val registros: Int)

    val tipos: StateFlow<List<TipoConUso>> = combine(
        repositorio.observarTodos(),
        repositorio.observarUso()
    ) { tipos, uso ->
        val usoPorTipo = uso.associate { it.id to it.cantidad }
        tipos.map { TipoConUso(it, usoPorTipo[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    fun eliminar(tipo: TipoMantenimiento) {
        viewModelScope.launch {
            val evento = when (val r = repositorio.eliminar(tipo)) {
                ResultadoEliminacion.Eliminado -> EventoUi.Eliminado(R.string.tipo_eliminado)
                ResultadoEliminacion.Protegido -> EventoUi.Mensaje(R.string.tipo_protegido)
                is ResultadoEliminacion.EnUso -> EventoUi.Mensaje(R.string.tipo_en_uso, listOf(r.mantenimientos))
            }
            _eventos.send(evento)
        }
    }
}
