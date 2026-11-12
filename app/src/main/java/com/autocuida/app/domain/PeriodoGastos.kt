package com.autocuida.app.domain

import java.time.LocalDate

/** Periodo de consulta de gastos. */
sealed interface PeriodoGastos {
    val desde: LocalDate
    val hasta: LocalDate

    /** Año natural completo. Los gastos se agrupan por meses. */
    data class Anio(val anio: Int) : PeriodoGastos {
        override val desde: LocalDate get() = LocalDate.of(anio, 1, 1)
        override val hasta: LocalDate get() = LocalDate.of(anio, 12, 31)
    }

    /** Todo el historial. Los gastos se agrupan por años. */
    data object Todo : PeriodoGastos {
        override val desde: LocalDate = LocalDate.of(1900, 1, 1)
        override val hasta: LocalDate = LocalDate.of(2999, 12, 31)
    }
}
