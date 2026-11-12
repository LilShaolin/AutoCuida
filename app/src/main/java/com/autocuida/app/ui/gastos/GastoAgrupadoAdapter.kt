package com.autocuida.app.ui.gastos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autocuida.app.R
import com.autocuida.app.data.entity.GastoAgrupado
import com.autocuida.app.databinding.ItemGastoAgrupadoBinding
import com.autocuida.app.util.Formatos
import kotlin.math.roundToInt

/** Filas de gasto por tipo o por vehículo, con una barra proporcional al total del periodo. */
class GastoAgrupadoAdapter : ListAdapter<GastoAgrupado, GastoAgrupadoAdapter.GastoViewHolder>(Diferencias) {

    private var total = 0.0

    fun mostrar(lista: List<GastoAgrupado>, totalPeriodo: Double) {
        total = totalPeriodo
        submitList(lista)
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = GastoViewHolder(
        ItemGastoAgrupadoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) = holder.mostrar(getItem(position))

    inner class GastoViewHolder(private val b: ItemGastoAgrupadoBinding) : RecyclerView.ViewHolder(b.root) {
        fun mostrar(item: GastoAgrupado) {
            val r = b.root.resources
            val fraccion = if (total > 0) item.total / total else 0.0
            b.nombre.text = item.nombre
            b.importe.text = Formatos.euros(item.total)
            b.proporcion.setProgressCompat((fraccion * 1000).roundToInt(), false)
            b.detalle.text = r.getString(
                R.string.gastos_porcentaje,
                r.getQuantityString(R.plurals.gastos_registros, item.cantidad, item.cantidad),
                (fraccion * 100).roundToInt()
            )
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<GastoAgrupado>() {
        override fun areItemsTheSame(a: GastoAgrupado, b: GastoAgrupado) = a.id == b.id
        override fun areContentsTheSame(a: GastoAgrupado, b: GastoAgrupado) = a == b
    }
}
