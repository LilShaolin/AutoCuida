# Matriz de requisitos de AutoCuida

Versión del producto: **1.0.0** (`versionCode 1`).

Esta matriz vincula cada requisito funcional y no funcional con su implementación, los archivos afectados, los elementos de base de datos implicados, las pruebas asociadas y el lugar de la documentación en el que se describe. Las rutas de código son relativas a `app/src/main/java/com/autocuida/app/`.

Abreviaturas de documentación: **TE** = trabajo escrito (apartado), **MU** = manual de usuario (capítulo), **MI** = manual de instalación. Los casos **PF** son las pruebas funcionales descritas en el apartado 7.5 del trabajo escrito.

## Requisitos funcionales

| ID | Requisito | Implementación | Archivos principales | Base de datos | Pruebas | Documentación |
|---|---|---|---|---|---|---|
| RF-01 | Registrar vehículos | Formulario con validación y normalización de matrícula | `ui/vehiculos/FormVehiculoFragment.kt`, `FormVehiculoViewModel.kt`, `domain/validacion/ValidadorVehiculo.kt`, `data/repository/VehiculoRepository.kt` | `vehiculos` (INSERT) | `ValidadorVehiculoTest`, `VehiculoDaoTest.insertarYConsultarVehiculo`, PF-01, PF-03 | TE 5.1, MU 2 |
| RF-02 | Consultar vehículos | Lista observable con estado general | `ui/vehiculos/ListaVehiculosFragment.kt`, `ListaVehiculosViewModel.kt`, `VehiculosAdapter.kt`, `ResumenVehiculo.kt` | `vehiculos`, `recordatorios` (Flow) | `VehiculoDaoTest`, PF-01, PF-02 | TE 5.1, MU 1 |
| RF-03 | Editar vehículos | Reutilización del formulario en modo edición | `FormVehiculoFragment.kt`, `FormVehiculoViewModel.kt`, `VehiculoRepository.guardar` | `vehiculos` (UPDATE) | `RevisionVencimientosWorkerTest.nuevaFechaItv_reiniciaElAvisoYDejaElEstadoAlDia` (edición vía repositorio) | TE 5.1, MU 3 |
| RF-04 | Eliminar vehículos en cascada | Diálogo de confirmación y `ON DELETE CASCADE` | `ui/vehiculos/DetalleVehiculoFragment.kt`, `data/entity/Mantenimiento.kt`, `Recordatorio.kt` | FK `vehiculo_id` CASCADE | `MantenimientoDaoTest.borrarVehiculoEliminaEnCascadaMantenimientosYRecordatorios`, PF-12 | TE 5.5, MU 12 |
| RF-05 | Matrícula única | Índice UNIQUE y comprobación previa | `data/entity/Vehiculo.kt`, `VehiculoRepository.kt` | Índice `index_vehiculos_matricula` | `VehiculoDaoTest.matriculaDuplicadaLanzaExcepcion`, `RecordatorioTest.repositorioDeVehiculos_detectaMatriculaDuplicada` | TE 5.5 |
| RF-06 | Registrar, editar y eliminar mantenimientos | Formulario con validación cronológica de kilómetros | `ui/mantenimiento/FormMantenimientoFragment.kt`, `FormMantenimientoViewModel.kt`, `domain/validacion/ValidadorMantenimiento.kt`, `data/repository/MantenimientoRepository.kt` | `mantenimientos` | `ValidadorMantenimientoTest`, `MantenimientoDaoTest`, `RecordatorioTest.eliminarElUnicoMantenimiento_eliminaElRecordatorio`, PF-04 | TE 7.2, MU 5 |
| RF-07 | Historial por vehículo con filtro | Lista en el detalle filtrable por tipo | `ui/vehiculos/DetalleVehiculoFragment.kt`, `DetalleVehiculoViewModel.kt`, `ui/mantenimiento/HistorialAdapter.kt` | Consulta `observarPorVehiculo` | `MantenimientoDaoTest.insertarYConsultarHistorialConRelaciones`, PF-05 | TE 5.1, MU 6 |
| RF-08 | Tipos de mantenimiento configurables | Lista, alta, edición y borrado seguro | `ui/tipos/TiposFragment.kt`, `EditarTipoDialogFragment.kt`, `TiposViewModel.kt`, `EditarTipoViewModel.kt`, `domain/validacion/ValidadorTipo.kt` | `tipos_mantenimiento`, triggers de intervalos, FK RESTRICT | `ValidadorTipoTest`, `TipoMantenimientoDaoTest`, `FlujoTiposTest`, PF-11 | TE 5.5, MU 10 |
| RF-09 | Precarga de seis tipos | `RoomDatabase.Callback.onCreate` | `data/db/AppDatabase.kt`, `data/db/DatosIniciales.kt` | `tipos_mantenimiento` | `TipoMantenimientoDaoTest.precargaCreaSeisTipos` | TE 5.2 |
| RF-10 | Cálculo de próximos vencimientos | `CalculadoraMantenimiento` independiente de Android | `domain/CalculadoraMantenimiento.kt` | `recordatorios` (`km_limite`, `fecha_limite`) | `CalculadoraMantenimientoTest`, PF-06 | TE 7.2 |
| RF-11 | Estados verde, ámbar y rojo | Enumeración `EstadoMantenimiento`, insignia con icono y texto | `domain/CalculadoraMantenimiento.kt`, `ui/comun/EstadoUi.kt` | — | `CalculadoraMantenimientoTest`, PF-08 | TE 5.1 y 5.3.1, MU 7 |
| RF-12 | Ausencia de historial | Estado `SIN_REFERENCIA` y acceso a registrar el último mantenimiento conocido | `CalculadoraMantenimiento.kt`, `ui/vehiculos/VencimientosAdapter.kt` | Sin fila en `recordatorios` | `CalculadoraMantenimientoTest.sinReferencia_elEstadoEsSinReferenciaYNoSeInventanLimites` | TE 7.2, MU 13 |
| RF-13 | Control de ITV | Fecha editable, cuenta atrás, estado y actualización al registrar una inspección | `FormVehiculoFragment.kt`, `DetalleVehiculoFragment.kt`, `FormMantenimientoViewModel.kt`, `RecordatorioRepository.kt` | `vehiculos.fecha_itv`, `recordatorios` | `CalculadoraMantenimientoTest.itv_evaluaSoloPorFecha`, `RecordatorioTest.itvRegistrada_actualizaLaFechaDelVehiculoYSuRecordatorio` | TE 7.2, MU 4 |
| RF-14 | Actualización de kilómetros | Diálogo con validación de retrocesos | `ui/vehiculos/ActualizarKmDialogFragment.kt`, `ActualizarKmViewModel.kt`, `domain/validacion/ValidadorKilometros.kt`, `VehiculoRepository.kt` | `vehiculos.km_actuales` | `ValidadorKilometrosTest`, `FlujoPrincipalTest`, PF-07 | TE 7.2, MU 8 |
| RF-15 | Recordatorios sincronizados | Recálculo transaccional tras cada cambio | `data/repository/RecordatorioRepository.kt` | `recordatorios` UNIQUE(`vehiculo_id`,`tipo_id`) | `RecordatorioTest` | TE 7.2 |
| RF-16 | Notificaciones diarias | `CoroutineWorker` periódico, canal propio y permiso de Android 13 | `worker/RevisionVencimientosWorker.kt`, `worker/PlanificadorRevisiones.kt`, `util/Notificaciones.kt`, `MainActivity.kt` | `recordatorios.avisado` | `RevisionVencimientosWorkerTest`, `PlanificadorRevisionesTest`, PF-09 | TE 7.2, MU 11, MI 2.5 |
| RF-17 | Evitar avisos duplicados | Nivel avisado (0, 1, 2) por recordatorio e identificador de notificación estable | `domain/EvaluadorAvisos.kt`, `RevisionVencimientosWorker.kt` | `recordatorios.avisado` | `EvaluadorAvisosTest`, `RevisionVencimientosWorkerTest.itvVencida_seNotificaUnaSolaVez` | TE 7.2 |
| RF-18 | Estadísticas de gastos | Total, por tipo, por vehículo, serie, historial y filtros | `ui/gastos/GastosFragment.kt`, `GastosViewModel.kt`, `GastoAgrupadoAdapter.kt`, `data/repository/GastosRepository.kt` | Consultas agregadas en `MantenimientoDao` | `GastosDaoTest`, PF-10 | TE 7.2, MU 9 |
| RF-19 | Gráfica de barras | MPAndroidChart `BarChart` mensual o anual | `ui/gastos/GraficoGastos.kt` | — | PF-10 (revisión de capturas) | TE 7.2, MU 9 |
| RF-20 | Acceso desde la notificación al detalle | `NavDeepLinkBuilder` hacia `detalleVehiculoFragment` | `util/Notificaciones.kt`, `MainActivity.onNewIntent` | — | PF-13 (prueba manual en emulador) | TE 5.2 y 5.3.5, MU 11 |

## Requisitos no funcionales

| ID | Requisito | Implementación | Pruebas o comprobación | Documentación |
|---|---|---|---|---|
| RNF-01 | Almacenamiento local sin cuenta | Room/SQLite, sin permiso de Internet | Revisión del manifiesto combinado | TE 4.1 |
| RNF-02 | Android 8.0 (API 26) mínimo | `minSdk = 26`, `java.time` nativo | 41 pruebas instrumentadas superadas en un emulador con API 26 | TE 7.5 |
| RNF-03 | Arquitectura MVVM por capas | Paquetes `data`, `domain`, `ui`, `worker`, `util` | Revisión de dependencias; el dominio se prueba en la JVM sin Android | TE 5.2 y 5.2 |
| RNF-04 | Operaciones de datos fuera del hilo principal | Funciones `suspend` y `Flow` de Room | Room impide por defecto el acceso en el hilo principal | TE 5.2 |
| RNF-05 | Consultas parametrizadas | Anotaciones `@Query` con parámetros | Revisión del código | TE 7.2 |
| RNF-06 | Integridad de datos | FK, UNIQUE, triggers y validadores | Pruebas de integración de DAO y repositorios | TE 5.5 |
| RNF-07 | Listas eficientes | `ListAdapter` + `DiffUtil` | Revisión del código | TE 5.2 |
| RNF-08 | Adaptación a pantallas y orientación | `ConstraintLayout`, rejilla en horizontal y tabletas, navegación lateral, tema oscuro | Capturas en vertical, horizontal y tema oscuro; dos resoluciones | TE 7.5 |
| RNF-09 | Accesibilidad del estado | Color + icono + texto | Revisión de capturas | TE 5.1, MU 7 |
| RNF-10 | Textos centralizados | `res/values/strings.xml` y `plurals` | Lint sin errores | TE 5.1 |
| RNF-11 | Permisos mínimos | Solo `POST_NOTIFICATIONS` solicitado; se eliminan permisos no usados añadidos por WorkManager | Manifiesto combinado: `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` | TE 7.2 |
