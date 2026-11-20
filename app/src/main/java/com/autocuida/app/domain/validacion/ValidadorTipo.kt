package com.autocuida.app.domain.validacion

import java.text.Normalizer
import java.util.Locale

/** Validación de un tipo de mantenimiento y de sus intervalos. */
object ValidadorTipo {

    enum class Campo { NOMBRE, INTERVALO_KM, INTERVALO_MESES }

    const val LONGITUD_NOMBRE = 30
    const val INTERVALO_KM_MAXIMO = 500_000
    const val INTERVALO_MESES_MAXIMO = 240

    private val LOCALE_ES: Locale = Locale.forLanguageTag("es-ES")
    private val MARCAS_DIACRITICAS = Regex("\\p{M}+")
    private val ESPACIOS = Regex("\\s+")

    /**
     * @property esItv la ITV solo usa el intervalo en meses (propone la siguiente inspección):
     * el de kilómetros se ignora y los meses son obligatorios.
     */
    data class Entrada(
        val nombre: String,
        val intervaloKm: String,
        val intervaloMeses: String,
        val esItv: Boolean = false
    )

    fun validar(entrada: Entrada): ResultadoValidacion<Campo> {
        val errores = mutableMapOf<Campo, ErrorValidacion>()

        when {
            entrada.nombre.isBlank() -> errores[Campo.NOMBRE] = ErrorValidacion.OBLIGATORIO
            entrada.nombre.trim().length > LONGITUD_NOMBRE -> errores[Campo.NOMBRE] = ErrorValidacion.LONGITUD_EXCEDIDA
        }

        if (entrada.esItv) {
            if (entrada.intervaloMeses.isBlank()) {
                errores[Campo.INTERVALO_MESES] = ErrorValidacion.OBLIGATORIO
            } else {
                validarIntervalo(entrada.intervaloMeses, INTERVALO_MESES_MAXIMO)?.let { errores[Campo.INTERVALO_MESES] = it }
            }
            return ResultadoValidacion(errores)
        }

        validarIntervalo(entrada.intervaloKm, INTERVALO_KM_MAXIMO)?.let { errores[Campo.INTERVALO_KM] = it }
        validarIntervalo(entrada.intervaloMeses, INTERVALO_MESES_MAXIMO)?.let { errores[Campo.INTERVALO_MESES] = it }

        if (entrada.intervaloKm.isBlank() && entrada.intervaloMeses.isBlank()) {
            errores[Campo.INTERVALO_KM] = ErrorValidacion.INTERVALO_REQUERIDO
            errores[Campo.INTERVALO_MESES] = ErrorValidacion.INTERVALO_REQUERIDO
        }
        return ResultadoValidacion(errores)
    }

    /**
     * Forma comparable de un nombre para detectar duplicados: sin espacios sobrantes, en minúsculas
     * y sin tildes ni diéresis ("  NEUMÁTICOS " y "neumaticos" son el mismo nombre).
     */
    fun normalizarNombre(nombre: String): String {
        val minusculas = nombre.trim().replace(ESPACIOS, " ").lowercase(LOCALE_ES)
        return Normalizer.normalize(minusculas, Normalizer.Form.NFD).replace(MARCAS_DIACRITICAS, "")
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
