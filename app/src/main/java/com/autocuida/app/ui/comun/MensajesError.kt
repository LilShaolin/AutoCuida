package com.autocuida.app.ui.comun

import android.content.Context
import com.autocuida.app.R
import com.autocuida.app.domain.validacion.ErrorValidacion
import com.autocuida.app.util.Fechas
import com.autocuida.app.util.Formatos

/**
 * Traduce un error de validación del dominio a un texto de strings.xml.
 *
 * @param kmReferencia kilómetros a mostrar en los errores de coherencia del historial.
 */
fun ErrorValidacion.mensaje(context: Context, kmReferencia: Int? = null): String {
    val km = kmReferencia?.let { Formatos.km(it) } ?: ""
    return when (this) {
        ErrorValidacion.OBLIGATORIO -> context.getString(R.string.error_obligatorio)
        ErrorValidacion.LONGITUD_EXCEDIDA -> context.getString(R.string.error_longitud)
        ErrorValidacion.MATRICULA_FORMATO -> context.getString(R.string.error_matricula_formato)
        ErrorValidacion.MATRICULA_DUPLICADA -> context.getString(R.string.error_matricula_duplicada)
        ErrorValidacion.ANIO_FUERA_DE_RANGO -> context.getString(R.string.error_anio_rango, Fechas.hoy().year + 1)
        ErrorValidacion.NUMERO_NO_VALIDO -> context.getString(R.string.error_numero)
        ErrorValidacion.KM_NEGATIVOS -> context.getString(R.string.error_km_negativos)
        ErrorValidacion.KM_EXCESIVOS -> context.getString(R.string.error_km_excesivos)
        ErrorValidacion.KM_INFERIORES_A_ANTERIOR -> context.getString(R.string.error_km_inferiores_anterior, km)
        ErrorValidacion.KM_SUPERIORES_A_POSTERIOR -> context.getString(R.string.error_km_superiores_posterior, km)
        ErrorValidacion.KM_INFERIORES_A_HISTORIAL -> context.getString(R.string.error_km_inferiores_historial, km)
        ErrorValidacion.KM_RETROCESO -> context.getString(R.string.error_km_retroceso)
        ErrorValidacion.COSTE_NEGATIVO -> context.getString(R.string.error_coste_negativo)
        ErrorValidacion.COSTE_EXCESIVO -> context.getString(R.string.error_coste_excesivo)
        ErrorValidacion.FECHA_FUTURA -> context.getString(R.string.error_fecha_futura)
        ErrorValidacion.FECHA_ITV_ANTERIOR -> context.getString(R.string.error_fecha_itv_anterior)
        ErrorValidacion.INTERVALO_REQUERIDO -> context.getString(R.string.error_intervalo_requerido)
        ErrorValidacion.INTERVALO_NO_POSITIVO -> context.getString(R.string.error_intervalo_positivo)
        ErrorValidacion.INTERVALO_EXCESIVO -> context.getString(R.string.error_intervalo_excesivo)
        ErrorValidacion.NOMBRE_DUPLICADO -> context.getString(R.string.error_nombre_duplicado)
    }
}
