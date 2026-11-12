package com.autocuida.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.Recordatorio
import com.autocuida.app.data.entity.TipoMantenimiento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MantenimientoDaoTest {

    private lateinit var db: AppDatabase
    private var vehiculoId = 0L
    private var aceiteId = 0L
    private var frenosId = 0L

    @Before
    fun crear() = runTest {
        db = BaseDatosPrueba.crear()
        vehiculoId = db.vehiculoDao().insertar(BaseDatosPrueba.vehiculo())
        aceiteId = db.tipoMantenimientoDao().obtenerPorCodigo(TipoMantenimiento.CODIGO_ACEITE)!!.id
        frenosId = db.tipoMantenimientoDao().obtenerPorCodigo(TipoMantenimiento.CODIGO_FRENOS)!!.id
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun insertarYConsultarHistorialConRelaciones() = runTest {
        val dao = db.mantenimientoDao()
        dao.insertar(BaseDatosPrueba.mantenimiento(vehiculoId, aceiteId, LocalDate.of(2025, 1, 10), 30_000))
        dao.insertar(BaseDatosPrueba.mantenimiento(vehiculoId, frenosId, LocalDate.of(2026, 2, 1), 45_000))
        val historial = dao.observarPorVehiculo(vehiculoId).first()
        assertEquals(2, historial.size)
        // Orden: del más reciente al más antiguo, con el nombre del tipo resuelto por JOIN.
        assertEquals("Frenos", historial[0].tipoNombre)
        assertEquals("Aceite", historial[1].tipoNombre)
        assertEquals("1234ABC", historial[0].vehiculoMatricula)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun claveForaneaRechazaVehiculoInexistente() = runTest {
        db.mantenimientoDao().insertar(BaseDatosPrueba.mantenimiento(vehiculoId = 999, tipoId = aceiteId))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun claveForaneaRechazaTipoInexistente() = runTest {
        db.mantenimientoDao().insertar(BaseDatosPrueba.mantenimiento(vehiculoId, tipoId = 999))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun triggerRechazaCosteNegativo() = runTest {
        db.mantenimientoDao().insertar(BaseDatosPrueba.mantenimiento(vehiculoId, aceiteId, coste = -10.0))
    }

    @Test
    fun borrarVehiculoEliminaEnCascadaMantenimientosYRecordatorios() = runTest {
        val id = db.mantenimientoDao().insertar(BaseDatosPrueba.mantenimiento(vehiculoId, aceiteId))
        db.recordatorioDao().insertar(Recordatorio(vehiculoId = vehiculoId, tipoId = aceiteId, fechaLimite = 1, kmLimite = 1))
        db.vehiculoDao().eliminar(db.vehiculoDao().obtenerPorId(vehiculoId)!!)
        assertNull(db.mantenimientoDao().obtenerPorId(id))
        assertTrue(db.recordatorioDao().obtenerPorVehiculo(vehiculoId).isEmpty())
    }

    @Test
    fun ultimoPorTipo_eligeElMasRecientePorFechaYKm() = runTest {
        val dao = db.mantenimientoDao()
        dao.insertar(BaseDatosPrueba.mantenimiento(vehiculoId, aceiteId, LocalDate.of(2024, 3, 1), 20_000))
        dao.insertar(BaseDatosPrueba.mantenimiento(vehiculoId, aceiteId, LocalDate.of(2025, 3, 1), 35_000))
        dao.insertar(BaseDatosPrueba.mantenimiento(vehiculoId, frenosId, LocalDate.of(2025, 6, 1), 38_000))
        val ultimos = dao.obtenerUltimosPorTipo(vehiculoId).associateBy { it.tipoId }
        assertEquals(35_000, ultimos[aceiteId]!!.km)
        assertEquals(LocalDate.of(2025, 3, 1).toEpochDay(), ultimos[aceiteId]!!.fecha)
        assertEquals(38_000, ultimos[frenosId]!!.km)
    }

    @Test
    fun limitesDeKilometrosPorFecha() = runTest {
        val dao = db.mantenimientoDao()
        dao.insertar(BaseDatosPrueba.mantenimiento(vehiculoId, aceiteId, LocalDate.of(2024, 3, 1), 20_000))
        dao.insertar(BaseDatosPrueba.mantenimiento(vehiculoId, frenosId, LocalDate.of(2025, 6, 1), 38_000))
        val fecha = LocalDate.of(2025, 1, 1).toEpochDay()
        assertEquals(20_000, dao.kmMaximoAntesDe(vehiculoId, fecha, 0))
        assertEquals(38_000, dao.kmMinimoDespuesDe(vehiculoId, fecha, 0))
        assertEquals(38_000, dao.kmMaximoRegistrado(vehiculoId))
    }
}
