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
        val kmMinimoPosterior: Int?,
        /** Kilómetros actuales del vehículo: un registro anterior a hoy no puede superarlos. */
        val kmActualesVehiculo: Int? = null
    )

    /** Número con puntos como separador de miles y sin decimales: "1.500", "12.000", "1.250.000". */
    private val PATRON_MILES = Regex("^[1-9]\\d{0,2}(\\.\\d{3})+$")

    /**
     * Convierte el texto del coste aceptando coma o punto decimal. Vacío equivale a 0.
     * Sin coma, un punto seguido de exactamente tres dígitos se interpreta como separador de miles
     * ("1.500" son 1500 €); en otro caso es decimal ("45.90" son 45,90 €).
     */
    fun parsearCoste(texto: String): Double? {
        val limpio = texto.trim().replace(" ", "").replace("€", "")
        if (limpio.isEmpty()) return 0.0
        val normalizado = when {
            limpio.contains(',') -> limpio.replace(".", "").replace(',', '.')
            PATRON_MILES.matches(limpio) -> limpio.replace(".", "")
            else -> limpio
        }
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
                // Solo un registro de hoy puede adelantar el cuentakilómetros.
                contexto.kmActualesVehiculo != null && km > contexto.kmActualesVehiculo &&
                    entrada.fechaEpochDay != null && entrada.fechaEpochDay < contexto.hoyEpochDay ->
                    errores[Campo.KM] = ErrorValidacion.KM_SUPERIORES_A_ACTUALES
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
