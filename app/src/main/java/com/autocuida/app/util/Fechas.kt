package com.autocuida.app.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Conversión y formato de fechas. Las fechas se guardan como día epoch. */
object Fechas {

    private const val MILIS_POR_DIA = 86_400_000L
    /** Comprobación periódica del cambio de día mientras una pantalla observa la fecha. */
    private const val MAX_ESPERA_MS = 60_000L
    private val FORMATO_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Reloj del sistema que consulta la zona horaria en cada llamada, de modo que un cambio de zona
     * (por ejemplo, al viajar) se aplica sin reiniciar la aplicación.
     */
    private object RelojSistema : Clock() {
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId): Clock = Clock.system(zone)
        override fun instant(): Instant = Instant.now()
    }

    /** Reloj sustituible en pruebas. */
    @Volatile
    var reloj: Clock = RelojSistema

    fun hoy(): LocalDate = LocalDate.now(reloj)

    fun hoyEpochDay(): Long = hoy().toEpochDay()

    /**
     * Emite la fecha de hoy y la vuelve a emitir al cambiar de día, para que los estados
     * («vence hoy», días restantes) se actualicen aunque la pantalla siga abierta.
     */
    fun flujoHoy(): Flow<LocalDate> = flow {
        while (true) {
            emit(hoy())
            val ahora = reloj.instant()
            val manana = hoy().plusDays(1).atStartOfDay(reloj.zone).toInstant()
            delay((manana.toEpochMilli() - ahora.toEpochMilli() + 500).coerceIn(1_000, MAX_ESPERA_MS))
        }
    }.distinctUntilChanged()

    fun formatear(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(FORMATO_CORTO)

    fun formatear(fecha: LocalDate): String = fecha.format(FORMATO_CORTO)

    /** MaterialDatePicker trabaja con milisegundos UTC a medianoche. */
    fun utcMillisAEpochDay(millis: Long): Long = Math.floorDiv(millis, MILIS_POR_DIA)

    fun epochDayAUtcMillis(epochDay: Long): Long = epochDay * MILIS_POR_DIA
}
