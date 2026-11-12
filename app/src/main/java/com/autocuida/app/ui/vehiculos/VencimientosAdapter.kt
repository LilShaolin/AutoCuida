package com.autocuida.app.ui.vehiculos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autocuida.app.databinding.ItemVencimientoBinding
import com.autocuida.app.domain.EstadoMantenimiento
import com.autocuida.app.ui.comun.mostrarIconoEstado
import com.autocuida.app.ui.comun.mostrarInsignia
import com.autocuida.app.util.TextosVencimiento

/**
 * Próximos vencimientos de un vehículo. Los tipos sin historial muestran el estado
 * «Sin referencia» y un acceso para registrar el último mantenimiento conocido.
 */
class VencimientosAdapter(
    private val alRegistrarUltimo: (tipoId: Long) -> Unit
) : ListAdapter<VencimientoEvaluado, VencimientosAdapter.VencimientoViewHolder>(Diferencias) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VencimientoViewHolder(
        ItemVencimientoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VencimientoViewHolder, position: Int) = holder.mostrar(getItem(position))

    inner class VencimientoViewHolder(private val b: ItemVencimientoBinding) : RecyclerView.ViewHolder(b.root) {

        fun mostrar(item: VencimientoEvaluado) {
            val contexto = b.root.context
            val evaluacion = item.evaluacion
            val sinReferencia = evaluacion.estado == EstadoMantenimiento.SIN_REFERENCIA
            b.nombre.text = item.tipoNombre
            b.icono.mostrarIconoEstado(evaluacion.estado)
            b.insignia.mostrarInsignia(evaluacion.estado)
            b.restante.isVisible = !sinReferencia
            b.restante.text = if (sinReferencia) "" else TextosVencimiento.restante(contexto, evaluacion)
            b.limites.text = TextosVencimiento.limites(contexto, evaluacion)
            b.botonRegistrar.isVisible = sinReferencia
            b.botonRegistrar.setOnClickListener { alRegistrarUltimo(item.tipoId) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<VencimientoEvaluado>() {
        override fun areItemsTheSame(a: VencimientoEvaluado, b: VencimientoEvaluado) = a.tipoId == b.tipoId
        override fun areContentsTheSame(a: VencimientoEvaluado, b: VencimientoEvaluado) = a == b
    }
}
