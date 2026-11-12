package com.autocuida.app.ui.tipos

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.autocuida.app.AutoCuidaApp
import com.autocuida.app.R
import com.autocuida.app.databinding.DialogEditarTipoBinding
import com.autocuida.app.domain.validacion.ValidadorTipo
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.ui.comun.mensaje
import com.autocuida.app.ui.comun.mostrarMensaje
import com.autocuida.app.ui.comun.recolectarEnDialogo
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Alta y edición de un tipo de mantenimiento y de sus intervalos. */
class EditarTipoDialogFragment : DialogFragment() {

    companion object {
        const val ARG_TIPO_ID = "tipoId"
    }

    private var _binding: DialogEditarTipoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditarTipoViewModel by viewModels {
        viewModelFactory {
            initializer {
                val c = (requireActivity().application as AutoCuidaApp).contenedor
                EditarTipoViewModel(createSavedStateHandle(), c.tipoRepository, c.programadorAvisos)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditarTipoBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_ajustes)
            .setTitle(if (viewModel.esEdicion) R.string.tipo_dialogo_editar else R.string.tipo_dialogo_nuevo)
            .setView(binding.root)
            .setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_guardar, null)
            .create()
            .also { observar() }
    }

    private fun observar() {
        recolectarEnDialogo(viewModel.estado) { estado ->
            val original = estado.original
            if (original != null && !viewModel.camposRellenados) {
                binding.etNombre.setText(original.nombre)
                binding.etIntervaloKm.setText(original.intervaloKm?.toString() ?: "")
                binding.etIntervaloMeses.setText(original.intervaloMeses?.toString() ?: "")
                viewModel.camposRellenados = true
            }
            val contexto = requireContext()
            binding.tilNombre.error = estado.errores[ValidadorTipo.Campo.NOMBRE]?.mensaje(contexto)
            binding.tilIntervaloKm.error = estado.errores[ValidadorTipo.Campo.INTERVALO_KM]?.mensaje(contexto)
            binding.tilIntervaloMeses.error = estado.errores[ValidadorTipo.Campo.INTERVALO_MESES]?.mensaje(contexto)
        }
        recolectarEnDialogo(viewModel.eventos) { evento ->
            when (evento) {
                is EventoUi.Guardado -> {
                    mostrarMensaje(evento.texto)
                    dismiss()
                }
                is EventoUi.Mensaje -> mostrarMensaje(evento.texto)
                is EventoUi.Eliminado -> Unit
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            viewModel.guardar(
                ValidadorTipo.Entrada(
                    nombre = binding.etNombre.text.toString(),
                    intervaloKm = binding.etIntervaloKm.text.toString(),
                    intervaloMeses = binding.etIntervaloMeses.text.toString()
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
