package com.autocuida.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.Recordatorio
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.repository.MantenimientoRepository
import com.autocuida.app.data.repository.RecordatorioRepository
import com.autocuida.app.data.repository.TipoMantenimientoRepository
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.domain.EvaluadorAvisos
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Restricciones de la tabla de recordatorios y sincronización realizada por los repositorios. */
@RunWith(AndroidJUnit4::class)
class RecordatorioTest {

    private lateinit var db: AppDatabase
    private lateinit var recordatorios: RecordatorioRepository
    private lateinit var vehiculos: VehiculoRepository
    private lateinit var mantenimientos: MantenimientoRepository
    private lateinit var tipos: TipoMantenimientoRepository
    private lateinit var aceite: TipoMantenimiento
    private lateinit var itv: TipoMantenimiento

    @Before
    fun crear() = runTest {
        db = BaseDatosPrueba.crear()
        recordatorios = RecordatorioRepository(db)
        vehiculos = VehiculoRepository(db, recordatorios)
        mantenimientos = MantenimientoRepository(db, recordatorios)
        tipos = TipoMantenimientoRepository(db, recordatorios)
        aceite = db.tipoMantenimientoDao().obtenerPorCodigo(TipoMantenimiento.CODIGO_ACEITE)!!
        itv = db.tipoMantenimientoDao().obtenerPorCodigo(TipoMantenimiento.CODIGO_ITV)!!
    }

    @After
    fun cerrar() = db.close()

    private suspend fun nuevoVehiculo(): Long =
        (vehiculos.guardar(BaseDatosPrueba.vehiculo()) as VehiculoRepository.ResultadoGuardado.Guardado).id

    @Test(expected = SQLiteConstraintException::class)
    fun uniqueVehiculoTipoImpideDuplicados() = runTest {
        val vehiculoId = db.vehiculoDao().insertar(BaseDatosPrueba.vehiculo())
        db.recordatorioDao().insertar(Recordatorio(vehiculoId = vehiculoId, tipoId = aceite.id, fechaLimite = 1, kmLimite = null))
        db.recordatorioDao().insertar(Recordatorio(vehiculoId = vehiculoId, tipoId = aceite.id, fechaLimite = 2, kmLimite = null))
    }

    @Test
    fun vehiculoNuevo_soloTieneRecordatorioDeItv() = runTest {
        val id = nuevoVehiculo()
        val lista = db.recordatorioDao().obtenerPorVehiculo(id)
        assertEquals(1, lista.size)
        assertEquals(itv.id, lista[0].tipoId)
        assertEquals(LocalDate.of(2027, 5, 12).toEpochDay(), lista[0].fechaLimite)
        assertNull(lista[0].kmLimite)
    }

    @Test
    fun registrarMantenimiento_creaRecordatorioConLosLimitesCalculados() = runTest {
        val id = nuevoVehiculo()
        mantenimientos.guardar(BaseDatosPrueba.mantenimiento(id, aceite.id, LocalDate.of(2026, 1, 15), 45_000))
        val r = db.recordatorioDao().obtener(id, aceite.id)!!
        assertEquals(60_000, r.kmLimite) // 45.000 + 15.000
        assertEquals(LocalDate.of(2027, 1, 15).toEpochDay(), r.fechaLimite) // + 12 meses
    }

    @Test
    fun mantenimientoConMasKmQueElVehiculo_actualizaElCuentakilometros() = runTest {
        val id = nuevoVehiculo() // 50.000 km
        mantenimientos.guardar(BaseDatosPrueba.mantenimiento(id, aceite.id, km = 52_300))
        assertEquals(52_300, db.vehiculoDao().obtenerPorId(id)!!.kmActuales)
    }

    @Test
    fun itvRegistrada_actualizaLaFechaDelVehiculoYSuRecordatorio() = runTest {
        val id = nuevoVehiculo()
        val nueva = LocalDate.of(2028, 5, 20).toEpochDay()
        val r = mantenimientos.guardar(BaseDatosPrueba.mantenimiento(id, itv.id, LocalDate.of(2026, 5, 20)), nuevaFechaItv = nueva)
        assertFalse(r.fechaItvMantenida)
        assertEquals(nueva, db.vehiculoDao().obtenerPorId(id)!!.fechaItv)
        assertEquals(nueva, db.recordatorioDao().obtener(id, itv.id)!!.fechaLimite)
    }

    @Test
    fun itvAntiguaRegistrada_noAdelantaLaFechaDelVehiculo() = runTest {
        val id = nuevoVehiculo()
        val actual = db.vehiculoDao().obtenerPorId(id)!!.fechaItv
        val anterior = LocalDate.of(2025, 6, 1).toEpochDay()
        val r = mantenimientos.guardar(BaseDatosPrueba.mantenimiento(id, itv.id, LocalDate.of(2024, 6, 1)), nuevaFechaItv = anterior)
        // Se informa de que la fecha del vehículo se mantiene, para avisar al usuario.
        assertTrue(r.fechaItvMantenida)
        assertEquals(actual, db.vehiculoDao().obtenerPorId(id)!!.fechaItv)
        assertEquals(actual, db.recordatorioDao().obtener(id, itv.id)!!.fechaLimite)
    }

    @Test
    fun cambiarIntervalo_recalculaYReiniciaElNivelAvisado() = runTest {
        val id = nuevoVehiculo()
        mantenimientos.guardar(BaseDatosPrueba.mantenimiento(id, aceite.id, LocalDate.of(2026, 1, 15), 45_000))
        val antes = db.recordatorioDao().obtener(id, aceite.id)!!
        db.recordatorioDao().marcarAvisado(antes.id, EvaluadorAvisos.NIVEL_PROXIMO)

        tipos.guardar(aceite.copy(intervaloKm = 10_000))
        val despues = db.recordatorioDao().obtener(id, aceite.id)!!
        assertEquals(55_000, despues.kmLimite)
        assertEquals(EvaluadorAvisos.NIVEL_NINGUNO, despues.avisado)
    }

    @Test
    fun recalcularSinCambios_conservaElNivelAvisado() = runTest {
        val id = nuevoVehiculo()
        mantenimientos.guardar(BaseDatosPrueba.mantenimiento(id, aceite.id, LocalDate.of(2026, 1, 15), 45_000))
        val r = db.recordatorioDao().obtener(id, aceite.id)!!
        db.recordatorioDao().marcarAvisado(r.id, EvaluadorAvisos.NIVEL_VENCIDO)
        vehiculos.actualizarKm(id, 70_000)
        assertEquals(EvaluadorAvisos.NIVEL_VENCIDO, db.recordatorioDao().obtener(id, aceite.id)!!.avisado)
    }

    @Test
    fun eliminarElUnicoMantenimiento_eliminaElRecordatorio() = runTest {
        val id = nuevoVehiculo()
        val mId = mantenimientos.guardar(BaseDatosPrueba.mantenimiento(id, aceite.id)).id
        mantenimientos.eliminar(db.mantenimientoDao().obtenerPorId(mId)!!)
        assertNull(db.recordatorioDao().obtener(id, aceite.id))
    }

    @Test
    fun repositorioDeTipos_noEliminaTiposEnUsoNiLaItv() = runTest {
        val id = nuevoVehiculo()
        mantenimientos.guardar(BaseDatosPrueba.mantenimiento(id, aceite.id))
        assertEquals(TipoMantenimientoRepository.ResultadoEliminacion.EnUso(1), tipos.eliminar(aceite))
        assertEquals(TipoMantenimientoRepository.ResultadoEliminacion.Protegido, tipos.eliminar(itv))
    }

    @Test
    fun repositorioDeTipos_detectaNombreDuplicadoSinDistinguirMayusculasNiTildes() = runTest {
        val r = tipos.guardar(TipoMantenimiento(nombre = " NEUMATICOS ", intervaloKm = 5_000, intervaloMeses = null))
        assertEquals(TipoMantenimientoRepository.ResultadoGuardado.NombreDuplicado, r)
    }

    @Test
    fun repositorioDeVehiculos_detectaMatriculaDuplicada() = runTest {
        nuevoVehiculo()
        val r = vehiculos.guardar(BaseDatosPrueba.vehiculo(matricula = "1234ABC"))
        assertTrue(r is VehiculoRepository.ResultadoGuardado.MatriculaDuplicada)
    }
}
