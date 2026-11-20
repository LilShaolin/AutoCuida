package com.autocuida.app.ui.vehiculos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autocuida.app.R
import com.autocuida.app.databinding.ItemResumenVehiculosBinding
import com.autocuida.app.databinding.ItemVehiculoBinding
import com.autocuida.app.domain.EstadoMantenimiento
import com.autocuida.app.ui.comun.EstadoUi
import com.autocuida.app.ui.comun.mostrarIconoEstado
import com.autocuida.app.ui.comun.mostrarInsignia
import com.autocuida.app.util.Fechas
import com.autocuida.app.util.Formatos
import com.autocuida.app.util.TextosVencimiento

/** Tarjetas de la lista de vehículos. */
class VehiculosAdapter(
    private val alPulsar: (Long) -> Unit
) : ListAdapter<ResumenVehiculo, VehiculosAdapter.VehiculoViewHolder>(Diferencias) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VehiculoViewHolder(
        ItemVehiculoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VehiculoViewHolder, position: Int) = holder.mostrar(getItem(position))

    inner class VehiculoViewHolder(private val b: ItemVehiculoBinding) : RecyclerView.ViewHolder(b.root) {

        fun mostrar(resumen: ResumenVehiculo) {
            val contexto = b.root.context
            val v = resumen.vehiculo
            b.titulo.text = contexto.getString(R.string.vehiculo_nombre, v.marca, v.modelo)
            b.matricula.text = v.matricula
            b.subtitulo.text = contexto.getString(R.string.vehiculo_subtitulo, v.anio, v.combustible)
            b.km.text = Formatos.km(v.kmActuales)
            b.itv.text = contexto.getString(R.string.vehiculo_itv_fecha, Fechas.formatear(v.fechaItv))
            // La fecha de ITV se resalta solo cuando está próxima o vencida.
            val colorItv = EstadoUi.de(resumen.itv.estado).let { if (it == EstadoUi.VERDE) R.color.ac_on_surface else it.color }
            b.itv.setTextColor(ContextCompat.getColor(contexto, colorItv))
            b.insignia.mostrarInsignia(
                resumen.estado,
                if (resumen.estado == EstadoMantenimiento.SIN_REFERENCIA) R.string.estado_sin_datos else null
            )

            val urgente = resumen.masUrgente
            val avisoMostrado = when {
                resumen.itv.estado > (urgente?.evaluacion?.estado ?: EstadoMantenimiento.SIN_REFERENCIA) ->
                    contexto.getString(R.string.detalle_itv) to resumen.itv
                urgente != null -> urgente.tipoNombre to urgente.evaluacion
                else -> null
            }
            if (avisoMostrado == null) {
                b.aviso.text = contexto.getString(R.string.vehiculo_sin_avisos)
                b.avisoIcono.mostrarIconoEstado(EstadoMantenimiento.SIN_REFERENCIA)
            } else {
                b.aviso.text = contexto.getString(
                    R.string.vehiculo_proximo_aviso,
                    avisoMostrado.first,
                    TextosVencimiento.restantePrincipal(contexto, avisoMostrado.second)
                )
                b.avisoIcono.mostrarIconoEstado(avisoMostrado.second.estado)
            }
            b.avisoIcono.isVisible = true
            b.tarjeta.setOnClickListener { alPulsar(v.id) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<ResumenVehiculo>() {
        override fun areItemsTheSame(a: ResumenVehiculo, b: ResumenVehiculo) = a.vehiculo.id == b.vehiculo.id
        override fun areContentsTheSame(a: ResumenVehiculo, b: ResumenVehiculo) = a == b
    }
}

/** Cabecera con el número de vehículos y de avisos pendientes. */
class CabeceraVehiculosAdapter : RecyclerView.Adapter<CabeceraVehiculosAdapter.CabeceraViewHolder>() {

    private var vehiculos = 0
    private var avisos = 0

    fun actualizar(vehiculos: Int, avisos: Int) {
        val visibleAntes = this.vehiculos > 0
        this.vehiculos = vehiculos
        this.avisos = avisos
        when {
            visibleAntes && vehiculos == 0 -> notifyItemRemoved(0)
            !visibleAntes && vehiculos > 0 -> notifyItemInserted(0)
            vehiculos > 0 -> notifyItemChanged(0)
        }
    }

    override fun getItemCount() = if (vehiculos > 0) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CabeceraViewHolder(
        ItemResumenVehiculosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: CabeceraViewHolder, position: Int) {
        val r = holder.itemView.resources
        holder.b.resumenVehiculos.text = r.getQuantityString(R.plurals.resumen_vehiculos, vehiculos, vehiculos)
        // El texto final (recuento) es también el que leen los lectores de pantalla.
        if (avisos > 0) {
            holder.b.resumenAvisos.mostrarInsignia(
                EstadoMantenimiento.PROXIMO, textoLibre = r.getQuantityString(R.plurals.resumen_avisos, avisos, avisos)
            )
        } else {
            holder.b.resumenAvisos.mostrarInsignia(EstadoMantenimiento.AL_DIA, textoLibre = r.getString(R.string.resumen_sin_avisos))
        }
    }

    class CabeceraViewHolder(val b: ItemResumenVehiculosBinding) : RecyclerView.ViewHolder(b.root)
}
