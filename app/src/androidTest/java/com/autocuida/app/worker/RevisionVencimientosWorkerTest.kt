package com.autocuida.app.worker

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.autocuida.app.AutoCuidaApp
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.domain.EvaluadorAvisos
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Ejecuta el Worker real sobre la base de datos de la aplicación: un vehículo con la ITV vencida
 * debe generar una notificación, y una segunda revisión no debe repetirla.
 */
@RunWith(AndroidJUnit4::class)
class RevisionVencimientosWorkerTest {

    @get:Rule
    val permiso: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contenedor get() = (context as AutoCuidaApp).contenedor
    private val gestor get() = context.getSystemService(NotificationManager::class.java)
    private val matricula = "W" + (System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')
    private var vehiculoId = 0L

    @Before
    fun crearVehiculoConItvVencida() = runBlocking {
        gestor.cancelAll()
        val r = contenedor.vehiculoRepository.guardar(
            Vehiculo(
                marca = "Prueba", modelo = "Worker", matricula = matricula, anio = 2015, combustible = "Diésel",
                kmActuales = 100_000, fechaItv = LocalDate.now().minusDays(3).toEpochDay()
            )
        )
        vehiculoId = (r as VehiculoRepository.ResultadoGuardado.Guardado).id
    }

    @After
    fun limpiar() = runBlocking {
        contenedor.vehiculoRepository.obtenerPorId(vehiculoId)?.let { contenedor.vehiculoRepository.eliminar(it) }
        gestor.cancelAll()
    }

    private fun ejecutarWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<RevisionVencimientosWorker>(context).build().doWork()
    }

    /** El sistema publica y retira las notificaciones de forma asíncrona: se espera hasta un máximo. */
    private fun esperarHasta(maxMs: Long = 3_000, condicion: () -> Boolean): Boolean {
        val limite = System.currentTimeMillis() + maxMs
        while (!condicion()) {
            if (System.currentTimeMillis() > limite) return false
            Thread.sleep(100)
        }
        return true
    }

    private fun notificacionVisible(id: Int) = gestor.activeNotifications.any { it.id == id }

    private suspend fun recordatorioItv() = contenedor.recordatorioRepository.obtenerTodosConDetalle()
        .first { it.recordatorio.vehiculoId == vehiculoId && it.tipoCodigo == TipoMantenimiento.CODIGO_ITV }

    @Test
    fun itvVencida_seNotificaUnaSolaVez() = runBlocking {
        assertEquals(ListenableWorker.Result.success(), ejecutarWorker())
        val recordatorio = recordatorioItv().recordatorio
        assertEquals(EvaluadorAvisos.NIVEL_VENCIDO, recordatorio.avisado)
        val idNotificacion = 1_000 + recordatorio.id.toInt()
        assertTrue(gestor.activeNotifications.any { it.id == idNotificacion })

        // Segunda revisión: el nivel ya está avisado y no se vuelve a publicar.
        gestor.cancel(idNotificacion)
        assertEquals(ListenableWorker.Result.success(), ejecutarWorker())
        assertTrue(gestor.activeNotifications.none { it.id == idNotificacion })
        assertEquals(EvaluadorAvisos.NIVEL_VENCIDO, recordatorioItv().recordatorio.avisado)
    }

    @Test
    fun nuevaFechaItv_reiniciaElAvisoYDejaElEstadoAlDia() = runBlocking {
        ejecutarWorker()
        val idNotificacion = 1_000 + recordatorioItv().recordatorio.id.toInt()
        assertTrue(esperarHasta { notificacionVisible(idNotificacion) })
        val v = contenedor.vehiculoRepository.obtenerPorId(vehiculoId)!!
        contenedor.vehiculoRepository.guardar(v.copy(fechaItv = LocalDate.now().plusYears(1).toEpochDay()))
        assertEquals(EvaluadorAvisos.NIVEL_NINGUNO, recordatorioItv().recordatorio.avisado)
        ejecutarWorker()
        assertEquals(EvaluadorAvisos.NIVEL_NINGUNO, recordatorioItv().recordatorio.avisado)
        // El aviso de la ITV vencida se retira al volver a estar al día.
        assertTrue(esperarHasta { !notificacionVisible(idNotificacion) })
    }

    @Test
    fun eliminarVehiculo_retiraSusNotificaciones() = runBlocking {
        ejecutarWorker()
        val idNotificacion = 1_000 + recordatorioItv().recordatorio.id.toInt()
        assertTrue(esperarHasta { notificacionVisible(idNotificacion) })
        contenedor.vehiculoRepository.eliminar(contenedor.vehiculoRepository.obtenerPorId(vehiculoId)!!)
        ejecutarWorker()
        assertTrue(esperarHasta { !notificacionVisible(idNotificacion) })
    }
}
