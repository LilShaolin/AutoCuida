package com.autocuida.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.TimeZone

class FechasTest {

    private val zonaOriginal: TimeZone = TimeZone.getDefault()

    @After
    fun restaurar() {
        TimeZone.setDefault(zonaOriginal)
    }

    @Test
    fun relojDelSistema_usaLaZonaHorariaVigenteEnCadaLlamada() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
        assertEquals(ZoneId.of("Europe/Madrid"), Fechas.reloj.zone)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"))
        assertEquals(ZoneId.of("America/Mexico_City"), Fechas.reloj.zone)
    }

    @Test
    fun conversionEntreDiaEpochYMilisegundosUtc_esReversible() {
        assertEquals(20_000L, Fechas.utcMillisAEpochDay(Fechas.epochDayAUtcMillis(20_000L)))
        assertEquals(-1L, Fechas.utcMillisAEpochDay(-1L))
    }
}
