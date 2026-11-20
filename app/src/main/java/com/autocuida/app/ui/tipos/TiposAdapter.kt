package com.autocuida.app.ui.tipos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autocuida.app.R
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.databinding.ItemTipoBinding
import com.autocuida.app.util.Formatos
import com.google.android.material.button.MaterialButton

class TiposAdapter(
    private val alEditar: (TipoMantenimiento) -> Unit,
    private val alEliminar: (TipoMantenimiento) -> Unit
) : ListAdapter<TiposViewModel.TipoConUso, TiposAdapter.TipoViewHolder>(Diferencias) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TipoViewHolder(
        ItemTipoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: TipoViewHolder, position: Int) = holder.mostrar(getItem(position))

    inner class TipoViewHolder(private val b: ItemTipoBinding) : RecyclerView.ViewHolder(b.root) {

        fun mostrar(item: TiposViewModel.TipoConUso) {
            val tipo = item.tipo
            val contexto = b.root.context
            val r = contexto.resources
            b.nombre.text = tipo.nombre
            b.intervalos.text = textoIntervalos(contexto, tipo)
            b.uso.text = if (item.registros == 0) r.getString(R.string.tipo_sin_registros)
            else r.getQuantityString(R.plurals.tipo_uso, item.registros, item.registros)
            b.icono.setImageResource(if (tipo.esItv) R.drawable.ic_insignia_itv else R.drawable.ic_llave)
            b.notaItv.isVisible = tipo.esItv
            if (tipo.esItv) b.notaItv.setText(R.string.tipo_itv_nota)

            b.botonEditar.contentDescription = contexto.getString(R.string.cd_editar_tipo, tipo.nombre)
            b.botonEditar.setOnClickListener { alEditar(tipo) }
            b.tarjeta.setOnClickListener { alEditar(tipo) }

            // La ITV no se puede eliminar: se muestra un candado en lugar de la papelera.
            (b.botonEliminar as MaterialButton).setIconResource(if (tipo.esItv) R.drawable.ic_candado else R.drawable.ic_delete)
            b.botonEliminar.contentDescription = if (tipo.esItv) contexto.getString(R.string.cd_tipo_protegido)
            else contexto.getString(R.string.cd_eliminar_tipo, tipo.nombre)
            b.botonEliminar.setOnClickListener { alEliminar(tipo) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<TiposViewModel.TipoConUso>() {
        override fun areItemsTheSame(a: TiposViewModel.TipoConUso, b: TiposViewModel.TipoConUso) = a.tipo.id == b.tipo.id
        override fun areContentsTheSame(a: TiposViewModel.TipoConUso, b: TiposViewModel.TipoConUso) = a == b
    }

    companion object {
        fun textoIntervalos(contexto: android.content.Context, tipo: TipoMantenimiento): String {
            // La ITV solo usa los meses aunque conserve un intervalo en km de versiones anteriores.
            val km = tipo.intervaloKm.takeUnless { tipo.esItv }
            val meses = tipo.intervaloMeses
            return when {
                km != null && meses != null -> contexto.resources.getQuantityString(R.plurals.tipo_cada_km_meses, meses, Formatos.km(km), meses)
                km != null -> contexto.getString(R.string.tipo_cada_km, Formatos.km(km))
                meses != null -> contexto.resources.getQuantityString(R.plurals.tipo_cada_meses, meses, meses)
                else -> ""
            }
        }
    }
}
