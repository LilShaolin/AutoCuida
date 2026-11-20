package com.autocuida.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.autocuida.app.databinding.ActivityMainBinding
import com.autocuida.app.util.Notificaciones
import com.autocuida.app.worker.PlanificadorRevisiones
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.snackbar.Snackbar

/**
 * Actividad única de AutoCuida. Aloja el NavHostFragment, la barra superior y la navegación
 * principal (inferior en móviles en vertical, lateral en pantallas anchas).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val destinosPrincipales = setOf(
        R.id.listaVehiculosFragment, R.id.gastosFragment, R.id.tiposFragment
    )

    /** Si se podía notificar en la última comprobación; permite detectar que el usuario activa los avisos. */
    private var podiaNotificar = false

    private val preferencias by lazy { getSharedPreferences(PREFERENCIAS, MODE_PRIVATE) }

    private val solicitudPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            preferencias.edit { putBoolean(PREF_DENEGACION_INFORMADA, false) }
            // Se revisan al momento los vencimientos que no se pudieron avisar sin permiso.
            podiaNotificar = Notificaciones.puedeNotificar(this)
            PlanificadorRevisiones.revisarAhora(this)
        } else if (!preferencias.getBoolean(PREF_DENEGACION_INFORMADA, false)) {
            // Se informa una sola vez: si el permiso queda denegado, Android responde sin mostrar el
            // diálogo en cada arranque y el mensaje se repetiría sin que el usuario haya hecho nada.
            preferencias.edit { putBoolean(PREF_DENEGACION_INFORMADA, true) }
            Snackbar.make(binding.root, R.string.permiso_notif_denegado, Snackbar.LENGTH_LONG)
                .setAction(R.string.accion_ajustes) { abrirAjustesNotificaciones() }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHost.navController
        binding.toolbar.setupWithNavController(navController, AppBarConfiguration(destinosPrincipales))

        val navegacion = binding.root.findViewById<NavigationBarView>(R.id.navegacion)
        navegacion.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destino, _ ->
            navegacion.isVisible = destino.id in destinosPrincipales
            ViewCompat.requestApplyInsets(binding.navHost)
        }

        val tiposInsets = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar) { vista, insets ->
            val barras = insets.getInsets(tiposInsets)
            // Con navegación lateral, el propio raíl ya ocupa el margen izquierdo.
            val izquierda = if (navegacion is NavigationRailView) 0 else barras.left
            vista.updatePadding(top = barras.top, left = izquierda, right = barras.right)
            insets
        }
        // El contenido evita la barra de navegación del sistema y el teclado cuando no hay barra inferior.
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHost) { vista, insets ->
            val barras = insets.getInsets(tiposInsets)
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navegacionInferior = navegacion.isVisible && navegacion !is NavigationRailView
            val inferior = if (navegacionInferior) teclado.bottom else maxOf(barras.bottom, teclado.bottom)
            vista.updatePadding(bottom = inferior, right = barras.right)
            insets
        }

        podiaNotificar = Notificaciones.puedeNotificar(this)
        if (savedInstanceState == null) solicitarPermisoNotificaciones()
    }

    override fun onResume() {
        super.onResume()
        // Si el usuario ha activado los avisos desde los ajustes, se revisan los vencimientos pendientes.
        val puede = Notificaciones.puedeNotificar(this)
        if (puede && !podiaNotificar) PlanificadorRevisiones.revisarAhora(this)
        podiaNotificar = puede
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Abre el detalle indicado al pulsar una notificación con la aplicación ya abierta.
        navController.handleDeepLink(intent)
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permiso = Manifest.permission.POST_NOTIFICATIONS
        when {
            ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED -> Unit
            shouldShowRequestPermissionRationale(permiso) ->
                MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.ic_notificacion)
                    .setTitle(R.string.permiso_notif_titulo)
                    .setMessage(R.string.permiso_notif_mensaje)
                    .setPositiveButton(R.string.accion_permitir) { _, _ -> solicitudPermiso.launch(permiso) }
                    .setNegativeButton(R.string.accion_ahora_no, null)
                    .show()
            else -> solicitudPermiso.launch(permiso)
        }
    }

    private companion object {
        const val PREFERENCIAS = "autocuida_preferencias"
        const val PREF_DENEGACION_INFORMADA = "permiso_notificaciones_denegacion_informada"
    }

    private fun abrirAjustesNotificaciones() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }
}
