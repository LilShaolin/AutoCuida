package com.autocuida.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CalculadoraMantenimientoTest {

    private val hoy = LocalDate.of(2026, 6, 15)
    private val referencia = ReferenciaMantenimiento(fecha = LocalDate.of(2026, 1, 15), km = 50_000)

    // ---------- Cálculo de límites ----------

    @Test
    fun calculoPorKilometros_sumaElIntervaloAlUltimoKilometraje() {
        val proximo = CalculadoraMantenimiento.calcularProximo(referencia, intervaloKm = 15_000, intervaloMeses = null)!!
        assertEquals(65_000, proximo.kmLimite)
        assertNull(proximo.fechaLimite)
    }

    @Test
    fun calculoPorFecha_sumaLosMesesALaUltimaFecha() {
        val proximo = CalculadoraMantenimiento.calcularProximo(referencia, intervaloKm = null, intervaloMeses = 12)!!
        assertEquals(LocalDate.of(2027, 1, 15), proximo.fechaLimite)
        assertNull(proximo.kmLimite)
    }

    @Test
    fun calculoPorAmbosIntervalos_devuelveLosDosLimites() {
        val proximo = CalculadoraMantenimiento.calcularProximo(referencia, 15_000, 12)!!
        assertEquals(65_000, proximo.kmLimite)
        assertEquals(LocalDate.of(2027, 1, 15), proximo.fechaLimite)
    }

    @Test
    fun calculoPorFecha_finDeMesSeAjustaAlUltimoDiaValido() {
        val ref = ReferenciaMantenimiento(LocalDate.of(2026, 1, 31), 0)
        val proximo = CalculadoraMantenimiento.calcularProximo(ref, null, 1)!!
        assertEquals(LocalDate.of(2026, 2, 28), proximo.fechaLimite)
    }

    @Test
    fun sinIntervalos_noHayProximoVencimiento() {
        assertNull(CalculadoraMantenimiento.calcularProximo(referencia, null, null))
    }

    // ---------- Ausencia de historial ----------

    @Test
    fun sinReferencia_elEstadoEsSinReferenciaYNoSeInventanLimites() {
        val r = CalculadoraMantenimiento.calcular(null, 15_000, 12, kmActuales = 80_000, hoy = hoy)
        assertEquals(EstadoMantenimiento.SIN_REFERENCIA, r.estado)
        assertNull(r.proximo)
        assertNull(r.kmRestantes)
        assertNull(r.diasRestantes)
        assertNull(r.criterio)
    }

    // ---------- Estados ----------

    @Test
    fun estadoVerde_cuandoQuedaMargenEnAmbosCriterios() {
        // Límite: 65.000 km y 15/01/2027. Actual: 55.000 km → quedan 10.000 km y 214 días.
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, 12, 55_000, hoy)
        assertEquals(EstadoMantenimiento.AL_DIA, r.estado)
        assertEquals(10_000, r.kmRestantes)
        assertEquals(214L, r.diasRestantes)
    }

    @Test
    fun estadoAmbar_porKilometros() {
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, 12, 64_500, hoy)
        assertEquals(EstadoMantenimiento.PROXIMO, r.estado)
        assertEquals(CriterioVencimiento.KILOMETROS, r.criterio)
    }

    @Test
    fun estadoAmbar_porFecha() {
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, 12, 51_000, LocalDate.of(2027, 1, 1))
        assertEquals(EstadoMantenimiento.PROXIMO, r.estado)
        assertEquals(CriterioVencimiento.FECHA, r.criterio)
        assertEquals(14L, r.diasRestantes)
    }

    @Test
    fun estadoRojo_vencidoPorKilometros() {
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, 12, 65_350, hoy)
        assertEquals(EstadoMantenimiento.VENCIDO, r.estado)
        assertEquals(-350, r.kmRestantes)
        assertEquals(CriterioVencimiento.KILOMETROS, r.criterio)
    }

    @Test
    fun estadoRojo_vencidoPorFecha() {
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, 12, 52_000, LocalDate.of(2027, 2, 1))
        assertEquals(EstadoMantenimiento.VENCIDO, r.estado)
        assertEquals(-17L, r.diasRestantes)
        assertEquals(CriterioVencimiento.FECHA, r.criterio)
    }

    @Test
    fun estadoRojo_tienePrioridadAunqueElOtroCriterioEsteAlDia() {
        // Fecha holgada pero kilómetros superados.
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, 12, 70_000, LocalDate.of(2026, 2, 1))
        assertEquals(EstadoMantenimiento.VENCIDO, r.estado)
        assertEquals(CriterioVencimiento.KILOMETROS, r.criterio)
    }

    @Test
    fun ambosIntervalos_seConsideraElVencimientoQueLlegaAntes() {
        // Por km quedan 4.000 (verde) pero por fecha quedan 10 días (ámbar): manda la fecha.
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, 12, 61_000, LocalDate.of(2027, 1, 5))
        assertEquals(EstadoMantenimiento.PROXIMO, r.estado)
        assertEquals(CriterioVencimiento.FECHA, r.criterio)
    }

    @Test
    fun ambosIntervalosEnMismoEstado_elCriterioEsElDeMenorFraccionRestante() {
        // Quedan 7.500 km de 15.000 (50 %) y ~92 días de 365 (25 %): llega antes la fecha.
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, 12, 57_500, LocalDate.of(2026, 10, 15))
        assertEquals(EstadoMantenimiento.AL_DIA, r.estado)
        assertEquals(CriterioVencimiento.FECHA, r.criterio)
    }

    // ---------- Límites exactos ----------

    @Test
    fun limiteExacto_1000KmRestantesEsVerde() {
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, null, 64_000, hoy)
        assertEquals(1_000, r.kmRestantes)
        assertEquals(EstadoMantenimiento.AL_DIA, r.estado)
    }

    @Test
    fun limiteExacto_999KmRestantesEsAmbar() {
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, null, 64_001, hoy)
        assertEquals(EstadoMantenimiento.PROXIMO, r.estado)
    }

    @Test
    fun limiteExacto_0KmRestantesEsAmbarYUnoMasEsRojo() {
        assertEquals(EstadoMantenimiento.PROXIMO, CalculadoraMantenimiento.calcular(referencia, 15_000, null, 65_000, hoy).estado)
        assertEquals(EstadoMantenimiento.VENCIDO, CalculadoraMantenimiento.calcular(referencia, 15_000, null, 65_001, hoy).estado)
    }

    @Test
    fun limiteExacto_30DiasRestantesEsVerde() {
        val r = CalculadoraMantenimiento.calcular(referencia, null, 12, 0, LocalDate.of(2026, 12, 16))
        assertEquals(30L, r.diasRestantes)
        assertEquals(EstadoMantenimiento.AL_DIA, r.estado)
    }

    @Test
    fun limiteExacto_29DiasRestantesEsAmbar() {
        val r = CalculadoraMantenimiento.calcular(referencia, null, 12, 0, LocalDate.of(2026, 12, 17))
        assertEquals(29L, r.diasRestantes)
        assertEquals(EstadoMantenimiento.PROXIMO, r.estado)
    }

    @Test
    fun limiteExacto_elDiaDelVencimientoEsAmbarYElSiguienteRojo() {
        val diaLimite = CalculadoraMantenimiento.calcular(referencia, null, 12, 0, LocalDate.of(2027, 1, 15))
        assertEquals(0L, diaLimite.diasRestantes)
        assertEquals(EstadoMantenimiento.PROXIMO, diaLimite.estado)
        val diaSiguiente = CalculadoraMantenimiento.calcular(referencia, null, 12, 0, LocalDate.of(2027, 1, 16))
        assertEquals(EstadoMantenimiento.VENCIDO, diaSiguiente.estado)
    }

    // ---------- Intervalos opcionales ----------

    @Test
    fun soloIntervaloKm_ignoraLaFecha() {
        val r = CalculadoraMantenimiento.calcular(referencia, 15_000, null, 55_000, LocalDate.of(2040, 1, 1))
        assertEquals(EstadoMantenimiento.AL_DIA, r.estado)
        assertNull(r.diasRestantes)
        assertEquals(CriterioVencimiento.KILOMETROS, r.criterio)
    }

    @Test
    fun soloIntervaloMeses_ignoraLosKilometros() {
        val r = CalculadoraMantenimiento.calcular(referencia, null, 12, 999_999, hoy)
        assertEquals(EstadoMantenimiento.AL_DIA, r.estado)
        assertNull(r.kmRestantes)
        assertEquals(CriterioVencimiento.FECHA, r.criterio)
    }

    @Test
    fun intervaloNoPositivo_seTrataComoNoDefinido() {
        val proximo = CalculadoraMantenimiento.calcularProximo(referencia, 0, 12)!!
        assertNull(proximo.kmLimite)
    }

    // ---------- ITV y estado general ----------

    @Test
    fun itv_evaluaSoloPorFecha() {
        assertEquals(EstadoMantenimiento.AL_DIA, CalculadoraMantenimiento.evaluarItv(LocalDate.of(2026, 9, 1), hoy).estado)
        assertEquals(EstadoMantenimiento.PROXIMO, CalculadoraMantenimiento.evaluarItv(LocalDate.of(2026, 7, 1), hoy).estado)
        assertEquals(EstadoMantenimiento.VENCIDO, CalculadoraMantenimiento.evaluarItv(LocalDate.of(2026, 6, 14), hoy).estado)
    }

    @Test
    fun estadoGeneral_esElMasGraveIgnorandoSinReferencia() {
        val evaluaciones = listOf(
            EvaluacionVencimiento(EstadoMantenimiento.AL_DIA),
            EvaluacionVencimiento(EstadoMantenimiento.SIN_REFERENCIA),
            EvaluacionVencimiento(EstadoMantenimiento.PROXIMO)
        )
        assertEquals(EstadoMantenimiento.PROXIMO, CalculadoraMantenimiento.estadoGeneral(evaluaciones))
        assertEquals(
            EstadoMantenimiento.SIN_REFERENCIA,
            CalculadoraMantenimiento.estadoGeneral(listOf(EvaluacionVencimiento(EstadoMantenimiento.SIN_REFERENCIA)))
        )
    }
}
