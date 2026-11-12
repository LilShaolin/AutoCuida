package com.autocuida.app.util

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Conversión y formato de fechas. Las fechas se guardan como día epoch. */
object Fechas {

    private const val MILIS_POR_DIA = 86_400_000L
    private val FORMATO_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Reloj sustituible en pruebas. */
    @Volatile
    var reloj: Clock = Clock.systemDefaultZone()

    fun hoy(): LocalDate = LocalDate.now(reloj)

    fun hoyEpochDay(): Long = hoy().toEpochDay()

    fun formatear(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(FORMATO_CORTO)

    fun formatear(fecha: LocalDate): String = fecha.format(FORMATO_CORTO)

    /** MaterialDatePicker trabaja con milisegundos UTC a medianoche. */
    fun utcMillisAEpochDay(millis: Long): Long = Math.floorDiv(millis, MILIS_POR_DIA)

    fun epochDayAUtcMillis(epochDay: Long): Long = epochDay * MILIS_POR_DIA
}
