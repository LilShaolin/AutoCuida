package com.autocuida.app.domain.validacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorVehiculoTest {

    private val valida = ValidadorVehiculo.Entrada(
        marca = "Seat", modelo = "León", matricula = "1234 abc", anio = "2018",
        combustible = "Gasolina", km = "84250", fechaItvEpochDay = 20_000
    )

    @Test
    fun entradaCorrecta_esValida() {
        assertTrue(ValidadorVehiculo.validar(valida, 2026).esValido)
    }

    @Test
    fun matricula_seNormalizaSinEspaciosNiGuionesYEnMayusculas() {
        assertEquals("1234ABC", ValidadorVehiculo.normalizarMatricula(" 1234-abc "))
    }

    @Test
    fun camposObligatoriosVacios_devuelvenError() {
        val r = ValidadorVehiculo.validar(
            ValidadorVehiculo.Entrada("", " ", "", "", "", "", null), 2026
        )
        ValidadorVehiculo.Campo.entries.forEach { assertEquals(ErrorValidacion.OBLIGATORIO, r.error(it)) }
    }

    @Test
    fun matriculaConFormatoIncorrecto_devuelveError() {
        val r = ValidadorVehiculo.validar(valida.copy(matricula = "AB1"), 2026)
        assertEquals(ErrorValidacion.MATRICULA_FORMATO, r.error(ValidadorVehiculo.Campo.MATRICULA))
    }

    @Test
    fun anioFueraDeRango_devuelveError() {
        assertEquals(ErrorValidacion.ANIO_FUERA_DE_RANGO,
            ValidadorVehiculo.validar(valida.copy(anio = "1899"), 2026).error(ValidadorVehiculo.Campo.ANIO))
        assertEquals(ErrorValidacion.ANIO_FUERA_DE_RANGO,
            ValidadorVehiculo.validar(valida.copy(anio = "2028"), 2026).error(ValidadorVehiculo.Campo.ANIO))
        assertNull(ValidadorVehiculo.validar(valida.copy(anio = "2027"), 2026).error(ValidadorVehiculo.Campo.ANIO))
    }

    @Test
    fun kilometrosNegativosOExcesivos_devuelvenError() {
        assertEquals(ErrorValidacion.KM_NEGATIVOS, ValidadorVehiculo.validarKm("-5"))
        assertEquals(ErrorValidacion.KM_EXCESIVOS, ValidadorVehiculo.validarKm("2000001"))
        assertEquals(ErrorValidacion.NUMERO_NO_VALIDO, ValidadorVehiculo.validarKm("12a"))
        assertNull(ValidadorVehiculo.validarKm("0"))
    }
}

class ValidadorMantenimientoTest {

    private val hoy = 20_000L
    private val entrada = ValidadorMantenimiento.Entrada(
        tipoId = 1, fechaEpochDay = hoy - 10, km = "50000", coste = "89,90", taller = "", notas = ""
    )
    private val sinHistorial = ValidadorMantenimiento.Contexto(hoy, null, null)

    @Test
    fun entradaCorrecta_esValida() {
        assertTrue(ValidadorMantenimiento.validar(entrada, sinHistorial).esValido)
    }

    @Test
    fun costeAdmiteComaPuntoYVacio() {
        assertEquals(89.9, ValidadorMantenimiento.parsearCoste("89,90")!!, 0.001)
        assertEquals(89.9, ValidadorMantenimiento.parsearCoste("89.90")!!, 0.001)
        assertEquals(1234.5, ValidadorMantenimiento.parsearCoste("1.234,50")!!, 0.001)
        assertEquals(0.0, ValidadorMantenimiento.parsearCoste("")!!, 0.0)
        assertNull(ValidadorMantenimiento.parsearCoste("abc"))
    }

    @Test
    fun costeNegativo_devuelveError() {
        val r = ValidadorMantenimiento.validar(entrada.copy(coste = "-1"), sinHistorial)
        assertEquals(ErrorValidacion.COSTE_NEGATIVO, r.error(ValidadorMantenimiento.Campo.COSTE))
    }

    @Test
    fun fechaFutura_devuelveError() {
        val r = ValidadorMantenimiento.validar(entrada.copy(fechaEpochDay = hoy + 1), sinHistorial)
        assertEquals(ErrorValidacion.FECHA_FUTURA, r.error(ValidadorMantenimiento.Campo.FECHA))
        assertTrue(ValidadorMantenimiento.validar(entrada.copy(fechaEpochDay = hoy), sinHistorial).esValido)
    }

    @Test
    fun kmInferioresAUnMantenimientoAnterior_devuelveError() {
        val r = ValidadorMantenimiento.validar(entrada, ValidadorMantenimiento.Contexto(hoy, 52_000, null))
        assertEquals(ErrorValidacion.KM_INFERIORES_A_ANTERIOR, r.error(ValidadorMantenimiento.Campo.KM))
    }

    @Test
    fun kmSuperioresAUnMantenimientoPosterior_devuelveError() {
        val r = ValidadorMantenimiento.validar(entrada, ValidadorMantenimiento.Contexto(hoy, null, 48_000))
        assertEquals(ErrorValidacion.KM_SUPERIORES_A_POSTERIOR, r.error(ValidadorMantenimiento.Campo.KM))
    }

    @Test
    fun kmIgualesALosLimites_sonValidos() {
        assertTrue(ValidadorMantenimiento.validar(entrada, ValidadorMantenimiento.Contexto(hoy, 50_000, 50_000)).esValido)
    }

    @Test
    fun tipoObligatorio() {
        val r = ValidadorMantenimiento.validar(entrada.copy(tipoId = null), sinHistorial)
        assertEquals(ErrorValidacion.OBLIGATORIO, r.error(ValidadorMantenimiento.Campo.TIPO))
    }

    @Test
    fun proximaItvDebeSerPosteriorALaInspeccion() {
        val itv = entrada.copy(esItv = true, proximaItvEpochDay = entrada.fechaEpochDay)
        assertEquals(ErrorValidacion.FECHA_ITV_ANTERIOR,
            ValidadorMantenimiento.validar(itv, sinHistorial).error(ValidadorMantenimiento.Campo.FECHA_ITV))
        assertTrue(ValidadorMantenimiento.validar(itv.copy(proximaItvEpochDay = hoy + 365), sinHistorial).esValido)
    }
}

class ValidadorKilometrosTest {

    @Test
    fun valorMayorOIgual_esValido() {
        assertEquals(ValidadorKilometros.Resultado.Valido(90_000, false),
            ValidadorKilometros.validar("90000", 85_000, 80_000, false))
        assertEquals(ValidadorKilometros.Resultado.Valido(85_000, false),
            ValidadorKilometros.validar("85000", 85_000, 80_000, false))
    }

    @Test
    fun retrocesoSinMarcarCorreccion_esInvalido() {
        assertEquals(ValidadorKilometros.Resultado.Invalido(ErrorValidacion.KM_RETROCESO),
            ValidadorKilometros.validar("84000", 85_000, 80_000, false))
    }

    @Test
    fun retrocesoComoCorreccion_esValidoSiNoBajaDelHistorial() {
        assertEquals(ValidadorKilometros.Resultado.Valido(84_000, true),
            ValidadorKilometros.validar("84000", 85_000, 80_000, true))
    }

    @Test
    fun correccionPorDebajoDelHistorial_esInvalida() {
        assertEquals(ValidadorKilometros.Resultado.Invalido(ErrorValidacion.KM_INFERIORES_A_HISTORIAL),
            ValidadorKilometros.validar("79000", 85_000, 80_000, true))
    }

    @Test
    fun negativo_esInvalido() {
        assertEquals(ValidadorKilometros.Resultado.Invalido(ErrorValidacion.KM_NEGATIVOS),
            ValidadorKilometros.validar("-1", 0, null, true))
    }
}

class ValidadorTipoTest {

    @Test
    fun alMenosUnIntervaloEsObligatorio() {
        val r = ValidadorTipo.validar(ValidadorTipo.Entrada("Embrague", "", ""))
        assertFalse(r.esValido)
        assertEquals(ErrorValidacion.INTERVALO_REQUERIDO, r.error(ValidadorTipo.Campo.INTERVALO_KM))
    }

    @Test
    fun soloKmOSoloMeses_sonValidos() {
        assertTrue(ValidadorTipo.validar(ValidadorTipo.Entrada("Embrague", "120000", "")).esValido)
        assertTrue(ValidadorTipo.validar(ValidadorTipo.Entrada("Embrague", "", "48")).esValido)
    }

    @Test
    fun intervaloCeroOExcesivo_esInvalido() {
        assertEquals(ErrorValidacion.INTERVALO_NO_POSITIVO,
            ValidadorTipo.validar(ValidadorTipo.Entrada("X", "0", "")).error(ValidadorTipo.Campo.INTERVALO_KM))
        assertEquals(ErrorValidacion.INTERVALO_EXCESIVO,
            ValidadorTipo.validar(ValidadorTipo.Entrada("X", "", "241")).error(ValidadorTipo.Campo.INTERVALO_MESES))
    }

    @Test
    fun nombreObligatorio() {
        assertEquals(ErrorValidacion.OBLIGATORIO,
            ValidadorTipo.validar(ValidadorTipo.Entrada(" ", "1000", "")).error(ValidadorTipo.Campo.NOMBRE))
    }
}
