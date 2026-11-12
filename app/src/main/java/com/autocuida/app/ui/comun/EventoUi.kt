package com.autocuida.app.ui.comun

import androidx.annotation.StringRes

/** Eventos de una sola vez que los ViewModels envían a la vista (mensajes y navegación). */
sealed interface EventoUi {
    data class Mensaje(@StringRes val texto: Int, val args: List<Any> = emptyList()) : EventoUi
    data class Guardado(@StringRes val texto: Int, val id: Long = 0) : EventoUi
    data class Eliminado(@StringRes val texto: Int) : EventoUi
}
