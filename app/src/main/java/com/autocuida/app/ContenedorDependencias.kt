package com.autocuida.app

import android.content.Context
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.repository.GastosRepository
import com.autocuida.app.data.repository.MantenimientoRepository
import com.autocuida.app.data.repository.RecordatorioRepository
import com.autocuida.app.data.repository.TipoMantenimientoRepository
import com.autocuida.app.data.repository.VehiculoRepository
import com.autocuida.app.worker.PlanificadorRevisiones

/**
 * Inyección de dependencias manual. Crea una única instancia de la base de datos y de cada
 * repositorio para toda la aplicación. Evita introducir un framework adicional en un proyecto
 * de este tamaño, manteniendo los ViewModels desacoplados de la construcción de sus dependencias.
 */
class ContenedorDependencias(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.obtener(appContext) }

    val recordatorioRepository by lazy { RecordatorioRepository(database) }
    val vehiculoRepository by lazy { VehiculoRepository(database, recordatorioRepository) }
    val mantenimientoRepository by lazy { MantenimientoRepository(database, recordatorioRepository) }
    val tipoRepository by lazy { TipoMantenimientoRepository(database, recordatorioRepository) }
    val gastosRepository by lazy { GastosRepository(database) }

    /** Solicita una revisión inmediata de avisos tras un cambio de datos. */
    val programadorAvisos: ProgramadorAvisos = ProgramadorAvisos { PlanificadorRevisiones.revisarAhora(appContext) }
}

/** Abstracción que permite a los ViewModels pedir una revisión de avisos sin depender de Context. */
fun interface ProgramadorAvisos {
    fun revisarAhora()
}
