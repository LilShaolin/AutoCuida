package com.autocuida.app.ui.comun

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.autocuida.app.R
import com.autocuida.app.domain.EstadoMantenimiento

/**
 * Representación visual de cada estado. El color siempre va acompañado de un icono y de un
 * texto, para que el estado se entienda sin depender de la percepción del color.
 */
enum class EstadoUi(
    @ColorRes val color: Int,
    @ColorRes val contenedor: Int,
    @DrawableRes val icono: Int,
    @StringRes val texto: Int,
    @StringRes val textoGeneral: Int
) {
    VERDE(R.color.estado_verde, R.color.estado_verde_contenedor, R.drawable.ic_check_circle,
        R.string.estado_al_dia, R.string.estado_general_al_dia),
    AMBAR(R.color.estado_ambar, R.color.estado_ambar_contenedor, R.drawable.ic_warning,
        R.string.estado_proximo, R.string.estado_general_proximo),
    ROJO(R.color.estado_rojo, R.color.estado_rojo_contenedor, R.drawable.ic_error,
        R.string.estado_vencido, R.string.estado_general_vencido),
    NEUTRO(R.color.estado_neutro, R.color.estado_neutro_contenedor, R.drawable.ic_help,
        R.string.estado_sin_referencia, R.string.estado_general_sin_referencia);

    companion object {
        fun de(estado: EstadoMantenimiento): EstadoUi = when (estado) {
            EstadoMantenimiento.AL_DIA -> VERDE
            EstadoMantenimiento.PROXIMO -> AMBAR
            EstadoMantenimiento.VENCIDO -> ROJO
            EstadoMantenimiento.SIN_REFERENCIA -> NEUTRO
        }
    }
}

/**
 * Aplica a una insignia el texto, el icono y los colores del estado.
 *
 * @param textoLibre texto ya formado (por ejemplo, un recuento); si se indica, sustituye al del
 * estado y se usa también como descripción para los lectores de pantalla.
 */
fun TextView.mostrarInsignia(
    estado: EstadoMantenimiento,
    @StringRes textoAlternativo: Int? = null,
    textoLibre: CharSequence? = null
) {
    val ui = EstadoUi.de(estado)
    val color = ContextCompat.getColor(context, ui.color)
    text = textoLibre ?: context.getString(textoAlternativo ?: ui.texto)
    setTextColor(color)
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, ui.contenedor))
    val tamano = resources.getDimensionPixelSize(R.dimen.icono_s)
    val icono = ContextCompat.getDrawable(context, ui.icono)?.mutate()?.apply {
        setBounds(0, 0, tamano, tamano)
        setTint(color)
    }
    setCompoundDrawablesRelative(icono, null, null, null)
    contentDescription = textoLibre ?: context.getString(R.string.cd_estado, text)
}

/** Aplica a un icono el símbolo y el color del estado. */
fun ImageView.mostrarIconoEstado(estado: EstadoMantenimiento) {
    val ui = EstadoUi.de(estado)
    setImageResource(ui.icono)
    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, ui.color))
    contentDescription = context.getString(R.string.cd_estado, context.getString(ui.texto))
}
