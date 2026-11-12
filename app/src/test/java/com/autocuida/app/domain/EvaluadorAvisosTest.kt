package com.autocuida.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluadorAvisosTest {

    @Test
    fun primerAvisoProximo_seNotifica() {
        val d = EvaluadorAvisos.decidir(EstadoMantenimiento.PROXIMO, EvaluadorAvisos.NIVEL_NINGUNO)
        assertTrue(d.notificar)
        assertEquals(EvaluadorAvisos.NIVEL_PROXIMO, d.nuevoNivel)
    }

    @Test
    fun mismoNivelYaAvisado_noSeRepite() {
        val d = EvaluadorAvisos.decidir(EstadoMantenimiento.PROXIMO, EvaluadorAvisos.NIVEL_PROXIMO)
        assertFalse(d.notificar)
        assertEquals(EvaluadorAvisos.NIVEL_PROXIMO, d.nuevoNivel)
    }

    @Test
    fun pasarDeProximoAVencido_seNotificaDeNuevo() {
        val d = EvaluadorAvisos.decidir(EstadoMantenimiento.VENCIDO, EvaluadorAvisos.NIVEL_PROXIMO)
        assertTrue(d.notificar)
        assertEquals(EvaluadorAvisos.NIVEL_VENCIDO, d.nuevoNivel)
    }

    @Test
    fun vencidoYaAvisado_noSeRepiteCadaDia() {
        assertFalse(EvaluadorAvisos.decidir(EstadoMantenimiento.VENCIDO, EvaluadorAvisos.NIVEL_VENCIDO).notificar)
    }

    @Test
    fun siElEstadoMejora_seRebajaElNivelSinNotificar() {
        val d = EvaluadorAvisos.decidir(EstadoMantenimiento.AL_DIA, EvaluadorAvisos.NIVEL_VENCIDO)
        assertFalse(d.notificar)
        assertEquals(EvaluadorAvisos.NIVEL_NINGUNO, d.nuevoNivel)
    }

    @Test
    fun alDiaOSinReferencia_nuncaNotifican() {
        assertFalse(EvaluadorAvisos.decidir(EstadoMantenimiento.AL_DIA, EvaluadorAvisos.NIVEL_NINGUNO).notificar)
        assertFalse(EvaluadorAvisos.decidir(EstadoMantenimiento.SIN_REFERENCIA, EvaluadorAvisos.NIVEL_NINGUNO).notificar)
    }
}
