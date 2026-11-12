package com.autocuida.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.autocuida.app.data.db.AppDatabase
import com.autocuida.app.data.entity.Mantenimiento
import com.autocuida.app.data.entity.Vehiculo
import java.time.LocalDate

/** Utilidades comunes de las pruebas de integración: base de datos Room en memoria. */
object BaseDatosPrueba {

    /** Crea una base de datos en memoria con el mismo callback (triggers y tipos iniciales) que la real. */
    fun crear(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CallbackInicial())
            .allowMainThreadQueries()
            .build()
    }

    fun vehiculo(
        matricula: String = "1234ABC",
        km: Int = 50_000,
        fechaItv: LocalDate = LocalDate.of(2027, 5, 12)
    ) = Vehiculo(
        marca = "Seat", modelo = "León", matricula = matricula, anio = 2018,
        combustible = "Gasolina", kmActuales = km, fechaItv = fechaItv.toEpochDay()
    )

    fun mantenimiento(
        vehiculoId: Long,
        tipoId: Long,
        fecha: LocalDate = LocalDate.of(2026, 1, 15),
        km: Int = 45_000,
        coste: Double = 80.0
    ) = Mantenimiento(
        vehiculoId = vehiculoId, tipoId = tipoId, fecha = fecha.toEpochDay(),
        km = km, coste = coste, taller = "Taller Centro", notas = null
    )
}
