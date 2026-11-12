package com.autocuida.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autocuida.app.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehiculoDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun crear() {
        db = BaseDatosPrueba.crear()
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun insertarYConsultarVehiculo() = runTest {
        val dao = db.vehiculoDao()
        val id = dao.insertar(BaseDatosPrueba.vehiculo())
        val guardado = dao.obtenerPorId(id)!!
        assertEquals("1234ABC", guardado.matricula)
        assertEquals(50_000, guardado.kmActuales)
        assertEquals(listOf(guardado), dao.observarTodos().first())
    }

    @Test(expected = SQLiteConstraintException::class)
    fun matriculaDuplicadaLanzaExcepcion() = runTest {
        val dao = db.vehiculoDao()
        dao.insertar(BaseDatosPrueba.vehiculo(matricula = "1234ABC"))
        dao.insertar(BaseDatosPrueba.vehiculo(matricula = "1234ABC"))
    }

    @Test
    fun existeMatricula_excluyeElPropioVehiculo() = runTest {
        val dao = db.vehiculoDao()
        val id = dao.insertar(BaseDatosPrueba.vehiculo(matricula = "1234ABC"))
        assertTrue(dao.existeMatricula("1234ABC"))
        assertFalse(dao.existeMatricula("1234ABC", excluirId = id))
    }

    @Test
    fun actualizarKmYFechaItv() = runTest {
        val dao = db.vehiculoDao()
        val id = dao.insertar(BaseDatosPrueba.vehiculo())
        dao.actualizarKm(id, 61_000)
        dao.actualizarFechaItv(id, 21_000)
        val v = dao.obtenerPorId(id)!!
        assertEquals(61_000, v.kmActuales)
        assertEquals(21_000L, v.fechaItv)
    }

    @Test
    fun eliminarVehiculo() = runTest {
        val dao = db.vehiculoDao()
        val id = dao.insertar(BaseDatosPrueba.vehiculo())
        dao.eliminar(dao.obtenerPorId(id)!!)
        assertNull(dao.obtenerPorId(id))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun triggerRechazaKilometrosNegativos() = runTest {
        db.vehiculoDao().insertar(BaseDatosPrueba.vehiculo(km = -1))
    }
}
