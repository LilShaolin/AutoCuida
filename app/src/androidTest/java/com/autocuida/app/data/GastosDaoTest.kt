package com.autocuida.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.repository.GastosRepository
import com.autocuida.app.domain.PeriodoGastos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class GastosDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var gastos: GastosRepository
    private var cocheA = 0L
    private var cocheB = 0L

    @Before
    fun crear() = runTest {
        db = BaseDatosPrueba.crear()
        gastos = GastosRepository(db)
        val tipos = db.tipoMantenimientoDao()
        val aceite = tipos.obtenerPorCodigo(TipoMantenimiento.CODIGO_ACEITE)!!.id
        val frenos = tipos.obtenerPorCodigo(TipoMantenimiento.CODIGO_FRENOS)!!.id
        cocheA = db.vehiculoDao().insertar(BaseDatosPrueba.vehiculo(matricula = "1111AAA"))
        cocheB = db.vehiculoDao().insertar(BaseDatosPrueba.vehiculo(matricula = "2222BBB"))
        val dao = db.mantenimientoDao()
        dao.insertar(BaseDatosPrueba.mantenimiento(cocheA, aceite, LocalDate.of(2025, 11, 3), 40_000, 70.0))
        dao.insertar(BaseDatosPrueba.mantenimiento(cocheA, aceite, LocalDate.of(2026, 1, 20), 45_000, 90.5))
        dao.insertar(BaseDatosPrueba.mantenimiento(cocheA, frenos, LocalDate.of(2026, 3, 2), 46_000, 210.0))
        dao.insertar(BaseDatosPrueba.mantenimiento(cocheB, aceite, LocalDate.of(2026, 3, 28), 12_000, 60.0))
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun totalDelAnioSoloIncluyeEsePeriodo() = runTest {
        assertEquals(360.5, gastos.observarTotal(PeriodoGastos.Anio(2026), null).first(), 0.001)
        assertEquals(70.0, gastos.observarTotal(PeriodoGastos.Anio(2025), null).first(), 0.001)
        assertEquals(430.5, gastos.observarTotal(PeriodoGastos.Todo, null).first(), 0.001)
    }

    @Test
    fun totalFiltradoPorVehiculo() = runTest {
        assertEquals(300.5, gastos.observarTotal(PeriodoGastos.Anio(2026), cocheA).first(), 0.001)
        assertEquals(60.0, gastos.observarTotal(PeriodoGastos.Anio(2026), cocheB).first(), 0.001)
    }

    @Test
    fun gastosPorTipoOrdenadosDeMayorAMenor() = runTest {
        val porTipo = gastos.observarPorTipo(PeriodoGastos.Anio(2026), null).first()
        assertEquals(listOf("Frenos", "Aceite"), porTipo.map { it.nombre })
        assertEquals(150.5, porTipo[1].total, 0.001)
        assertEquals(2, porTipo[1].cantidad)
    }

    @Test
    fun gastosPorVehiculo() = runTest {
        val porVehiculo = gastos.observarPorVehiculo(PeriodoGastos.Anio(2026), null).first()
        assertEquals(2, porVehiculo.size)
        assertEquals(cocheA, porVehiculo[0].id)
        assertEquals(300.5, porVehiculo[0].total, 0.001)
    }

    @Test
    fun serieMensualYAnual() = runTest {
        val mensual = gastos.observarSerie(PeriodoGastos.Anio(2026), null).first().associate { it.periodo to it.total }
        assertEquals(mapOf(1 to 90.5, 3 to 270.0), mensual)
        val anual = gastos.observarSerie(PeriodoGastos.Todo, null).first().associate { it.periodo to it.total }
        assertEquals(mapOf(2025 to 70.0, 2026 to 360.5), anual)
    }

    @Test
    fun aniosConGastosEHistorialDelPeriodo() = runTest {
        assertEquals(listOf(2026, 2025), gastos.observarAnios().first())
        assertEquals(3, gastos.observarHistorial(PeriodoGastos.Anio(2026), null).first().size)
    }

    @Test
    fun periodoSinRegistrosDevuelveCero() = runTest {
        assertEquals(0.0, gastos.observarTotal(PeriodoGastos.Anio(2020), null).first(), 0.0)
    }
}
