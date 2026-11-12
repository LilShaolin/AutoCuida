package com.autocuida.app.domain

/**
 * Decide si un recordatorio debe notificarse, evitando avisos repetidos.
 *
 * Cada recordatorio guarda el nivel ya avisado (0 = ninguno, 1 = próximo, 2 = vencido).
 * Solo se notifica cuando el nivel actual supera al ya avisado; si el estado mejora
 * (por ejemplo, tras corregir los kilómetros), el nivel se rebaja para poder avisar de nuevo
 * si vuelve a empeorar.
 */
object EvaluadorAvisos {

    const val NIVEL_NINGUNO = 0
    const val NIVEL_PROXIMO = 1
    const val NIVEL_VENCIDO = 2

    data class Decision(val notificar: Boolean, val nuevoNivel: Int)

    fun nivelDe(estado: EstadoMantenimiento): Int = when (estado) {
        EstadoMantenimiento.VENCIDO -> NIVEL_VENCIDO
        EstadoMantenimiento.PROXIMO -> NIVEL_PROXIMO
        EstadoMantenimiento.AL_DIA, EstadoMantenimiento.SIN_REFERENCIA -> NIVEL_NINGUNO
    }

    fun decidir(estado: EstadoMantenimiento, nivelAvisado: Int): Decision {
        val nivelActual = nivelDe(estado)
        return when {
            nivelActual > nivelAvisado -> Decision(notificar = true, nuevoNivel = nivelActual)
            nivelActual < nivelAvisado -> Decision(notificar = false, nuevoNivel = nivelActual)
            else -> Decision(notificar = false, nuevoNivel = nivelAvisado)
        }
    }
}
