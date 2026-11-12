package com.autocuida.app.demo

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.autocuida.app.AutoCuidaApp
import com.autocuida.app.MainActivity
import com.autocuida.app.R
import com.autocuida.app.data.entity.Mantenimiento
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.worker.PlanificadorRevisiones
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Herramienta de documentación, separada del código de producción (solo existe en androidTest).
 *
 * Carga un conjunto de vehículos de demostración en el emulador y obtiene capturas reales de las
 * pantallas para los manuales. No se ejecuta en las pruebas normales: requiere el argumento
 * `-e capturas true` al lanzarla con `adb shell am instrument`.
 */
@RunWith(AndroidJUnit4::class)
class CapturasDemostracion {

    @get:Rule
    val permiso: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val instrumentacion get() = InstrumentationRegistry.getInstrumentation()
    private val hoy = LocalDate.now()

    @Before
    fun soloBajoPeticion() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("capturas") == "true")
    }

    private fun shell(comando: String) {
        instrumentacion.uiAutomation.executeShellCommand(comando).close()
    }

    private fun capturar(nombre: String) {
        Thread.sleep(900)
        shell("screencap -p /data/local/tmp/capturas/$nombre.png")
        Thread.sleep(700)
    }

    @Test
    fun cargarDatosYCapturar() {
        shell("mkdir -p /data/local/tmp/capturas")
        val contenedor = (ApplicationProvider.getApplicationContext<Context>() as AutoCuidaApp).contenedor
        val leonId = runBlocking { cargarDatos(contenedor) }

        ActivityScenario.launch(MainActivity::class.java).use {
            // 01. Lista de vehículos
            capturar("01_lista_vehiculos")

            // 02. Formulario de alta
            onView(withId(R.id.fab_anadir)).perform(click())
            onView(withId(R.id.et_marca)).perform(replaceText("Peugeot"))
            onView(withId(R.id.et_modelo)).perform(replaceText("208"))
            onView(withId(R.id.et_matricula)).perform(replaceText("12 ab"))
            onView(withId(R.id.boton_guardar)).perform(scrollTo(), click())
            capturar("02_formulario_vehiculo_validacion")
            pressBack()

            // 03-05. Detalle del vehículo
            onView(allOf(withId(R.id.matricula), withText("4821KLM"))).perform(click())
            capturar("03_detalle_cabecera")
            onView(withId(R.id.lista_vencimientos)).perform(scrollTo())
            capturar("04_detalle_vencimientos")
            onView(withId(R.id.tarjeta_historial)).perform(scrollTo())
            capturar("05_detalle_historial")

            // 06. Nuevo mantenimiento
            onView(withId(R.id.fab_mantenimiento)).perform(click())
            capturar("06_formulario_mantenimiento")
            pressBack()

            // 07. Actualizar kilómetros
            onView(withId(R.id.scroll)).perform(swipeDownCompleto())
            onView(withId(R.id.boton_km)).perform(click())
            capturar("07_actualizar_km")
            onView(withText(R.string.accion_cancelar)).perform(click())

            // 08. Confirmación de borrado
            onView(withId(R.id.accion_eliminar)).perform(click())
            capturar("08_confirmar_eliminacion")
            onView(withText(R.string.accion_cancelar)).perform(click())
            pressBack()

            // 09-10. Gastos
            onView(withId(R.id.gastosFragment)).perform(click())
            capturar("09_gastos_resumen")
            instrumentacion.uiAutomation.executeShellCommand("input swipe 540 1800 540 600 400").close()
            capturar("10_gastos_desglose")
            instrumentacion.uiAutomation.executeShellCommand("input swipe 540 1800 540 400 400").close()
            capturar("11_gastos_historial")

            // 12-13. Tipos
            onView(withId(R.id.tiposFragment)).perform(click())
            capturar("12_tipos")
            onView(withText("Aceite")).perform(click())
            capturar("13_editar_tipo")
            pressBack()
            onView(withId(R.id.listaVehiculosFragment)).perform(click())
        }

        // 14. Notificaciones: revisión inmediata y panel de notificaciones abierto
        PlanificadorRevisiones.revisarAhora(instrumentacion.targetContext)
        Thread.sleep(4_000)
        shell("cmd statusbar expand-notifications")
        capturar("14_notificaciones")
        shell("cmd statusbar collapse")

        // 15. Orientación horizontal
        shell("settings put system accelerometer_rotation 0")
        shell("settings put system user_rotation 1")
        ActivityScenario.launch(MainActivity::class.java).use {
            capturar("15_lista_horizontal")
        }
        shell("settings put system user_rotation 0")

        // 16. Tema oscuro
        shell("cmd uimode night yes")
        ActivityScenario.launch(MainActivity::class.java).use {
            capturar("16_lista_tema_oscuro")
        }
        shell("cmd uimode night no")
        check(leonId > 0)
    }

    private fun swipeDownCompleto() = androidx.test.espresso.action.ViewActions.swipeDown()

    /** Vehículos y mantenimientos de ejemplo con fechas relativas al día de la ejecución. */
    private suspend fun cargarDatos(c: com.autocuida.app.ContenedorDependencias): Long {
        val tipos = c.tipoRepository.obtenerTodos().associateBy { it.codigo }
        fun tipo(codigo: String) = tipos.getValue(codigo).id

        suspend fun vehiculo(v: Vehiculo): Long =
            (c.vehiculoRepository.guardar(v) as VehiculoRepository.ResultadoGuardado.Guardado).id

        suspend fun mant(vId: Long, codigo: String, haceDias: Long, km: Int, coste: Double, taller: String?, notas: String? = null,
                         proximaItv: LocalDate? = null) {
            c.mantenimientoRepository.guardar(
                Mantenimiento(vehiculoId = vId, tipoId = tipo(codigo), fecha = hoy.minusDays(haceDias).toEpochDay(),
                    km = km, coste = coste, taller = taller, notas = notas),
                nuevaFechaItv = proximaItv?.toEpochDay()
            )
        }

        val leon = vehiculo(Vehiculo(marca = "Seat", modelo = "León 1.5 TSI", matricula = "4821KLM", anio = 2019,
            combustible = "Gasolina", kmActuales = 70_000, fechaItv = hoy.plusDays(126).toEpochDay()))
        mant(leon, TipoMantenimiento.CODIGO_ACEITE, 730, 58_000, 79.90, "Talleres Ruiz", "Aceite 5W30 y filtro")
        mant(leon, TipoMantenimiento.CODIGO_FILTROS, 768, 57_800, 64.00, "Talleres Ruiz", "Aire y habitáculo")
        mant(leon, TipoMantenimiento.CODIGO_ITV, 604, 61_000, 45.50, "ITV Norte", proximaItv = hoy.plusDays(126))
        mant(leon, TipoMantenimiento.CODIGO_FRENOS, 516, 66_000, 210.00, "Frenos Express", "Pastillas delanteras")
        mant(leon, TipoMantenimiento.CODIGO_ACEITE, 357, 72_400, 85.00, "Talleres Ruiz")
        mant(leon, TipoMantenimiento.CODIGO_NEUMATICOS, 216, 82_000, 389.60, "Neumáticos Sur", "Cuatro neumáticos 205/55 R16")
        c.vehiculoRepository.actualizarKm(leon, 86_300)

        val corolla = vehiculo(Vehiculo(marca = "Toyota", modelo = "Corolla Hybrid", matricula = "2156MNB", anio = 2022,
            combustible = "Híbrido", kmActuales = 41_250, fechaItv = hoy.plusDays(18).toEpochDay()))
        mant(corolla, TipoMantenimiento.CODIGO_NEUMATICOS, 265, 28_000, 340.00, "Neumáticos Sur")
        mant(corolla, TipoMantenimiento.CODIGO_ACEITE, 188, 33_900, 119.00, "Concesionario oficial", "Revisión anual")

        val clio = vehiculo(Vehiculo(marca = "Renault", modelo = "Clio dCi", matricula = "9087HJT", anio = 2015,
            combustible = "Diésel", kmActuales = 158_900, fechaItv = hoy.minusDays(7).toEpochDay()))
        mant(clio, TipoMantenimiento.CODIGO_DISTRIBUCION, 1_168, 120_000, 620.00, "Talleres Ruiz", "Kit de distribución y bomba de agua")
        mant(clio, TipoMantenimiento.CODIGO_ACEITE, 242, 150_000, 95.00, "Talleres Ruiz")
        return leon
    }
}
