package com.autocuida.app.ui.mantenimiento

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.autocuida.app.R
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.databinding.FragmentFormMantenimientoBinding
import com.autocuida.app.domain.validacion.ErrorValidacion
import com.autocuida.app.domain.validacion.ValidadorMantenimiento.Campo
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.ui.comun.contenedor
import com.autocuida.app.ui.comun.mensaje
import com.autocuida.app.ui.comun.mostrarMensaje
import com.autocuida.app.ui.comun.recolectar
import com.autocuida.app.ui.comun.seleccionarFecha
import com.autocuida.app.util.Fechas
import com.autocuida.app.util.Formatos
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

/** Pantalla 4: registro y edición de mantenimientos. */
class FormMantenimientoFragment : Fragment() {

    companion object {
        const val ARG_VEHICULO_ID = "vehiculoId"
        const val ARG_MANTENIMIENTO_ID = "mantenimientoId"
        const val ARG_TIPO_ID = "tipoId"
    }

    private var _binding: FragmentFormMantenimientoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FormMantenimientoViewModel by viewModels {
        viewModelFactory {
            initializer {
                val c = contenedor
                FormMantenimientoViewModel(
                    createSavedStateHandle(), c.mantenimientoRepository, c.vehiculoRepository,
                    c.tipoRepository, c.programadorAvisos
                )
            }
        }
    }

    private var tiposMostrados: List<TipoMantenimiento>? = null
    private var erroresMostrados: Map<Campo, ErrorValidacion>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFormMantenimientoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(
            if (viewModel.esEdicion) R.string.titulo_editar_mantenimiento else R.string.titulo_nuevo_mantenimiento
        )
        if (viewModel.esEdicion) requireActivity().addMenuProvider(menu, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val abrirFecha = View.OnClickListener {
            seleccionarFecha(getString(R.string.campo_fecha), viewModel.estado.value.fecha, soloPasado = true) {
                viewModel.seleccionarFecha(it)
            }
        }
        binding.etFecha.setOnClickListener(abrirFecha)
        binding.tilFecha.setEndIconOnClickListener(abrirFecha)

        binding.etProximaItv.setOnClickListener {
            val estado = viewModel.estado.value
            seleccionarFecha(getString(R.string.campo_fecha_itv), estado.proximaItv ?: estado.fecha) {
                viewModel.seleccionarProximaItv(it)
            }
        }
        binding.tilProximaItv.setEndIconOnClickListener { viewModel.seleccionarProximaItv(null) }

        binding.etTipo.setOnItemClickListener { _, _, posicion, _ ->
            tiposMostrados?.getOrNull(posicion)?.let { viewModel.seleccionarTipo(it.id) }
        }
        binding.botonGuardar.setOnClickListener {
            viewModel.guardar(
                km = binding.etKm.text.toString(),
                coste = binding.etCoste.text.toString(),
                taller = binding.etTaller.text.toString(),
                notas = binding.etNotas.text.toString()
            )
        }

        recolectar(viewModel.estado) { estado ->
            if (!estado.cargado) return@recolectar
            estado.vehiculo?.let {
                binding.vehiculo.text = getString(R.string.mantenimiento_vehiculo, "${it.marca} ${it.modelo} (${it.matricula})")
            }
            if (estado.tipos != tiposMostrados) {
                tiposMostrados = estado.tipos
                binding.etTipo.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estado.tipos.map { it.nombre })
                )
            }
            binding.etTipo.setText(estado.tipoSeleccionado?.nombre ?: "", false)
            binding.etFecha.setText(estado.fecha?.let { Fechas.formatear(it) } ?: "")
            binding.tilProximaItv.isVisible = estado.esItv
            binding.etProximaItv.setText(estado.proximaItv?.let { Fechas.formatear(it) } ?: "")

            if (!viewModel.camposRellenados) {
                val original = estado.original
                if (original != null) {
                    binding.etKm.setText(original.km.toString())
                    binding.etCoste.setText(Formatos.costeEditable(original.coste))
                    binding.etTaller.setText(original.taller ?: "")
                    binding.etNotas.setText(original.notas ?: "")
                } else {
                    estado.vehiculo?.let { binding.etKm.setText(it.kmActuales.toString()) }
                }
                viewModel.camposRellenados = true
            }
            binding.botonGuardar.isEnabled = !estado.guardando
            mostrarErrores(estado.errores, estado.kmReferenciaError)
        }

        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is EventoUi.Guardado -> {
                    mostrarMensaje(evento.texto)
                    findNavController().navigateUp()
                }
                is EventoUi.Eliminado -> {
                    mostrarMensaje(evento.texto)
                    findNavController().navigateUp()
                }
                is EventoUi.Mensaje -> mostrarMensaje(evento.texto, larga = true)
            }
        }
    }

    private fun mostrarErrores(errores: Map<Campo, ErrorValidacion>, kmReferencia: Int?) {
        if (errores == erroresMostrados) return
        erroresMostrados = errores
        val campos: Map<Campo, TextInputLayout> = mapOf(
            Campo.TIPO to binding.tilTipo,
            Campo.FECHA to binding.tilFecha,
            Campo.KM to binding.tilKm,
            Campo.COSTE to binding.tilCoste,
            Campo.TALLER to binding.tilTaller,
            Campo.NOTAS to binding.tilNotas,
            Campo.FECHA_ITV to binding.tilProximaItv
        )
        campos.forEach { (campo, til) ->
            til.error = errores[campo]?.mensaje(requireContext(), if (campo == Campo.KM) kmReferencia else null)
        }
        campos.entries.firstOrNull { errores.containsKey(it.key) }?.value?.requestFocus()
    }

    private val menu = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_eliminar, menu)
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean {
            if (item.itemId != R.id.accion_eliminar) return false
            MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.eliminar_mantenimiento_titulo)
                .setMessage(R.string.eliminar_mantenimiento_mensaje)
                .setNegativeButton(R.string.accion_cancelar, null)
                .setPositiveButton(R.string.accion_eliminar) { _, _ -> viewModel.eliminar() }
                .show()
            return true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tiposMostrados = null
        erroresMostrados = null
        _binding = null
    }
}
