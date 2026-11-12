package com.autocuida.app

import android.app.Application
import com.autocuida.app.util.Notificaciones
import com.autocuida.app.worker.PlanificadorRevisiones

class AutoCuidaApp : Application() {

    lateinit var contenedor: ContenedorDependencias
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = ContenedorDependencias(this)
        Notificaciones.crearCanal(this)
        PlanificadorRevisiones.programarRevisionDiaria(this)
    }
}
