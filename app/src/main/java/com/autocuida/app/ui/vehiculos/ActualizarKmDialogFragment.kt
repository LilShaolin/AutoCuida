package com.autocuida.app.ui.vehiculos

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
import com.autocuida.app.databinding.DialogActualizarKmBinding
import com.autocuida.app.domain.validacion.ErrorValidacion
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.ui.comun.mensaje
import com.autocuida.app.ui.comun.mostrarMensaje
import com.autocuida.app.ui.comun.recolectarEnDialogo
import com.autocuida.app.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Diálogo para actualizar el cuentakilómetros de un vehículo. */
class ActualizarKmDialogFragment : DialogFragment() {

    private var _binding: DialogActualizarKmBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActualizarKmViewModel by viewModels {
        viewModelFactory {
            initializer {
                val c = (requireActivity().application as AutoCuidaApp).contenedor
                ActualizarKmViewModel(createSavedStateHandle(), c.vehiculoRepository, c.programadorAvisos)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogActualizarKmBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_velocimetro)
            .setTitle(R.string.km_dialogo_titulo)
            .setView(binding.root)
            .setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_actualizar, null)
            .create()
            .also { observar() }
    }

    /** Se llama desde onCreateDialog: el diálogo usa el ciclo de vida del fragmento, no una vista propia. */
    private fun observar() {
        recolectarEnDialogo(viewModel.estado) { estado ->
            estado.vehiculo?.let {
                binding.kmActuales.text = getString(R.string.km_dialogo_actuales, Formatos.km(it.kmActuales))
            }
            val referencia = when (estado.error) {
                ErrorValidacion.KM_INFERIORES_A_HISTORIAL -> estado.kmMaximoHistorial
                else -> null
            }
            binding.tilKm.error = estado.error?.mensaje(requireContext(), referencia)
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
        // Se sustituye el comportamiento por defecto para no cerrar el diálogo si hay errores.
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            viewModel.guardar(binding.etKm.text.toString(), binding.checkCorreccion.isChecked)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
