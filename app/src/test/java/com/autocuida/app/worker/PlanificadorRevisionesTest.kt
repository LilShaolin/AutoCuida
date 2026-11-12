package com.autocuida.app.worker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class PlanificadorRevisionesTest {

    @Test
    fun antesDeLaHora_seProgramaParaEseMismoDia() {
        val retraso = PlanificadorRevisiones.retrasoHastaProximaRevision(LocalDateTime.of(2026, 3, 1, 7, 30))
        assertEquals(Duration.ofMinutes(90), retraso)
    }

    @Test
    fun despuesDeLaHora_seProgramaParaElDiaSiguiente() {
        val retraso = PlanificadorRevisiones.retrasoHastaProximaRevision(LocalDateTime.of(2026, 3, 1, 10, 0))
        assertEquals(Duration.ofHours(23), retraso)
    }

    @Test
    fun justoALaHora_seProgramaParaElDiaSiguiente() {
        val retraso = PlanificadorRevisiones.retrasoHastaProximaRevision(LocalDateTime.of(2026, 3, 1, 9, 0))
        assertEquals(Duration.ofHours(24), retraso)
    }
}
