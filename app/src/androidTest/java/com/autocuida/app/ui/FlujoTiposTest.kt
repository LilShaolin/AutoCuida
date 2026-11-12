package com.autocuida.app.ui

import android.Manifest
import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
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

/** Edición de intervalos desde la pantalla de tipos, con validación y restauración del valor original. */
@RunWith(AndroidJUnit4::class)
class FlujoTiposTest {

    private val permiso: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    @get:Rule
    val reglas: RuleChain = RuleChain.outerRule(permiso).around(ActivityScenarioRule(MainActivity::class.java))

    private fun editarAceite(km: String, meses: String) {
        onView(withText("Aceite")).perform(click())
        onView(withId(R.id.et_intervalo_km)).inRoot(isDialog()).perform(replaceText(km))
        onView(withId(R.id.et_intervalo_meses)).inRoot(isDialog()).perform(replaceText(meses), closeSoftKeyboard())
        onView(withText(R.string.accion_guardar)).inRoot(isDialog()).perform(click())
    }

    @Test
    fun modificarIntervalosDeUnTipo() {
        onView(withId(R.id.tiposFragment)).perform(click())

        // Sin ningún intervalo: el diálogo muestra el error y no se cierra.
        editarAceite("", "")
        onView(allOf(withText(R.string.error_intervalo_requerido), isDescendantOfA(withId(R.id.til_intervalo_km))))
            .inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.accion_cancelar)).inRoot(isDialog()).perform(click())

        // Cambio válido y comprobación en la lista.
        editarAceite("16000", "12")
        esperar { onView(withId(R.id.lista)).check(matches(hasDescendant(allOf(withId(R.id.intervalos), withText("Cada 16.000 km o 12 meses"))))) }

        // Restauración del valor inicial.
        editarAceite("15000", "12")
        esperar { onView(withId(R.id.lista)).check(matches(hasDescendant(allOf(withId(R.id.intervalos), withText("Cada 15.000 km o 12 meses"))))) }
    }

    private fun esperar(maxMs: Long = 5_000, accion: () -> Unit) {
        val limite = System.currentTimeMillis() + maxMs
        while (true) {
            try {
                accion(); return
            } catch (e: Throwable) {
                if (System.currentTimeMillis() > limite) throw e
                Thread.sleep(150)
            }
        }
    }
}
