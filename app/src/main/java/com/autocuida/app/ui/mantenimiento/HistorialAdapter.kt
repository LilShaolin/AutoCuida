package com.autocuida.app.ui.mantenimiento

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autocuida.app.R
import com.autocuida.app.data.entity.MantenimientoDetalle
import com.autocuida.app.databinding.ItemMantenimientoBinding
import com.autocuida.app.util.Fechas
import com.autocuida.app.util.Formatos

/**
 * Historial de mantenimientos. Se usa en el detalle del vehículo y en la pantalla de gastos.
 *
 * @param mostrarVehiculo añade la matrícula del vehículo, útil cuando se listan varios.
 */
class HistorialAdapter(
    private val mostrarVehiculo: Boolean,
    private val alPulsar: (MantenimientoDetalle) -> Unit
) : ListAdapter<MantenimientoDetalle, HistorialAdapter.MantenimientoViewHolder>(Diferencias) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MantenimientoViewHolder(
        ItemMantenimientoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: MantenimientoViewHolder, position: Int) = holder.mostrar(getItem(position))

    inner class MantenimientoViewHolder(private val b: ItemMantenimientoBinding) : RecyclerView.ViewHolder(b.root) {

        fun mostrar(item: MantenimientoDetalle) {
            val m = item.mantenimiento
            val contexto = b.root.context
            b.tipo.text = item.tipoNombre
            b.coste.text = if (m.coste == 0.0) contexto.getString(R.string.historial_coste_cero) else Formatos.euros(m.coste)
            b.fechaKm.text = contexto.getString(R.string.historial_km_taller, Fechas.formatear(m.fecha), Formatos.km(m.km))

            val partes = buildList {
                if (mostrarVehiculo) add(item.vehiculoMatricula)
                m.taller?.takeIf { it.isNotBlank() }?.let { add(it) }
                m.notas?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            b.detalle.text = partes.joinToString(contexto.getString(R.string.venc_separador))
            b.detalle.isVisible = partes.isNotEmpty()
            b.root.setOnClickListener { alPulsar(item) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<MantenimientoDetalle>() {
        override fun areItemsTheSame(a: MantenimientoDetalle, b: MantenimientoDetalle) =
            a.mantenimiento.id == b.mantenimiento.id
        override fun areContentsTheSame(a: MantenimientoDetalle, b: MantenimientoDetalle) = a == b
    }
}
