package com.autocuida.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autocuida.app.data.dao.MantenimientoDao
import com.autocuida.app.data.dao.RecordatorioDao
import com.autocuida.app.data.dao.TipoMantenimientoDao
import com.autocuida.app.data.dao.VehiculoDao
import com.autocuida.app.data.entity.Mantenimiento
import com.autocuida.app.data.entity.Recordatorio
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.data.entity.Vehiculo

@Database(
    entities = [Vehiculo::class, TipoMantenimiento::class, Mantenimiento::class, Recordatorio::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehiculoDao(): VehiculoDao
    abstract fun tipoMantenimientoDao(): TipoMantenimientoDao
    abstract fun mantenimientoDao(): MantenimientoDao
    abstract fun recordatorioDao(): RecordatorioDao

    /** Crea triggers y precarga los tipos la primera vez que se crea la base de datos. */
    class CallbackInicial : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            DatosIniciales.crearTriggers(db)
            DatosIniciales.insertarTipos(db)
        }
    }

    companion object {
        const val NOMBRE = "autocuida.db"

        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NOMBRE)
                    .addCallback(CallbackInicial())
                    .build()
                    .also { instancia = it }
            }
    }
}
