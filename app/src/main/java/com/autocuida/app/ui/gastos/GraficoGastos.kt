package com.autocuida.app.ui.gastos

import android.graphics.Typeface
import com.autocuida.app.R
import com.autocuida.app.data.entity.GastoPorPeriodo
import com.autocuida.app.domain.PeriodoGastos
import com.autocuida.app.util.Formatos
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import kotlin.math.roundToInt

/** Configuración y datos de la gráfica de barras de gastos (MPAndroidChart). */
object GraficoGastos {

    fun configurar(grafica: BarChart) {
        val colorTexto = MaterialColors.getColor(grafica, com.google.android.material.R.attr.colorOnSurfaceVariant)
        val colorRejilla = MaterialColors.getColor(grafica, com.google.android.material.R.attr.colorOutlineVariant)
        grafica.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            isDoubleTapToZoomEnabled = false
            setFitBars(true)
            setNoDataText("")
            axisRight.isEnabled = false
            extraBottomOffset = 4f
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = colorTexto
                axisLineColor = colorRejilla
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = colorTexto
                gridColor = colorRejilla
                setDrawAxisLine(false)
                setLabelCount(4, false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = "${Formatos.numero(value.roundToInt())} €"
                }
            }
        }
    }

    /** Muestra 12 meses para un año o un año por barra para todo el historial. */
    fun mostrar(grafica: BarChart, serie: List<GastoPorPeriodo>, periodo: PeriodoGastos) {
        val porPeriodo = serie.associate { it.periodo to it.total }
        val claves: List<Int> = when (periodo) {
            is PeriodoGastos.Anio -> (1..12).toList()
            PeriodoGastos.Todo -> if (serie.isEmpty()) emptyList() else (serie.minOf { it.periodo }..serie.maxOf { it.periodo }).toList()
        }
        val etiquetas: List<String> = when (periodo) {
            is PeriodoGastos.Anio -> grafica.resources.getStringArray(R.array.meses_cortos).toList()
            PeriodoGastos.Todo -> claves.map { it.toString() }
        }
        val entradas = claves.mapIndexed { i, clave -> BarEntry(i.toFloat(), (porPeriodo[clave] ?: 0.0).toFloat()) }

        val colorBarra = MaterialColors.getColor(grafica, androidx.appcompat.R.attr.colorPrimary)
        val colorTexto = MaterialColors.getColor(grafica, com.google.android.material.R.attr.colorOnSurface)
        val conjunto = BarDataSet(entradas, "").apply {
            color = colorBarra
            valueTextColor = colorTexto
            valueTextSize = 9f
            valueTypeface = Typeface.DEFAULT_BOLD
            isHighlightEnabled = false
            valueFormatter = object : ValueFormatter() {
                // Solo se rotulan las barras con gasto para no saturar la gráfica.
                override fun getFormattedValue(value: Float) = if (value > 0f) Formatos.numero(value.roundToInt()) else ""
            }
        }
        grafica.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float) = etiquetas.getOrNull(value.roundToInt()) ?: ""
        }
        grafica.xAxis.labelCount = etiquetas.size.coerceAtMost(12)
        grafica.data = BarData(conjunto).apply { barWidth = 0.6f }
        grafica.animateY(400)
        grafica.invalidate()
    }
}
