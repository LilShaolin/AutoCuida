package com.autocuida.app.ui.vehiculos

import com.autocuida.app.data.entity.Recordatorio
import com.autocuida.app.data.entity.RecordatorioDetalle
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.domain.EstadoMantenimiento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ResumenVehiculoTest {

    private val hoy = LocalDate.of(2026, 6, 15)

    private fun vehiculo(itv: LocalDate, km: Int = 50_000) = Vehiculo(
        id = 1, marca = "Seat", modelo = "León", matricula = "1234ABC", anio = 2018,
        combustible = "Gasolina", kmActuales = km, fechaItv = itv.toEpochDay()
    )

    private fun recordatorio(tipoId: Long, nombre: String, codigo: String?, kmLimite: Int?, fechaLimite: LocalDate?, km: Int) =
        RecordatorioDetalle(
            recordatorio = Recordatorio(id = tipoId, vehiculoId = 1, tipoId = tipoId,
                fechaLimite = fechaLimite?.toEpochDay(), kmLimite = kmLimite),
            tipoNombre = nombre, tipoCodigo = codigo, intervaloKm = null, intervaloMeses = null,
            vehiculoDescripcion = "Seat León", vehiculoMatricula = "1234ABC", kmActuales = km
        )

    @Test
    fun sinHistorial_elEstadoEsSinReferenciaAunqueLaItvEsteAlDia() {
        val itv = LocalDate.of(2027, 1, 1)
        val itvRecordatorio = recordatorio(6, "ITV", TipoMantenimiento.CODIGO_ITV, null, itv, 50_000)
        val resumen = ResumenVehiculo.construir(listOf(vehiculo(itv)), listOf(itvRecordatorio), hoy).single()
        assertEquals(EstadoMantenimiento.SIN_REFERENCIA, resumen.estado)
        assertEquals(EstadoMantenimiento.AL_DIA, resumen.itv.estado)
        assertEquals(0, resumen.avisos)
    }

    @Test
    fun avisos_cuentaCadaVencimientoProximoOVencidoIncluidaLaItv() {
        val itv = LocalDate.of(2026, 6, 20) // faltan 5 días: próxima
        val lista = listOf(
            recordatorio(1, "Aceite", TipoMantenimiento.CODIGO_ACEITE, 49_000, null, 50_000), // vencido
            recordatorio(4, "Frenos", TipoMantenimiento.CODIGO_FRENOS, 50_500, null, 50_000), // próximo
            recordatorio(3, "Neumáticos", TipoMantenimiento.CODIGO_NEUMATICOS, 70_000, null, 50_000), // al día
            recordatorio(6, "ITV", TipoMantenimiento.CODIGO_ITV, null, itv, 50_000)
        )
        val resumen = ResumenVehiculo.construir(listOf(vehiculo(itv)), lista, hoy).single()
        assertEquals(3, resumen.avisos)
        assertEquals(EstadoMantenimiento.VENCIDO, resumen.estado)
    }
}
