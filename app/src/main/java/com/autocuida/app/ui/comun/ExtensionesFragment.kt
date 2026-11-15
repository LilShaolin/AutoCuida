package com.autocuida.app.ui.comun

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.autocuida.app.R
import com.google.android.material.navigationrail.NavigationRailView
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
    val actividad = requireActivity()
    val raiz = actividad.findViewById<View>(android.R.id.content)
    val snackbar = Snackbar.make(raiz, texto, if (larga) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
    // Se espera a que termine una posible navegación (doble post) para anclar el mensaje encima
    // del botón flotante o de la barra inferior de la pantalla que quede visible, sin taparlos.
    raiz.post {
        raiz.post {
            val anclaje = ANCLAJES_SNACKBAR.asSequence()
                .mapNotNull { actividad.findViewById<View>(it) }
                .firstOrNull { it.isShown && it !is NavigationRailView }
            snackbar.anchorView = anclaje
            snackbar.show()
        }
    }
}

/** Vistas sobre las que se coloca un Snackbar, por orden de preferencia. */
private val ANCLAJES_SNACKBAR = listOf(R.id.fab_anadir, R.id.fab_mantenimiento, R.id.navegacion)

/**
 * Navega con una acción solo si existe desde el destino actual. Evita el cierre de la aplicación
 * cuando una doble pulsación intenta navegar de nuevo desde el destino al que ya se ha llegado.
 */
fun Fragment.navegar(@IdRes accion: Int, argumentos: Bundle? = null) {
    val nav = findNavController()
    if (nav.currentDestination?.getAction(accion) != null) nav.navigate(accion, argumentos)
}

/**
 * Muestra un selector de fecha de Material y devuelve el día epoch elegido.
 *
 * @param etiqueta identifica el selector de cada campo; se usa en [reengancharSelectorFecha].
 * @param soloPasado si es `true`, no permite elegir fechas posteriores a hoy.
 */
fun Fragment.seleccionarFecha(
    etiqueta: String,
    titulo: CharSequence,
    epochDayInicial: Long?,
    soloPasado: Boolean = false,
    alSeleccionar: (Long) -> Unit
) {
    if (childFragmentManager.findFragmentByTag(etiqueta) != null) return
    val restricciones = CalendarConstraints.Builder().apply {
        if (soloPasado) {
            // El validador admite las fechas anteriores o iguales al punto indicado: hoy es el último día válido.
            setValidator(DateValidatorPointBackward.before(Fechas.epochDayAUtcMillis(Fechas.hoyEpochDay())))
            setEnd(Fechas.epochDayAUtcMillis(Fechas.hoyEpochDay()))
        }
    }.build()
    val selector = MaterialDatePicker.Builder.datePicker()
        .setTitleText(titulo)
        .setSelection(Fechas.epochDayAUtcMillis(epochDayInicial ?: Fechas.hoyEpochDay()))
        .setCalendarConstraints(restricciones)
        .build()
    selector.addOnPositiveButtonClickListener { millis -> alSeleccionar(Fechas.utcMillisAEpochDay(millis)) }
    selector.show(childFragmentManager, etiqueta)
}

/**
 * Vuelve a conectar la acción de un selector de fecha que sigue abierto tras recrearse el fragmento
 * (por ejemplo, al girar la pantalla): MaterialDatePicker no conserva sus listeners. Se llama en
 * `onViewCreated` con la misma etiqueta y acción que [seleccionarFecha].
 */
fun Fragment.reengancharSelectorFecha(etiqueta: String, alSeleccionar: (Long) -> Unit) {
    @Suppress("UNCHECKED_CAST")
    val selector = childFragmentManager.findFragmentByTag(etiqueta) as? MaterialDatePicker<Long> ?: return
    selector.clearOnPositiveButtonClickListeners()
    selector.addOnPositiveButtonClickListener { millis -> alSeleccionar(Fechas.utcMillisAEpochDay(millis)) }
}
