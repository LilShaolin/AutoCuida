package com.autocuida.app.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.autocuida.app.data.entity.TipoMantenimiento

/**
 * Tipos de mantenimiento precargados y reglas de integridad adicionales.
 *
 * Los intervalos iniciales son valores genéricos de partida y se pueden modificar desde la
 * pantalla «Tipos». No sustituyen a las indicaciones del fabricante de cada vehículo.
 * La ITV solo usa su intervalo para proponer la fecha de la siguiente inspección; el
 * vencimiento real es la fecha de ITV guardada en cada vehículo.
 */
internal object DatosIniciales {

    data class TipoInicial(val codigo: String, val nombre: String, val km: Int?, val meses: Int?)

    val TIPOS = listOf(
        TipoInicial(TipoMantenimiento.CODIGO_ACEITE, "Aceite", 15_000, 12),
        TipoInicial(TipoMantenimiento.CODIGO_FILTROS, "Filtros", 30_000, 24),
        TipoInicial(TipoMantenimiento.CODIGO_NEUMATICOS, "Neumáticos", 10_000, 12),
        TipoInicial(TipoMantenimiento.CODIGO_FRENOS, "Frenos", 20_000, 12),
        TipoInicial(TipoMantenimiento.CODIGO_DISTRIBUCION, "Distribución", 100_000, 60),
        TipoInicial(TipoMantenimiento.CODIGO_ITV, "ITV", null, 12)
    )

    /** Triggers que garantizan en SQLite las reglas que Room no expresa con anotaciones. */
    val TRIGGERS = listOf(
        """
        CREATE TRIGGER IF NOT EXISTS trg_tipos_intervalo_insert
        BEFORE INSERT ON tipos_mantenimiento
        WHEN (NEW.intervalo_km IS NULL AND NEW.intervalo_meses IS NULL)
          OR NEW.intervalo_km <= 0 OR NEW.intervalo_meses <= 0
        BEGIN SELECT RAISE(ABORT, 'Intervalo de mantenimiento no valido'); END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS trg_tipos_intervalo_update
        BEFORE UPDATE ON tipos_mantenimiento
        WHEN (NEW.intervalo_km IS NULL AND NEW.intervalo_meses IS NULL)
          OR NEW.intervalo_km <= 0 OR NEW.intervalo_meses <= 0
        BEGIN SELECT RAISE(ABORT, 'Intervalo de mantenimiento no valido'); END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS trg_mantenimientos_valores_insert
        BEFORE INSERT ON mantenimientos
        WHEN NEW.coste < 0 OR NEW.km < 0
        BEGIN SELECT RAISE(ABORT, 'Coste o kilometros negativos'); END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS trg_mantenimientos_valores_update
        BEFORE UPDATE ON mantenimientos
        WHEN NEW.coste < 0 OR NEW.km < 0
        BEGIN SELECT RAISE(ABORT, 'Coste o kilometros negativos'); END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS trg_vehiculos_km_insert
        BEFORE INSERT ON vehiculos WHEN NEW.km_actuales < 0
        BEGIN SELECT RAISE(ABORT, 'Kilometros negativos'); END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS trg_vehiculos_km_update
        BEFORE UPDATE ON vehiculos WHEN NEW.km_actuales < 0
        BEGIN SELECT RAISE(ABORT, 'Kilometros negativos'); END
        """
    )

    fun crearTriggers(db: SupportSQLiteDatabase) {
        TRIGGERS.forEach { db.execSQL(it.trimIndent()) }
    }

    fun insertarTipos(db: SupportSQLiteDatabase) {
        TIPOS.forEach { tipo ->
            db.execSQL(
                "INSERT OR IGNORE INTO tipos_mantenimiento (nombre, intervalo_km, intervalo_meses, codigo) VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(tipo.nombre, tipo.km, tipo.meses, tipo.codigo)
            )
        }
    }
}
