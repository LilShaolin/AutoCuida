package com.autocuida.app.ui.vehiculos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.autocuida.app.R
import com.autocuida.app.databinding.FragmentFormVehiculoBinding
import com.autocuida.app.domain.validacion.ValidadorVehiculo
import com.autocuida.app.domain.validacion.ValidadorVehiculo.Campo
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.ui.comun.contenedor
import com.autocuida.app.ui.comun.mensaje
import com.autocuida.app.ui.comun.recolectar
import com.autocuida.app.ui.comun.seleccionarFecha
import com.autocuida.app.util.Fechas
import com.autocuida.app.domain.validacion.ErrorValidacion
import com.autocuida.app.ui.comun.mostrarMensaje
import com.google.android.material.textfield.TextInputLayout

/** Pantalla 2: alta y edición de vehículos. */
class FormVehiculoFragment : Fragment() {

    private var _binding: FragmentFormVehiculoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FormVehiculoViewModel by viewModels {
        viewModelFactory {
            initializer {
                FormVehiculoViewModel(createSavedStateHandle(), contenedor.vehiculoRepository, contenedor.programadorAvisos)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFormVehiculoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(
            if (viewModel.esEdicion) R.string.titulo_editar_vehiculo else R.string.titulo_nuevo_vehiculo
        )

        val abrirSelector = View.OnClickListener {
            seleccionarFecha(getString(R.string.campo_fecha_itv), viewModel.estado.value.fechaItv) {
                viewModel.seleccionarFechaItv(it)
            }
        }
        binding.etFechaItv.setOnClickListener(abrirSelector)
        binding.tilFechaItv.setEndIconOnClickListener(abrirSelector)
        binding.botonGuardar.setOnClickListener { guardar() }

        recolectar(viewModel.estado) { estado ->
            estado.original?.let { v ->
                if (!viewModel.camposRellenados) {
                    binding.etMarca.setText(v.marca)
                    binding.etModelo.setText(v.modelo)
                    binding.etMatricula.setText(v.matricula)
                    binding.etAnio.setText(v.anio.toString())
                    binding.etCombustible.setText(v.combustible, false)
                    binding.etKm.setText(v.kmActuales.toString())
                    viewModel.camposRellenados = true
                }
            }
            binding.etFechaItv.setText(estado.fechaItv?.let { Fechas.formatear(it) } ?: "")
            binding.botonGuardar.isEnabled = !estado.guardando
            mostrarErrores(estado.errores)
        }

        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is EventoUi.Guardado -> {
                    mostrarMensaje(evento.texto)
                    findNavController().navigateUp()
                }
                is EventoUi.Mensaje -> mostrarMensaje(evento.texto, larga = true)
                is EventoUi.Eliminado -> Unit
            }
        }
    }

    private fun guardar() {
        viewModel.guardar(
            ValidadorVehiculo.Entrada(
                marca = binding.etMarca.text.toString(),
                modelo = binding.etModelo.text.toString(),
                matricula = binding.etMatricula.text.toString(),
                anio = binding.etAnio.text.toString(),
                combustible = binding.etCombustible.text.toString(),
                km = binding.etKm.text.toString(),
                fechaItvEpochDay = null
            )
        )
    }

    private var erroresMostrados: Map<Campo, ErrorValidacion>? = null

    private fun mostrarErrores(errores: Map<Campo, ErrorValidacion>) {
        if (errores == erroresMostrados) return
        erroresMostrados = errores
        val campos: Map<Campo, TextInputLayout> = mapOf(
            Campo.MARCA to binding.tilMarca,
            Campo.MODELO to binding.tilModelo,
            Campo.MATRICULA to binding.tilMatricula,
            Campo.ANIO to binding.tilAnio,
            Campo.COMBUSTIBLE to binding.tilCombustible,
            Campo.KM to binding.tilKm,
            Campo.FECHA_ITV to binding.tilFechaItv
        )
        campos.forEach { (campo, til) -> til.error = errores[campo]?.mensaje(requireContext()) }
        campos.entries.firstOrNull { errores.containsKey(it.key) }?.value?.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
