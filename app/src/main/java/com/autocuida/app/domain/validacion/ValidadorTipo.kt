package com.autocuida.app.domain.validacion

/** Validación de un tipo de mantenimiento y de sus intervalos. */
object ValidadorTipo {

    enum class Campo { NOMBRE, INTERVALO_KM, INTERVALO_MESES }

    const val LONGITUD_NOMBRE = 30
    const val INTERVALO_KM_MAXIMO = 500_000
    const val INTERVALO_MESES_MAXIMO = 240

    data class Entrada(val nombre: String, val intervaloKm: String, val intervaloMeses: String)

    fun validar(entrada: Entrada): ResultadoValidacion<Campo> {
        val errores = mutableMapOf<Campo, ErrorValidacion>()

        when {
            entrada.nombre.isBlank() -> errores[Campo.NOMBRE] = ErrorValidacion.OBLIGATORIO
            entrada.nombre.trim().length > LONGITUD_NOMBRE -> errores[Campo.NOMBRE] = ErrorValidacion.LONGITUD_EXCEDIDA
        }

        validarIntervalo(entrada.intervaloKm, INTERVALO_KM_MAXIMO)?.let { errores[Campo.INTERVALO_KM] = it }
        validarIntervalo(entrada.intervaloMeses, INTERVALO_MESES_MAXIMO)?.let { errores[Campo.INTERVALO_MESES] = it }

        if (entrada.intervaloKm.isBlank() && entrada.intervaloMeses.isBlank()) {
            errores[Campo.INTERVALO_KM] = ErrorValidacion.INTERVALO_REQUERIDO
            errores[Campo.INTERVALO_MESES] = ErrorValidacion.INTERVALO_REQUERIDO
        }
        return ResultadoValidacion(errores)
    }

    private fun validarIntervalo(texto: String, maximo: Int): ErrorValidacion? {
        val valor = texto.trim()
        if (valor.isEmpty()) return null
        val numero = valor.toLongOrNull() ?: return ErrorValidacion.NUMERO_NO_VALIDO
        return when {
            numero <= 0 -> ErrorValidacion.INTERVALO_NO_POSITIVO
            numero > maximo -> ErrorValidacion.INTERVALO_EXCESIVO
            else -> null
        }
    }
}
