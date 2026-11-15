package com.autocuida.app.ui.vehiculos

import com.autocuida.app.domain.EstadoMantenimiento
import com.autocuida.app.domain.EvaluacionVencimiento
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdenUrgenciaTest {

    private fun vencimiento(nombre: String, estado: EstadoMantenimiento, km: Int? = null, dias: Long? = null) =
        VencimientoEvaluado(
            tipoId = nombre.hashCode().toLong(),
            tipoNombre = nombre,
            esItv = false,
            evaluacion = EvaluacionVencimiento(estado, kmRestantes = km, diasRestantes = dias)
        )

    private fun ordenar(vararg items: VencimientoEvaluado) =
        items.sortedWith(ResumenVehiculo.ORDEN_URGENCIA).map { it.tipoNombre }

    @Test
    fun primeroElEstadoMasGrave() {
        val orden = ordenar(
            vencimiento("Aceite", EstadoMantenimiento.AL_DIA, km = 5_000),
            vencimiento("Frenos", EstadoMantenimiento.VENCIDO, km = -10),
            vencimiento("Filtros", EstadoMantenimiento.PROXIMO, km = 500)
        )
        assertEquals(listOf("Frenos", "Filtros", "Aceite"), orden)
    }

    @Test
    fun conIgualEstado_unVencimientoPorFechaCercanoVaAntesQueUnoPorKmLejano() {
        // 3 días son el 10 % del umbral; 999 km, casi el 100 %.
        val orden = ordenar(
            vencimiento("Neumáticos", EstadoMantenimiento.PROXIMO, km = 999),
            vencimiento("ITV", EstadoMantenimiento.PROXIMO, dias = 3)
        )
        assertEquals(listOf("ITV", "Neumáticos"), orden)
    }

    @Test
    fun sinReferencia_vaAlFinal() {
        val orden = ordenar(
            vencimiento("Distribución", EstadoMantenimiento.SIN_REFERENCIA),
            vencimiento("Aceite", EstadoMantenimiento.AL_DIA, dias = 200)
        )
        assertEquals(listOf("Aceite", "Distribución"), orden)
    }
}
