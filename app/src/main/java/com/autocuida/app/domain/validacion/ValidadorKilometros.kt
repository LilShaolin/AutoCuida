package com.autocuida.app.domain.validacion

/**
 * Validación de la actualización del cuentakilómetros.
 *
 * - No se admiten valores negativos ni superiores al máximo razonable.
 * - Un valor inferior al actual se considera un retroceso y solo se admite como corrección
 *   explícita de un error de lectura, y nunca por debajo del mayor kilometraje ya registrado
 *   en el historial de mantenimientos.
 */
object ValidadorKilometros {

    sealed interface Resultado {
        data class Valido(val km: Int, val esCorreccion: Boolean) : Resultado
        data class Invalido(val error: ErrorValidacion) : Resultado
    }

    fun validar(
        texto: String,
        kmActuales: Int,
        kmMaximoHistorial: Int?,
        permitirCorreccion: Boolean
    ): Resultado {
        ValidadorVehiculo.validarKm(texto)?.let { return Resultado.Invalido(it) }
        val km = texto.trim().toInt()
        if (km >= kmActuales) return Resultado.Valido(km, esCorreccion = false)
        if (kmMaximoHistorial != null && km < kmMaximoHistorial) {
            return Resultado.Invalido(ErrorValidacion.KM_INFERIORES_A_HISTORIAL)
        }
        return if (permitirCorreccion) Resultado.Valido(km, esCorreccion = true)
        else Resultado.Invalido(ErrorValidacion.KM_RETROCESO)
    }
}
