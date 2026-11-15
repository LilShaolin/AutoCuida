package com.autocuida.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.autocuida.app.MainActivity
import com.autocuida.app.R
import com.autocuida.app.data.entity.RecordatorioDetalle
import com.autocuida.app.data.entity.TipoMantenimiento
import com.autocuida.app.domain.EstadoMantenimiento
import com.autocuida.app.domain.EvaluacionVencimiento

/** Canal y construcción de las notificaciones locales de vencimientos. */
object Notificaciones {

    const val CANAL_VENCIMIENTOS = "vencimientos"
    private const val GRUPO_VENCIMIENTOS = "com.autocuida.app.VENCIMIENTOS"
    private const val BASE_ID_NOTIFICACION = 1_000

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_VENCIMIENTOS,
            context.getString(R.string.canal_vencimientos_nombre),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.canal_vencimientos_descripcion) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** Indica si la aplicación puede mostrar notificaciones (permiso en Android 13+ y ajustes del usuario). */
    fun puedeNotificar(context: Context): Boolean {
        val permiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permiso && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Muestra el aviso de un recordatorio. El identificador es estable por recordatorio, de modo que
     * un nuevo aviso sobre el mismo vencimiento sustituye al anterior en lugar de duplicarlo.
     *
     * @return `true` si la notificación se publicó.
     */
    fun mostrarVencimiento(
        context: Context,
        detalle: RecordatorioDetalle,
        evaluacion: EvaluacionVencimiento
    ): Boolean {
        if (!puedeNotificar(context)) return false
        val vencido = evaluacion.estado == EstadoMantenimiento.VENCIDO
        val vehiculo = "${detalle.vehiculoDescripcion} (${detalle.vehiculoMatricula})"
        val titulo = if (detalle.tipoCodigo == TipoMantenimiento.CODIGO_ITV) {
            context.getString(if (vencido) R.string.notif_titulo_itv_vencida else R.string.notif_titulo_itv_proxima, vehiculo)
        } else {
            context.getString(
                if (vencido) R.string.notif_titulo_vencido else R.string.notif_titulo_proximo,
                detalle.tipoNombre, vehiculo
            )
        }
        val texto = TextosVencimiento.restante(context, evaluacion) +
            context.getString(R.string.venc_separador) + TextosVencimiento.limites(context, evaluacion)

        val intencion = NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.detalleVehiculoFragment)
            .setArguments(bundleOf("vehiculoId" to detalle.recordatorio.vehiculoId))
            .createPendingIntent()

        val notificacion = NotificationCompat.Builder(context, CANAL_VENCIMIENTOS)
            .setSmallIcon(R.drawable.ic_stat_autocuida)
            .setColor(ContextCompat.getColor(context, if (vencido) R.color.estado_rojo else R.color.estado_ambar))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(intencion)
            .setAutoCancel(true)
            .setGroup(GRUPO_VENCIMIENTOS)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(idNotificacion(detalle.recordatorio.id), notificacion)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Retira los avisos que ya no corresponden: los de recordatorios eliminados (por ejemplo, al borrar
     * el vehículo) y los que han vuelto a estar al día. Se conservan los de [vigentes].
     */
    fun retirarObsoletos(context: Context, vigentes: Set<Long>) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        val idsVigentes = vigentes.map(::idNotificacion).toSet()
        // Solo se tocan los avisos de vencimientos (grupo propio), nunca otras notificaciones del sistema.
        gestor.activeNotifications
            .filter { it.notification.group == GRUPO_VENCIMIENTOS && it.id !in idsVigentes }
            .forEach { gestor.cancel(it.tag, it.id) }
    }

    private fun idNotificacion(recordatorioId: Long): Int = BASE_ID_NOTIFICACION + recordatorioId.toInt()
}
