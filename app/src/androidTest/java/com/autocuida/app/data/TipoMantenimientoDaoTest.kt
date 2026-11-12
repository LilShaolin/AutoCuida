package com.autocuida.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.TipoMantenimiento
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TipoMantenimientoDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun crear() {
        db = BaseDatosPrueba.crear()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun precargaCreaSeisTipos() = runTest {
        val tipos = db.tipoMantenimientoDao().obtenerTodos()
        assertEquals(listOf("Aceite", "Filtros", "Neumáticos", "Frenos", "Distribución", "ITV"), tipos.map { it.nombre })
        assertTrue(tipos.all { it.intervaloKm != null || it.intervaloMeses != null })
        assertNotNull(db.tipoMantenimientoDao().obtenerPorCodigo(TipoMantenimiento.CODIGO_ITV))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun triggerRechazaTipoSinIntervalos() = runTest {
        db.tipoMantenimientoDao().insertar(TipoMantenimiento(nombre = "Embrague", intervaloKm = null, intervaloMeses = null))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun triggerRechazaActualizacionSinIntervalos() = runTest {
        val dao = db.tipoMantenimientoDao()
        val aceite = dao.obtenerPorCodigo(TipoMantenimiento.CODIGO_ACEITE)!!
        dao.actualizar(aceite.copy(intervaloKm = null, intervaloMeses = null))
    }

    @Test
    fun tipoConUnSoloIntervaloEsValido() = runTest {
        val dao = db.tipoMantenimientoDao()
        dao.insertar(TipoMantenimiento(nombre = "Embrague", intervaloKm = 120_000, intervaloMeses = null))
        dao.insertar(TipoMantenimiento(nombre = "Líquido de frenos", intervaloKm = null, intervaloMeses = 24))
        assertEquals(8, dao.obtenerTodos().size)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun nombreDuplicadoLanzaExcepcion() = runTest {
        db.tipoMantenimientoDao().insertar(TipoMantenimiento(nombre = "Aceite", intervaloKm = 1000, intervaloMeses = null))
    }

    @Test
    fun restrictImpideBorrarTipoConMantenimientos() = runTest {
        val vehiculoId = db.vehiculoDao().insertar(BaseDatosPrueba.vehiculo())
        val dao = db.tipoMantenimientoDao()
        val aceite = dao.obtenerPorCodigo(TipoMantenimiento.CODIGO_ACEITE)!!
        db.mantenimientoDao().insertar(BaseDatosPrueba.mantenimiento(vehiculoId, aceite.id))
        assertEquals(1, dao.contarMantenimientos(aceite.id))
        try {
            dao.eliminar(aceite)
            fail("Se esperaba SQLiteConstraintException por la restricción RESTRICT")
        } catch (e: SQLiteConstraintException) {
            assertNotNull(dao.obtenerPorId(aceite.id))
        }
    }

    @Test
    fun tipoSinMantenimientosSePuedeBorrar() = runTest {
        val dao = db.tipoMantenimientoDao()
        val frenos = dao.obtenerPorCodigo(TipoMantenimiento.CODIGO_FRENOS)!!
        dao.eliminar(frenos)
        assertEquals(5, dao.obtenerTodos().size)
    }
}
