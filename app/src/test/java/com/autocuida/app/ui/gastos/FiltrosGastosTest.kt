package com.autocuida.app.ui.gastos

import com.autocuida.app.domain.PeriodoGastos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FiltrosGastosTest {

    private val periodo = PeriodoGastos.Anio(2026)

    @Test
    fun vehiculoExistente_seConservaElFiltro() {
        val filtros = GastosViewModel.Filtros(periodo, vehiculoId = 3)
        assertEquals(filtros, filtros.ajustadosA(listOf(1L, 3L)))
    }

    @Test
    fun vehiculoEliminado_seVuelveATodosLosVehiculos() {
        val ajustados = GastosViewModel.Filtros(periodo, vehiculoId = 3).ajustadosA(listOf(1L, 2L))
        assertNull(ajustados.vehiculoId)
        assertEquals(periodo, ajustados.periodo)
    }

    @Test
    fun sinFiltroDeVehiculo_noCambia() {
        val filtros = GastosViewModel.Filtros(periodo, vehiculoId = null)
        assertEquals(filtros, filtros.ajustadosA(emptyList()))
    }
}
