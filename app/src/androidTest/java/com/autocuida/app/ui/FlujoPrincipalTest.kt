package com.autocuida.app.ui

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.autocuida.app.MainActivity
import com.autocuida.app.R
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Prueba de interfaz del recorrido principal: alta de vehículo, detalle, registro de
 * mantenimiento, actualización de kilómetros y eliminación. Usa una matrícula única y
 * elimina el vehículo al final para no dejar datos en el dispositivo.
 */
@RunWith(AndroidJUnit4::class)
class FlujoPrincipalTest {

    private val permiso: GrantPermissionRule =
        if (android.os.Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    private val actividad = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val reglas: RuleChain = RuleChain.outerRule(permiso).around(actividad)

    private val matricula = "T" + (System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')

    @Test
    fun registrarVehiculoMantenimientoYKilometros() {
        // 1. Alta de vehículo
        onView(withId(R.id.fab_anadir)).perform(click())
        onView(withId(R.id.et_marca)).perform(replaceText("Prueba"))
        onView(withId(R.id.et_modelo)).perform(replaceText("Espresso"))
        onView(withId(R.id.et_matricula)).perform(replaceText(matricula))
        onView(withId(R.id.et_anio)).perform(replaceText("2020"))
        onView(withId(R.id.et_combustible)).perform(replaceText("Gasolina"))
        onView(withId(R.id.et_km)).perform(replaceText("30000"), closeSoftKeyboard())
        onView(withId(R.id.et_fecha_itv)).perform(scrollTo(), click())
        onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
        onView(withId(R.id.boton_guardar)).perform(scrollTo(), click())

        // 2. La tarjeta aparece en la lista y se abre el detalle
        esperar { onView(allOf(withId(R.id.matricula), withText(matricula))).perform(click()) }
        esperar { onView(withId(R.id.km)).check(matches(withText("30.000 km"))) }

        // 3. Registro de mantenimiento (tipo Aceite, km por defecto = actuales)
        onView(withId(R.id.fab_mantenimiento)).perform(click())
        onView(withId(R.id.et_tipo)).perform(click())
        onView(withText("Aceite")).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click())
        onView(withId(R.id.et_coste)).perform(replaceText("75,50"), closeSoftKeyboard())
        onView(withId(R.id.boton_guardar)).perform(scrollTo(), click())

        // 4. El historial muestra el registro
        esperar {
            onView(withId(R.id.lista_historial)).perform(scrollTo())
            onView(withId(R.id.lista_historial)).check(matches(hasDescendant(withText(com.autocuida.app.util.Formatos.euros(75.5)))))
        }

        // 5. Actualización de kilómetros
        onView(withId(R.id.boton_km)).perform(scrollTo(), click())
        onView(withId(R.id.et_km)).inRoot(isDialog()).perform(replaceText("44500"), closeSoftKeyboard())
        onView(withText(R.string.accion_actualizar)).inRoot(isDialog()).perform(click())
        esperar { onView(withId(R.id.km)).perform(scrollTo()).check(matches(withText("44.500 km"))) }

        // 6. Con 44.500 km y límite 45.000 km, el aceite pasa a «Próximo»
        esperar {
            onView(withId(R.id.lista_vencimientos)).perform(scrollTo())
            onView(withId(R.id.lista_vencimientos))
                .check(matches(hasDescendant(allOf(withId(R.id.insignia), withText(R.string.estado_proximo), isDisplayed()))))
        }

        // 7. Eliminación con confirmación
        onView(withId(R.id.accion_eliminar)).perform(click())
        onView(withText(R.string.accion_eliminar)).inRoot(isDialog()).perform(click())
        esperar { onView(withId(R.id.fab_anadir)).check(matches(isDisplayed())) }
    }

    /** Guarda una captura de diagnóstico en el almacenamiento de la app si una comprobación falla. */
    private fun capturarPantalla() {
        val inst = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        inst.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/fallo_flujo.png").close()
        Thread.sleep(1_500)
    }

    /** Reintenta una comprobación mientras Room completa sus operaciones en segundo plano. */
    private fun esperar(maxMs: Long = 5_000, accion: () -> Unit) {
        val limite = System.currentTimeMillis() + maxMs
        while (true) {
            try {
                accion(); return
            } catch (e: Throwable) {
                if (System.currentTimeMillis() > limite) {
                    capturarPantalla()
                    throw e
                }
                Thread.sleep(150)
            }
        }
    }
}
