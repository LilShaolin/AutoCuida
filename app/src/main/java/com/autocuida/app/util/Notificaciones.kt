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
import com.autocuida.app.domain.EvaluadorAvisos

/** Canal y construcción de las notificaciones locales de vencimientos. */
object Notificaciones {

    const val CANAL_VENCIMIENTOS = "vencimientos"
    private const val GRUPO_VENCIMIENTOS = "com.autocuida.app.VENCIMIENTOS"
    private const val BASE_ID_NOTIFICACION = 1_000
    private const val EXTRA_NIVEL = "com.autocuida.app.NIVEL_AVISO"

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_VENCIMIENTOS,
            context.getString(R.string.canal_vencimientos_nombre),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.canal_vencimientos_descripcion) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /**
     * Indica si la aplicación puede mostrar notificaciones: permiso en Android 13+, notificaciones de la
     * aplicación activadas y canal de vencimientos no silenciado por el usuario. Si el canal está
     * desactivado, el aviso no se marca como dado y se mostrará cuando se vuelva a activar.
     */
    fun puedeNotificar(context: Context): Boolean {
        val permiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permiso || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val canal = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CANAL_VENCIMIENTOS)
        return canal != null && canal.importance != NotificationManager.IMPORTANCE_NONE
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
        val nivel = EvaluadorAvisos.nivelDe(evaluacion.estado)
        val id = idNotificacion(detalle.recordatorio.id)
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
            // Repetir el mismo aviso (por ejemplo, si coinciden dos revisiones) no vuelve a sonar.
            .setOnlyAlertOnce(true)
            .addExtras(bundleOf(EXTRA_NIVEL to nivel))
            .build()

        return try {
            // Al empeorar (de próximo a vencido) se retira el aviso anterior para que el nuevo sí suene.
            val gestor = context.getSystemService(NotificationManager::class.java)
            val anterior = gestor.activeNotifications.firstOrNull { it.id == id }
            if (anterior != null && anterior.notification.extras.getInt(EXTRA_NIVEL) < nivel) gestor.cancel(id)
            NotificationManagerCompat.from(context).notify(id, notificacion)
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
