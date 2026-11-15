package com.autocuida.app.ui.tipos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.autocuida.app.R
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.databinding.FragmentTiposBinding
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.ui.comun.contenedor
import com.autocuida.app.ui.comun.mostrarMensaje
import com.autocuida.app.ui.comun.navegar
import com.autocuida.app.ui.comun.recolectar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/** Pantalla 6: tipos de mantenimiento e intervalos configurables. */
class TiposFragment : Fragment() {

    private var _binding: FragmentTiposBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TiposViewModel by viewModels {
        viewModelFactory { initializer { TiposViewModel(contenedor.tipoRepository) } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTiposBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adaptador = TiposAdapter(alEditar = { abrirEditor(it.id) }, alEliminar = ::confirmarEliminacion)
        binding.lista.adapter = adaptador
        binding.fabAnadir.setOnClickListener { abrirEditor(0) }
        binding.scroll.setOnScrollChangeListener { _: View, _: Int, y: Int, _: Int, yAnterior: Int ->
            if (y > yAnterior) binding.fabAnadir.shrink() else if (y < yAnterior) binding.fabAnadir.extend()
        }

        recolectar(viewModel.tipos) { adaptador.submitList(it) }
        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is EventoUi.Eliminado -> mostrarMensaje(evento.texto)
                is EventoUi.Mensaje -> {
                    val texto = getString(evento.texto, *evento.args.toTypedArray())
                    Snackbar.make(binding.root, texto, Snackbar.LENGTH_LONG).show()
                }
                is EventoUi.Guardado -> Unit
            }
        }
    }

    private fun abrirEditor(tipoId: Long) {
        navegar(R.id.action_tipos_to_editarTipo, bundleOf(EditarTipoDialogFragment.ARG_TIPO_ID to tipoId))
    }

    private fun confirmarEliminacion(tipo: TipoMantenimiento) {
        if (tipo.esItv) {
            viewModel.eliminar(tipo) // El repositorio responde que está protegido y se informa al usuario.
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_delete)
            .setTitle(R.string.eliminar_tipo_titulo)
            .setMessage(getString(R.string.eliminar_tipo_mensaje, tipo.nombre))
            .setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_eliminar) { _, _ -> viewModel.eliminar(tipo) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
