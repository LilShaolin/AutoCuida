package com.autocuida.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tipo de mantenimiento con sus intervalos configurables.
 *
 * Al menos uno de los dos intervalos debe estar definido; la regla se valida en
 * [com.autocuida.app.domain.validacion.ValidadorTipo] y, a nivel de base de datos,
 * mediante triggers creados en [com.autocuida.app.data.db.AppDatabase].
 *
 * @property codigo identificador estable de los tipos predefinidos (por ejemplo `ITV`).
 * Es `null` en los tipos creados por el usuario.
 */
@Entity(
    tableName = "tipos_mantenimiento",
    indices = [
        Index(value = ["nombre"], unique = true),
        Index(value = ["codigo"], unique = true)
    ]
)
data class TipoMantenimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    @ColumnInfo(name = "intervalo_km")
    val intervaloKm: Int?,
    @ColumnInfo(name = "intervalo_meses")
    val intervaloMeses: Int?,
    val codigo: String? = null
) {
    val esItv: Boolean get() = codigo == CODIGO_ITV

    companion object {
        const val CODIGO_ACEITE = "ACEITE"
        const val CODIGO_FILTROS = "FILTROS"
        const val CODIGO_NEUMATICOS = "NEUMATICOS"
        const val CODIGO_FRENOS = "FRENOS"
        const val CODIGO_DISTRIBUCION = "DISTRIBUCION"
        const val CODIGO_ITV = "ITV"
    }
}
