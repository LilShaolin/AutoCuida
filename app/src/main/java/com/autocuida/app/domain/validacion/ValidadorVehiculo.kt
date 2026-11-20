package com.autocuida.app.domain.validacion

/** Validación del formulario de vehículo. */
object ValidadorVehiculo {

    enum class Campo { MARCA, MODELO, MATRICULA, ANIO, COMBUSTIBLE, KM, FECHA_ITV }

    const val LONGITUD_MAXIMA_TEXTO = 40
    const val ANIO_MINIMO = 1900
    const val KM_MAXIMOS = 2_000_000

    /** Matrícula normalizada: 4 a 10 caracteres alfanuméricos sin espacios ni guiones. */
    private val PATRON_MATRICULA = Regex("^[A-Z0-9]{4,10}$")

    data class Entrada(
        val marca: String,
        val modelo: String,
        val matricula: String,
        val anio: String,
        val combustible: String,
        val km: String,
        val fechaItvEpochDay: Long?
    )

    /** Elimina espacios y guiones y convierte a mayúsculas: "1234 abc" → "1234ABC". */
    fun normalizarMatricula(matricula: String): String =
        matricula.uppercase().filter { it.isLetterOrDigit() }

    /**
     * @param kmMaximoHistorial mayor kilometraje del historial del vehículo (solo en edición). Los km
     * no pueden quedar por debajo, con el mismo criterio que la actualización de kilómetros.
     * @param kmActualesOriginal kilómetros guardados del vehículo (solo en edición). Bajar de ellos es
     * una corrección, que solo se admite desde «Actualizar km».
     */
    fun validar(
        entrada: Entrada,
        anioActual: Int,
        kmMaximoHistorial: Int? = null,
        kmActualesOriginal: Int? = null
    ): ResultadoValidacion<Campo> {
        val errores = mutableMapOf<Campo, ErrorValidacion>()

        validarTexto(entrada.marca)?.let { errores[Campo.MARCA] = it }
        validarTexto(entrada.modelo)?.let { errores[Campo.MODELO] = it }

        val matricula = normalizarMatricula(entrada.matricula)
        when {
            matricula.isEmpty() -> errores[Campo.MATRICULA] = ErrorValidacion.OBLIGATORIO
            !PATRON_MATRICULA.matches(matricula) -> errores[Campo.MATRICULA] = ErrorValidacion.MATRICULA_FORMATO
        }

        val anio = entrada.anio.trim()
        when {
            anio.isEmpty() -> errores[Campo.ANIO] = ErrorValidacion.OBLIGATORIO
            anio.toIntOrNull() == null -> errores[Campo.ANIO] = ErrorValidacion.NUMERO_NO_VALIDO
            anio.toInt() !in ANIO_MINIMO..(anioActual + 1) -> errores[Campo.ANIO] = ErrorValidacion.ANIO_FUERA_DE_RANGO
        }

        if (entrada.combustible.isBlank()) errores[Campo.COMBUSTIBLE] = ErrorValidacion.OBLIGATORIO

        val errorKm = validarKm(entrada.km)
        if (errorKm != null) {
            errores[Campo.KM] = errorKm
        } else {
            val km = entrada.km.trim().toInt()
            when {
                // Bajar del máximo del historial nunca es válido.
                kmMaximoHistorial != null && km < kmMaximoHistorial ->
                    errores[Campo.KM] = ErrorValidacion.KM_INFERIORES_A_HISTORIAL
                kmActualesOriginal != null && km < kmActualesOriginal ->
                    errores[Campo.KM] = ErrorValidacion.KM_INFERIORES_A_ACTUALES
            }
        }

        if (entrada.fechaItvEpochDay == null) errores[Campo.FECHA_ITV] = ErrorValidacion.OBLIGATORIO

        return ResultadoValidacion(errores)
    }

    fun validarKm(texto: String): ErrorValidacion? {
        val km = texto.trim()
        return when {
            km.isEmpty() -> ErrorValidacion.OBLIGATORIO
            km.toLongOrNull() == null -> ErrorValidacion.NUMERO_NO_VALIDO
            km.toLong() < 0 -> ErrorValidacion.KM_NEGATIVOS
            km.toLong() > KM_MAXIMOS -> ErrorValidacion.KM_EXCESIVOS
            else -> null
        }
    }

    private fun validarTexto(valor: String): ErrorValidacion? = when {
        valor.isBlank() -> ErrorValidacion.OBLIGATORIO
        valor.trim().length > LONGITUD_MAXIMA_TEXTO -> ErrorValidacion.LONGITUD_EXCEDIDA
        else -> null
    }
}
