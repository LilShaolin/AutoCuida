package com.autocuida.app.ui.vehiculos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autocuida.app.R
import com.autocuida.app.databinding.FragmentListaVehiculosBinding
import com.autocuida.app.ui.comun.contenedor
import com.autocuida.app.ui.comun.recolectar

/** Pantalla 1: Mis vehículos. */
class ListaVehiculosFragment : Fragment() {

    private var _binding: FragmentListaVehiculosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ListaVehiculosViewModel by viewModels {
        viewModelFactory {
            initializer {
                ListaVehiculosViewModel(contenedor.vehiculoRepository, contenedor.recordatorioRepository)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListaVehiculosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cabecera = CabeceraVehiculosAdapter()
        val adaptador = VehiculosAdapter { id ->
            findNavController().navigate(R.id.action_lista_to_detalle, bundleOf(DetalleVehiculoFragment.ARG_VEHICULO_ID to id))
        }
        val columnas = resources.getInteger(R.integer.columnas_lista)
        binding.lista.layoutManager = GridLayoutManager(requireContext(), columnas).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                    if (position < cabecera.itemCount) columnas else 1
            }
        }
        binding.lista.adapter = ConcatAdapter(cabecera, adaptador)

        binding.vacio.vacioIcono.setImageResource(R.drawable.ic_coche)
        binding.vacio.vacioTitulo.setText(R.string.vehiculos_vacio_titulo)
        binding.vacio.vacioTexto.setText(R.string.vehiculos_vacio_texto)

        binding.fabAnadir.setOnClickListener { findNavController().navigate(R.id.action_lista_to_formVehiculo) }
        binding.lista.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) binding.fabAnadir.shrink() else if (dy < 0) binding.fabAnadir.extend()
            }
        })

        recolectar(viewModel.estado) { estado ->
            adaptador.submitList(estado.vehiculos)
            cabecera.actualizar(estado.vehiculos.size, estado.avisos)
            binding.vacio.root.isVisible = !estado.cargando && estado.vehiculos.isEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
