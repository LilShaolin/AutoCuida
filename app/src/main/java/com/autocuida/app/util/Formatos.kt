package com.autocuida.app.util

import java.text.NumberFormat
import java.util.Locale

/** Formato de cantidades con convenciones españolas (separador de miles y euros). */
object Formatos {

    private val LOCALE_ES: Locale = Locale.forLanguageTag("es-ES")

    fun km(valor: Int): String = NumberFormat.getIntegerInstance(LOCALE_ES).apply {
        isGroupingUsed = true
    }.format(valor) + " km"

    fun numero(valor: Int): String = NumberFormat.getIntegerInstance(LOCALE_ES).apply {
        isGroupingUsed = true
    }.format(valor)

    fun euros(valor: Double): String = NumberFormat.getCurrencyInstance(LOCALE_ES).format(valor)

    /** Coste para un campo de texto editable: "45,90" o vacío si es 0. */
    fun costeEditable(valor: Double): String =
        if (valor == 0.0) "" else String.format(LOCALE_ES, "%.2f", valor)
}
