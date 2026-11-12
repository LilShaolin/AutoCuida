package com.autocuida.app.ui.vehiculos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
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
import com.autocuida.app.databinding.FragmentDetalleVehiculoBinding
import com.autocuida.app.ui.comun.EstadoUi
import com.autocuida.app.ui.comun.EventoUi
import com.autocuida.app.ui.comun.contenedor
import com.autocuida.app.ui.comun.mostrarInsignia
import com.autocuida.app.ui.comun.mostrarMensaje
import com.autocuida.app.ui.comun.recolectar
import com.autocuida.app.ui.comun.seleccionarFecha
import com.autocuida.app.ui.mantenimiento.FormMantenimientoFragment
import com.autocuida.app.ui.mantenimiento.HistorialAdapter
import com.autocuida.app.util.Fechas
import com.autocuida.app.util.Formatos
import com.autocuida.app.util.TextosVencimiento
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration

/** Pantalla 3: detalle del vehículo con estado, ITV, vencimientos e historial. */
class DetalleVehiculoFragment : Fragment() {

    companion object {
        const val ARG_VEHICULO_ID = "vehiculoId"
    }

    private var _binding: FragmentDetalleVehiculoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetalleVehiculoViewModel by viewModels {
        viewModelFactory {
            initializer {
                val c = contenedor
                DetalleVehiculoViewModel(
                    createSavedStateHandle(), c.vehiculoRepository, c.recordatorioRepository,
                    c.tipoRepository, c.mantenimientoRepository, c.programadorAvisos
                )
            }
        }
    }

    private var tiposEnFiltro: List<TipoMantenimiento>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleVehiculoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val vencimientos = VencimientosAdapter { tipoId -> abrirFormularioMantenimiento(tipoId = tipoId) }
        binding.listaVencimientos.adapter = vencimientos
        binding.listaVencimientos.addItemDecoration(separador())

        val historial = HistorialAdapter(mostrarVehiculo = false) { item ->
            abrirFormularioMantenimiento(mantenimientoId = item.mantenimiento.id)
        }
        binding.listaHistorial.adapter = historial
        binding.listaHistorial.addItemDecoration(separador())

        binding.botonKm.setOnClickListener {
            findNavController().navigate(R.id.action_detalle_to_actualizarKm, bundleOf(ARG_VEHICULO_ID to viewModel.vehiculoId))
        }
        binding.botonItv.setOnClickListener {
            seleccionarFecha(getString(R.string.campo_fecha_itv), viewModel.estado.value.vehiculo?.fechaItv) {
                viewModel.actualizarFechaItv(it)
            }
        }
        binding.fabMantenimiento.setOnClickListener { abrirFormularioMantenimiento() }
        binding.scroll.setOnScrollChangeListener { _: View, _: Int, y: Int, _: Int, yAnterior: Int ->
            if (y > yAnterior) binding.fabMantenimiento.shrink() else if (y < yAnterior) binding.fabMantenimiento.extend()
        }

        requireActivity().addMenuProvider(menu, viewLifecycleOwner, Lifecycle.State.RESUMED)

        recolectar(viewModel.estado) { estado ->
            val v = estado.vehiculo
            if (v == null) {
                if (!estado.cargando) salir()
                return@recolectar
            }
            (activity as? AppCompatActivity)?.supportActionBar?.title = "${v.marca} ${v.modelo}"
            binding.titulo.text = "${v.marca} ${v.modelo}"
            binding.matricula.text = v.matricula
            binding.subtitulo.text = getString(R.string.vehiculo_subtitulo, v.anio, v.combustible)
            binding.km.text = Formatos.km(v.kmActuales)

            binding.estadoGeneral.mostrarInsignia(estado.estadoGeneral, EstadoUi.de(estado.estadoGeneral).textoGeneral)

            estado.itv?.let { itv ->
                binding.itvInsignia.mostrarInsignia(itv.estado)
                binding.itvFecha.text = getString(R.string.detalle_itv_fecha, Fechas.formatear(v.fechaItv))
                binding.itvRestante.text = TextosVencimiento.restante(requireContext(), itv)
            }

            vencimientos.submitList(estado.vencimientos)
            historial.submitList(estado.historial)
            actualizarFiltros(estado.tiposFiltro, estado.filtroTipoId)

            val sinHistorial = estado.historial.isEmpty()
            binding.tarjetaHistorial.isVisible = !sinHistorial
            binding.historialVacio.isVisible = sinHistorial
            binding.historialVacio.setText(
                if (estado.historialTotal == 0) R.string.detalle_historial_vacio else R.string.detalle_historial_vacio_filtro
            )
            binding.filtros.isVisible = estado.historialTotal > 0
        }

        recolectar(viewModel.eventos) { evento ->
            when (evento) {
                is EventoUi.Mensaje -> mostrarMensaje(evento.texto)
                is EventoUi.Eliminado -> {
                    mostrarMensaje(evento.texto)
                    salir()
                }
                is EventoUi.Guardado -> Unit
            }
        }
    }

    /** Crea los chips de filtro solo cuando cambian los tipos disponibles. */
    private fun actualizarFiltros(tipos: List<TipoMantenimiento>, seleccionado: Long?) {
        if (tipos != tiposEnFiltro) {
            tiposEnFiltro = tipos
            binding.filtros.setOnCheckedStateChangeListener(null)
            binding.filtros.removeAllViews()
            binding.filtros.addView(crearChip(getString(R.string.filtro_todos), View.NO_ID.toLong()))
            tipos.forEach { binding.filtros.addView(crearChip(it.nombre, it.id)) }
            binding.filtros.setOnCheckedStateChangeListener { grupo, ids ->
                val chip = ids.firstOrNull()?.let { grupo.findViewById<Chip>(it) }
                viewModel.filtrar((chip?.tag as? Long)?.takeIf { it != View.NO_ID.toLong() })
            }
        }
        val objetivo = seleccionado ?: View.NO_ID.toLong()
        for (i in 0 until binding.filtros.childCount) {
            val chip = binding.filtros.getChildAt(i) as Chip
            if (chip.tag == objetivo && !chip.isChecked) chip.isChecked = true
        }
    }

    private fun crearChip(texto: String, tag: Long) = Chip(
        requireContext(), null, com.google.android.material.R.attr.chipStyle
    ).apply {
        id = View.generateViewId()
        text = texto
        this.tag = tag
        isCheckable = true
        isCheckedIconVisible = true
        setEnsureMinTouchTargetSize(true)
    }

    private fun separador() = MaterialDividerItemDecoration(
        requireContext(), MaterialDividerItemDecoration.VERTICAL
    ).apply {
        isLastItemDecorated = false
        dividerInsetStart = resources.getDimensionPixelSize(R.dimen.espacio_l)
        dividerInsetEnd = resources.getDimensionPixelSize(R.dimen.espacio_l)
    }

    /** Vuelve a la lista una sola vez aunque coincidan el evento de borrado y la desaparición del vehículo. */
    private fun salir() {
        val nav = findNavController()
        if (nav.currentDestination?.id == R.id.detalleVehiculoFragment) nav.navigateUp()
    }

    private fun abrirFormularioMantenimiento(mantenimientoId: Long = 0, tipoId: Long = 0) {
        findNavController().navigate(
            R.id.action_detalle_to_formMantenimiento,
            bundleOf(
                FormMantenimientoFragment.ARG_VEHICULO_ID to viewModel.vehiculoId,
                FormMantenimientoFragment.ARG_MANTENIMIENTO_ID to mantenimientoId,
                FormMantenimientoFragment.ARG_TIPO_ID to tipoId
            )
        )
    }

    private val menu = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_detalle_vehiculo, menu)
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
            R.id.accion_editar -> {
                findNavController().navigate(R.id.action_detalle_to_formVehiculo, bundleOf(ARG_VEHICULO_ID to viewModel.vehiculoId))
                true
            }
            R.id.accion_eliminar -> {
                confirmarEliminacion()
                true
            }
            else -> false
        }
    }

    private fun confirmarEliminacion() {
        val v = viewModel.estado.value.vehiculo ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_delete)
            .setTitle(R.string.eliminar_vehiculo_titulo)
            .setMessage(getString(R.string.eliminar_vehiculo_mensaje, "${v.marca} ${v.modelo} (${v.matricula})"))
            .setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_eliminar) { _, _ -> viewModel.eliminarVehiculo() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tiposEnFiltro = null
        _binding = null
    }
}
