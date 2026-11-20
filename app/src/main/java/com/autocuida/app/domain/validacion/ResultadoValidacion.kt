package com.autocuida.app.domain.validacion

/**
 * Errores de validación independientes de Android. La capa de presentación los traduce
 * a textos de `strings.xml`, de modo que los validadores pueden probarse con JUnit.
 */
enum class ErrorValidacion {
    OBLIGATORIO,
    LONGITUD_EXCEDIDA,
    MATRICULA_FORMATO,
    MATRICULA_DUPLICADA,
    ANIO_FUERA_DE_RANGO,
    NUMERO_NO_VALIDO,
    KM_NEGATIVOS,
    KM_EXCESIVOS,
    KM_INFERIORES_A_ANTERIOR,
    KM_SUPERIORES_A_POSTERIOR,
    KM_INFERIORES_A_HISTORIAL,
    KM_INFERIORES_A_ACTUALES,
    KM_SUPERIORES_A_ACTUALES,
    KM_RETROCESO,
    COSTE_NEGATIVO,
    COSTE_EXCESIVO,
    FECHA_FUTURA,
    FECHA_ITV_ANTERIOR,
    INTERVALO_REQUERIDO,
    INTERVALO_NO_POSITIVO,
    INTERVALO_EXCESIVO,
    NOMBRE_DUPLICADO
}

/** Resultado de validar un formulario: mapa campo → error. Vacío si es válido. */
class ResultadoValidacion<C : Enum<C>>(val errores: Map<C, ErrorValidacion>) {
    val esValido: Boolean get() = errores.isEmpty()
    fun error(campo: C): ErrorValidacion? = errores[campo]
}
