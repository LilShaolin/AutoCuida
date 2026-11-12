package com.autocuida.app.domain.validacion

/**
 * Validación del formulario de mantenimiento.
 *
 * Coherencia cronológica de kilómetros: un mantenimiento no puede tener menos kilómetros que
 * otro registrado en una fecha anterior, ni más que otro registrado en una fecha posterior.
 * Los registros del mismo día no se restringen entre sí.
 */
object ValidadorMantenimiento {

    enum class Campo { TIPO, FECHA, KM, COSTE, TALLER, NOTAS, FECHA_ITV }

    const val COSTE_MAXIMO = 1_000_000.0
    const val LONGITUD_TALLER = 60
    const val LONGITUD_NOTAS = 500

    data class Entrada(
        val tipoId: Long?,
        val fechaEpochDay: Long?,
        val km: String,
        val coste: String,
        val taller: String,
        val notas: String,
        /** Solo para ITV: fecha de la próxima inspección, si el usuario la indica. */
        val proximaItvEpochDay: Long? = null,
        val esItv: Boolean = false
    )

    /** Contexto obtenido de la base de datos para comprobar la coherencia. */
    data class Contexto(
        val hoyEpochDay: Long,
        val kmMaximoAnterior: Int?,
        val kmMinimoPosterior: Int?
    )

    /** Convierte el texto del coste aceptando coma o punto decimal. Vacío equivale a 0. */
    fun parsearCoste(texto: String): Double? {
        val limpio = texto.trim().replace(" ", "").replace("€", "")
        if (limpio.isEmpty()) return 0.0
        val normalizado = if (limpio.contains(',')) limpio.replace(".", "").replace(',', '.') else limpio
        return normalizado.toDoubleOrNull()
    }

    fun validar(entrada: Entrada, contexto: Contexto): ResultadoValidacion<Campo> {
        val errores = mutableMapOf<Campo, ErrorValidacion>()

        if (entrada.tipoId == null) errores[Campo.TIPO] = ErrorValidacion.OBLIGATORIO

        when {
            entrada.fechaEpochDay == null -> errores[Campo.FECHA] = ErrorValidacion.OBLIGATORIO
            entrada.fechaEpochDay > contexto.hoyEpochDay -> errores[Campo.FECHA] = ErrorValidacion.FECHA_FUTURA
        }

        val errorKm = ValidadorVehiculo.validarKm(entrada.km)
        if (errorKm != null) {
            errores[Campo.KM] = errorKm
        } else {
            val km = entrada.km.trim().toInt()
            when {
                contexto.kmMaximoAnterior != null && km < contexto.kmMaximoAnterior ->
                    errores[Campo.KM] = ErrorValidacion.KM_INFERIORES_A_ANTERIOR
                contexto.kmMinimoPosterior != null && km > contexto.kmMinimoPosterior ->
                    errores[Campo.KM] = ErrorValidacion.KM_SUPERIORES_A_POSTERIOR
            }
        }

        val coste = parsearCoste(entrada.coste)
        when {
            coste == null -> errores[Campo.COSTE] = ErrorValidacion.NUMERO_NO_VALIDO
            coste < 0 -> errores[Campo.COSTE] = ErrorValidacion.COSTE_NEGATIVO
            coste > COSTE_MAXIMO -> errores[Campo.COSTE] = ErrorValidacion.COSTE_EXCESIVO
        }

        if (entrada.taller.trim().length > LONGITUD_TALLER) errores[Campo.TALLER] = ErrorValidacion.LONGITUD_EXCEDIDA
        if (entrada.notas.trim().length > LONGITUD_NOTAS) errores[Campo.NOTAS] = ErrorValidacion.LONGITUD_EXCEDIDA

        if (entrada.esItv && entrada.proximaItvEpochDay != null && entrada.fechaEpochDay != null &&
            entrada.proximaItvEpochDay <= entrada.fechaEpochDay
        ) {
            errores[Campo.FECHA_ITV] = ErrorValidacion.FECHA_ITV_ANTERIOR
        }

        return ResultadoValidacion(errores)
    }
}
