package com.autocuida.app.ui.comun

import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.autocuida.app.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.autocuida.app.AutoCuidaApp
import com.autocuida.app.ContenedorDependencias
import com.autocuida.app.util.Fechas
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Acceso al contenedor de dependencias de la aplicación. */
val Fragment.contenedor: ContenedorDependencias
    get() = (requireActivity().application as AutoCuidaApp).contenedor

/** Recoge un Flow mientras la vista del fragmento está al menos en estado STARTED. */
fun <T> Fragment.recolectar(flujo: Flow<T>, accion: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flujo.collect { accion(it) }
        }
    }
}

/**
 * Variante para diálogos creados en `onCreateDialog`, que no tienen vista propia del fragmento:
 * se usa el ciclo de vida del propio fragmento.
 */
fun <T> DialogFragment.recolectarEnDialogo(flujo: Flow<T>, accion: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flujo.collect { accion(it) }
        }
    }
}

/**
 * Muestra un mensaje breve sobre la actividad, de modo que siga visible aunque el fragmento
 * navegue hacia atrás justo después (por ejemplo, tras guardar un formulario).
 */
fun Fragment.mostrarMensaje(@StringRes texto: Int, larga: Boolean = false) {
    val raiz = requireActivity().findViewById<View>(android.R.id.content)
    val snackbar = Snackbar.make(raiz, texto, if (larga) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
    requireActivity().findViewById<View>(R.id.navegacion)
        ?.takeIf { it.isShown && it is BottomNavigationView }
        ?.let { snackbar.anchorView = it }
    snackbar.show()
}

/**
 * Muestra un selector de fecha de Material y devuelve el día epoch elegido.
 *
 * @param soloPasado si es `true`, no permite elegir fechas posteriores a hoy.
 */
fun Fragment.seleccionarFecha(
    titulo: CharSequence,
    epochDayInicial: Long?,
    soloPasado: Boolean = false,
    alSeleccionar: (Long) -> Unit
) {
    val restricciones = CalendarConstraints.Builder().apply {
        if (soloPasado) {
            setValidator(DateValidatorPointBackward.before(Fechas.epochDayAUtcMillis(Fechas.hoyEpochDay() + 1)))
            setEnd(Fechas.epochDayAUtcMillis(Fechas.hoyEpochDay()))
        }
    }.build()
    val selector = MaterialDatePicker.Builder.datePicker()
        .setTitleText(titulo)
        .setSelection(Fechas.epochDayAUtcMillis(epochDayInicial ?: Fechas.hoyEpochDay()))
        .setCalendarConstraints(restricciones)
        .build()
    selector.addOnPositiveButtonClickListener { millis -> alSeleccionar(Fechas.utcMillisAEpochDay(millis)) }
    selector.show(childFragmentManager, "selector_fecha")
}
