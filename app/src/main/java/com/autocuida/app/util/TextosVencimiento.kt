package com.autocuida.app.util

import android.content.Context
import com.autocuida.app.R
import com.autocuida.app.domain.CriterioVencimiento
import com.autocuida.app.domain.EvaluacionVencimiento
import kotlin.math.abs

/** Textos legibles de un vencimiento, compartidos por las pantallas y las notificaciones. */
object TextosVencimiento {

    /** "Próximo: 45.000 km o 12/05/2027". */
    fun limites(context: Context, evaluacion: EvaluacionVencimiento): String {
        val proximo = evaluacion.proximo ?: return context.getString(R.string.venc_sin_referencia)
        val km = proximo.kmLimite?.let { Formatos.km(it) }
        val fecha = proximo.fechaLimite?.let { Fechas.formatear(it) }
        return when {
            km != null && fecha != null -> context.getString(R.string.venc_proximo_km_fecha, km, fecha)
            km != null -> context.getString(R.string.venc_proximo_km, km)
            else -> context.getString(R.string.venc_proximo_fecha, fecha)
        }
    }

    /** "faltan 800 km · faltan 45 días", con el criterio determinante en primer lugar. */
    fun restante(context: Context, evaluacion: EvaluacionVencimiento): String {
        val textoKm = evaluacion.kmRestantes?.let { textoKm(context, it) }
        val textoDias = evaluacion.diasRestantes?.let { textoDias(context, it) }
        val partes = if (evaluacion.criterio == CriterioVencimiento.FECHA) listOf(textoDias, textoKm)
        else listOf(textoKm, textoDias)
        return partes.filterNotNull().joinToString(context.getString(R.string.venc_separador))
            .replaceFirstChar { it.uppercase() }
    }

    /** Solo el texto del criterio determinante, para resúmenes breves. */
    fun restantePrincipal(context: Context, evaluacion: EvaluacionVencimiento): String {
        val texto = when (evaluacion.criterio) {
            CriterioVencimiento.KILOMETROS -> evaluacion.kmRestantes?.let { textoKm(context, it) }
            CriterioVencimiento.FECHA -> evaluacion.diasRestantes?.let { textoDias(context, it) }
            null -> null
        }
        return texto ?: context.getString(R.string.venc_sin_referencia)
    }

    private fun textoKm(context: Context, km: Int): String =
        if (km >= 0) context.getString(R.string.venc_faltan_km, Formatos.km(km))
        else context.getString(R.string.venc_superado_km, Formatos.km(abs(km)))

    private fun textoDias(context: Context, dias: Long): String {
        val n = abs(dias).toInt()
        return when {
            dias == 0L -> context.getString(R.string.venc_vence_hoy)
            dias > 0 -> context.resources.getQuantityString(R.plurals.venc_faltan_dias, n, n)
            else -> context.resources.getQuantityString(R.plurals.venc_vencido_dias, n, n)
        }
    }
}
