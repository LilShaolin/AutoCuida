package com.autocuida.app.ui.gastos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.autocuida.app.R
import com.autocuida.app.data.entity.Vehiculo
import com.autocuida.app.databinding.FragmentGastosBinding
import com.autocuida.app.domain.PeriodoGastos
import com.autocuida.app.ui.comun.contenedor
import com.autocuida.app.ui.comun.recolectar
import com.autocuida.app.ui.mantenimiento.FormMantenimientoFragment
import com.autocuida.app.ui.mantenimiento.HistorialAdapter
import com.autocuida.app.util.Formatos
import com.google.android.material.divider.MaterialDividerItemDecoration

/** Pantalla 5: estadísticas de gastos calculadas a partir de los mantenimientos registrados. */
class GastosFragment : Fragment() {

    private var _binding: FragmentGastosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GastosViewModel by viewModels {
        viewModelFactory {
            initializer { GastosViewModel(contenedor.gastosRepository, contenedor.vehiculoRepository) }
        }
    }

    private var periodos: List<PeriodoGastos> = emptyList()
    private var vehiculos: List<Vehiculo?> = emptyList()
    private var ultimaSerie: Any? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGastosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        GraficoGastos.configurar(binding.grafica)

        val porTipo = GastoAgrupadoAdapter()
        val porVehiculo = GastoAgrupadoAdapter()
        val historial = HistorialAdapter(mostrarVehiculo = true) { item ->
            findNavController().navigate(
                R.id.action_gastos_to_formMantenimiento,
                bundleOf(
                    FormMantenimientoFragment.ARG_VEHICULO_ID to item.mantenimiento.vehiculoId,
                    FormMantenimientoFragment.ARG_MANTENIMIENTO_ID to item.mantenimiento.id
                )
            )
        }
        binding.listaTipos.adapter = porTipo
        binding.listaVehiculos.adapter = porVehiculo
        binding.listaHistorial.adapter = historial
        binding.listaHistorial.addItemDecoration(
            MaterialDividerItemDecoration(requireContext(), MaterialDividerItemDecoration.VERTICAL).apply {
                isLastItemDecorated = false
            }
        )

        binding.vacio.vacioIcono.setImageResource(R.drawable.ic_euro)
        binding.vacio.vacioTitulo.setText(R.string.gastos_vacio_titulo)
        binding.vacio.vacioTexto.setText(R.string.gastos_vacio_texto)

        binding.etPeriodo.setOnItemClickListener { _, _, posicion, _ ->
            periodos.getOrNull(posicion)?.let { viewModel.seleccionarPeriodo(it) }
        }
        binding.etVehiculo.setOnItemClickListener { _, _, posicion, _ ->
            if (posicion in vehiculos.indices) viewModel.seleccionarVehiculo(vehiculos[posicion]?.id)
        }

        recolectar(viewModel.estado) { estado ->
            if (estado.cargando) return@recolectar
            val f = estado.filtros
            val d = estado.datos

            // Opciones de los filtros
            periodos = estado.anios.map { PeriodoGastos.Anio(it) } + PeriodoGastos.Todo
            binding.etPeriodo.setAdapter(simple(periodos.map { textoPeriodo(it) }))
            binding.etPeriodo.setText(textoPeriodo(f.periodo), false)
            vehiculos = listOf<Vehiculo?>(null) + estado.vehiculos
            binding.etVehiculo.setAdapter(simple(vehiculos.map { textoVehiculo(it) }))
            binding.etVehiculo.setText(textoVehiculo(estado.vehiculos.firstOrNull { it.id == f.vehiculoId }), false)

            // Total
            binding.totalEtiqueta.text = when (val p = f.periodo) {
                is PeriodoGastos.Anio -> getString(R.string.gastos_total_anio, p.anio)
                PeriodoGastos.Todo -> getString(R.string.gastos_total_historial)
            }
            binding.total.text = Formatos.euros(d.total)
            binding.totalRegistros.text = resources.getQuantityString(
                R.plurals.gastos_registros, d.historial.size, d.historial.size
            )

            val hayDatos = d.historial.isNotEmpty()
            binding.vacio.root.isVisible = !hayDatos
            binding.contenido.isVisible = hayDatos
            if (hayDatos) {
                binding.tituloGrafica.setText(
                    if (f.periodo is PeriodoGastos.Anio) R.string.gastos_grafica_mensual else R.string.gastos_grafica_anual
                )
                val serie = f.periodo to d.serie
                if (serie != ultimaSerie) {
                    ultimaSerie = serie
                    GraficoGastos.mostrar(binding.grafica, d.serie, f.periodo)
                }
                porTipo.mostrar(d.porTipo, d.total)
                porVehiculo.mostrar(d.porVehiculo, d.total)
                // Con un único vehículo filtrado, el desglose por vehículo no aporta información.
                binding.tarjetaVehiculos.isVisible = f.vehiculoId == null && d.porVehiculo.size > 1
                historial.submitList(d.historial)
            }
        }
    }

    private fun simple(textos: List<String>) =
        ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, textos)

    private fun textoPeriodo(periodo: PeriodoGastos) = when (periodo) {
        is PeriodoGastos.Anio -> periodo.anio.toString()
        PeriodoGastos.Todo -> getString(R.string.gastos_todo_historial)
    }

    private fun textoVehiculo(v: Vehiculo?) =
        v?.let { "${it.marca} ${it.modelo} · ${it.matricula}" } ?: getString(R.string.gastos_todos_vehiculos)

    override fun onDestroyView() {
        super.onDestroyView()
        ultimaSerie = null
        _binding = null
    }
}
