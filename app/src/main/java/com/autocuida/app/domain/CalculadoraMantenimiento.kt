package com.autocuida.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Estado de un mantenimiento. El orden de declaración refleja la gravedad creciente. */
enum class EstadoMantenimiento {
    /** No existe un mantenimiento previo que sirva de referencia. */
    SIN_REFERENCIA,

    /** Verde: al día. */
    AL_DIA,

    /** Ámbar: quedan menos de [CalculadoraMantenimiento.UMBRAL_KM] km o menos de [CalculadoraMantenimiento.UMBRAL_DIAS] días. */
    PROXIMO,

    /** Rojo: se ha superado algún límite aplicable. */
    VENCIDO
}

/** Criterio que determina el vencimiento cuando existen intervalos por kilómetros y por tiempo. */
enum class CriterioVencimiento { KILOMETROS, FECHA }

/** Último mantenimiento conocido de un tipo, usado como punto de partida del cálculo. */
data class ReferenciaMantenimiento(val fecha: LocalDate, val km: Int)

/** Límites del próximo mantenimiento. Al menos uno de los dos está definido. */
data class ProximoVencimiento(val kmLimite: Int?, val fechaLimite: LocalDate?) {
    init {
        require(kmLimite != null || fechaLimite != null) { "Se necesita al menos un límite" }
    }
}

/** Resultado de evaluar un vencimiento en un momento concreto. */
data class EvaluacionVencimiento(
    val estado: EstadoMantenimiento,
    val proximo: ProximoVencimiento? = null,
    /** Kilómetros que faltan; negativo si se han superado. `null` sin intervalo por km. */
    val kmRestantes: Int? = null,
    /** Días que faltan; negativo si se han superado. `null` sin intervalo temporal. */
    val diasRestantes: Long? = null,
    /** Criterio que llega antes (o que determina el estado). `null` sin referencia. */
    val criterio: CriterioVencimiento? = null
)

/**
 * Lógica de cálculo de vencimientos de AutoCuida. No depende de Android y se prueba con JUnit.
 *
 * Reglas:
 * - Por kilómetros: próximo = km del último mantenimiento + intervalo de km.
 * - Por fecha: próximo = fecha del último mantenimiento + intervalo de meses.
 * - Con ambos intervalos se considera el vencimiento que llegue antes.
 * - ROJO (VENCIDO) si se supera cualquier límite: km actuales > km límite o hoy > fecha límite.
 *   El día y el kilómetro exactos del límite todavía no se consideran superados.
 * - ÁMBAR (PROXIMO) si quedan menos de 1.000 km (0–999) o menos de 30 días (0–29).
 *   Con exactamente 1.000 km o 30 días restantes el estado es VERDE.
 * - VERDE (AL_DIA) en el resto de casos.
 * - Sin historial no se inventa referencia: el estado es SIN_REFERENCIA.
 */
object CalculadoraMantenimiento {

    const val UMBRAL_KM = 1_000
    const val UMBRAL_DIAS = 30L

    /** Calcula los límites del próximo mantenimiento, o `null` si no hay referencia o intervalos. */
    fun calcularProximo(
        referencia: ReferenciaMantenimiento?,
        intervaloKm: Int?,
        intervaloMeses: Int?
    ): ProximoVencimiento? {
        if (referencia == null) return null
        val kmLimite = intervaloKm?.takeIf { it > 0 }?.let { referencia.km + it }
        val fechaLimite = intervaloMeses?.takeIf { it > 0 }?.let { referencia.fecha.plusMonths(it.toLong()) }
        if (kmLimite == null && fechaLimite == null) return null
        return ProximoVencimiento(kmLimite, fechaLimite)
    }

    /** Calcula y evalúa en un solo paso a partir del último mantenimiento conocido. */
    fun calcular(
        referencia: ReferenciaMantenimiento?,
        intervaloKm: Int?,
        intervaloMeses: Int?,
        kmActuales: Int,
        hoy: LocalDate
    ): EvaluacionVencimiento {
        val proximo = calcularProximo(referencia, intervaloKm, intervaloMeses)
            ?: return EvaluacionVencimiento(EstadoMantenimiento.SIN_REFERENCIA)
        return evaluar(proximo, kmActuales, hoy, intervaloKm, intervaloMeses)
    }

    /**
     * Evalúa unos límites ya calculados (por ejemplo, los guardados en un recordatorio).
     *
     * Los intervalos solo se usan para desempatar el criterio cuando ambos límites
     * producen el mismo estado: se elige el que tiene menor fracción de intervalo restante.
     */
    fun evaluar(
        proximo: ProximoVencimiento,
        kmActuales: Int,
        hoy: LocalDate,
        intervaloKm: Int? = null,
        intervaloMeses: Int? = null
    ): EvaluacionVencimiento {
        val kmRestantes = proximo.kmLimite?.let { it - kmActuales }
        val diasRestantes = proximo.fechaLimite?.let { ChronoUnit.DAYS.between(hoy, it) }

        val estadoKm = kmRestantes?.let { estadoPorKm(it) }
        val estadoFecha = diasRestantes?.let { estadoPorDias(it) }

        val criterio = when {
            estadoKm == null -> CriterioVencimiento.FECHA
            estadoFecha == null -> CriterioVencimiento.KILOMETROS
            estadoKm > estadoFecha -> CriterioVencimiento.KILOMETROS
            estadoFecha > estadoKm -> CriterioVencimiento.FECHA
            else -> desempatar(kmRestantes!!, diasRestantes!!, proximo, intervaloKm, intervaloMeses)
        }
        val estado = listOfNotNull(estadoKm, estadoFecha).max()
        return EvaluacionVencimiento(estado, proximo, kmRestantes, diasRestantes, criterio)
    }

    /** Evalúa la ITV a partir de la fecha de la próxima inspección. */
    fun evaluarItv(fechaItv: LocalDate, hoy: LocalDate): EvaluacionVencimiento =
        evaluar(ProximoVencimiento(kmLimite = null, fechaLimite = fechaItv), kmActuales = 0, hoy = hoy)

    /** Estado global de un conjunto de evaluaciones: el más grave, ignorando las que no tienen referencia. */
    fun estadoGeneral(evaluaciones: Collection<EvaluacionVencimiento>): EstadoMantenimiento =
        evaluaciones.map { it.estado }.filter { it != EstadoMantenimiento.SIN_REFERENCIA }.maxOrNull()
            ?: EstadoMantenimiento.SIN_REFERENCIA

    /**
     * Margen restante expresado en umbrales de aviso, para comparar vencimientos por km y por fecha:
     * 1.000 km equivalen a 30 días. Con ambos límites se toma el menor. Es negativo si se ha superado
     * y `null` sin referencia.
     */
    fun margenRelativo(evaluacion: EvaluacionVencimiento): Double? = listOfNotNull(
        evaluacion.kmRestantes?.let { it.toDouble() / UMBRAL_KM },
        evaluacion.diasRestantes?.let { it.toDouble() / UMBRAL_DIAS }
    ).minOrNull()

    fun estadoPorKm(kmRestantes: Int): EstadoMantenimiento = when {
        kmRestantes < 0 -> EstadoMantenimiento.VENCIDO
        kmRestantes < UMBRAL_KM -> EstadoMantenimiento.PROXIMO
        else -> EstadoMantenimiento.AL_DIA
    }

    fun estadoPorDias(diasRestantes: Long): EstadoMantenimiento = when {
        diasRestantes < 0 -> EstadoMantenimiento.VENCIDO
        diasRestantes < UMBRAL_DIAS -> EstadoMantenimiento.PROXIMO
        else -> EstadoMantenimiento.AL_DIA
    }

    private fun desempatar(
        kmRestantes: Int,
        diasRestantes: Long,
        proximo: ProximoVencimiento,
        intervaloKm: Int?,
        intervaloMeses: Int?
    ): CriterioVencimiento {
        if (intervaloKm == null || intervaloKm <= 0 || intervaloMeses == null || intervaloMeses <= 0) {
            return CriterioVencimiento.KILOMETROS
        }
        val fechaLimite = proximo.fechaLimite!!
        val diasIntervalo = ChronoUnit.DAYS.between(fechaLimite.minusMonths(intervaloMeses.toLong()), fechaLimite)
        val fraccionKm = kmRestantes.toDouble() / intervaloKm
        val fraccionFecha = diasRestantes.toDouble() / diasIntervalo
        return if (fraccionFecha < fraccionKm) CriterioVencimiento.FECHA else CriterioVencimiento.KILOMETROS
    }
}
