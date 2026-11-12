# Matriz de requisitos de AutoCuida

Versión del producto: **1.0.0** (`versionCode 1`).

Esta matriz vincula cada requisito funcional y no funcional con su implementación, los archivos afectados, los elementos de base de datos implicados, las pruebas asociadas y el lugar de la documentación en el que se describe. Las rutas de código son relativas a `app/src/main/java/com/autocuida/app/`.

Abreviaturas de documentación: **IT** = informe técnico (capítulo), **MU** = manual de usuario, **MI** = manual de instalación.

## Requisitos funcionales

| ID | Requisito | Implementación | Archivos principales | Base de datos | Pruebas | Documentación |
|---|---|---|---|---|---|---|
| RF-01 | Registrar vehículos | Formulario con validación y normalización de matrícula | `ui/vehiculos/FormVehiculoFragment.kt`, `FormVehiculoViewModel.kt`, `domain/validacion/ValidadorVehiculo.kt`, `data/repository/VehiculoRepository.kt` | `vehiculos` (INSERT) | `ValidadorVehiculoTest`, `VehiculoDaoTest`, PF-01 | IT 5 y 7, MU 2 |
| RF-02 | Consultar vehículos | Lista observable con estado general | `ui/vehiculos/ListaVehiculosFragment.kt`, `ListaVehiculosViewModel.kt`, `VehiculosAdapter.kt` | `vehiculos`, `recordatorios` (Flow) | `VehiculoDaoTest`, PF-02 | IT 5, MU 1 |
| RF-03 | Editar vehículos | Reutilización del formulario en modo edición | `FormVehiculoFragment.kt`, `FormVehiculoViewModel.kt` | `vehiculos` (UPDATE) | `VehiculoDaoTest`, PF-03 | MU 3 |
| RF-04 | Eliminar vehículos en cascada | Diálogo de confirmación y `ON DELETE CASCADE` | `ui/vehiculos/DetalleVehiculoFragment.kt`, `data/entity/Mantenimiento.kt`, `Recordatorio.kt` | FK `vehiculo_id` CASCADE | `MantenimientoDaoTest.borrarVehiculoEliminaEnCascada`, PF-12 | IT 5.2, MU 12 |
| RF-05 | Matrícula única | Índice UNIQUE y comprobación previa | `data/entity/Vehiculo.kt`, `VehiculoRepository.kt` | Índice `index_vehiculos_matricula` | `VehiculoDaoTest.matriculaDuplicadaLanzaExcepcion`, PF-04 | IT 5.2 |
| RF-06 | Registrar, editar y eliminar mantenimientos | Formulario con validación cronológica de kilómetros | `ui/mantenimiento/FormMantenimientoFragment.kt`, `FormMantenimientoViewModel.kt`, `domain/validacion/ValidadorMantenimiento.kt`, `data/repository/MantenimientoRepository.kt` | `mantenimientos` | `ValidadorMantenimientoTest`, `MantenimientoDaoTest`, PF-05 | IT 7, MU 5 |
| RF-07 | Historial por vehículo con filtro | Lista en el detalle filtrable por tipo | `ui/vehiculos/DetalleVehiculoFragment.kt`, `ui/mantenimiento/HistorialAdapter.kt` | Consulta `observarPorVehiculo` | `MantenimientoDaoTest`, PF-06 | MU 6 |
| RF-08 | Tipos de mantenimiento configurables | Lista, alta, edición y borrado seguro | `ui/tipos/TiposFragment.kt`, `EditarTipoDialogFragment.kt`, `TiposViewModel.kt`, `domain/validacion/ValidadorTipo.kt` | `tipos_mantenimiento`, trigger de intervalos, FK RESTRICT | `ValidadorTipoTest`, `TipoMantenimientoDaoTest`, PF-11 | IT 5.2, MU 10 |
| RF-09 | Precarga de seis tipos | `RoomDatabase.Callback.onCreate` | `data/db/AppDatabase.kt`, `data/db/DatosIniciales.kt` | `tipos_mantenimiento` | `TipoMantenimientoDaoTest.precargaCreaSeisTipos` | IT 5.2 |
| RF-10 | Cálculo de próximos vencimientos | `CalculadoraMantenimiento` independiente de Android | `domain/CalculadoraMantenimiento.kt` | `recordatorios` (`km_limite`, `fecha_limite`) | `CalculadoraMantenimientoTest` | IT 7.3 |
| RF-11 | Estados verde, ámbar y rojo | Enumeración `EstadoMantenimiento` + icono y texto | `domain/CalculadoraMantenimiento.kt`, `ui/comun/EstadoUi.kt` | — | `CalculadoraMantenimientoTest` | IT 7.3, MU 7 |
| RF-12 | Ausencia de historial | Estado `SIN_REFERENCIA` y acceso a registrar el último mantenimiento conocido | `CalculadoraMantenimiento.kt`, `ui/vehiculos/VencimientosAdapter.kt` | Sin fila en `recordatorios` | `CalculadoraMantenimientoTest.sinReferencia*`, PF-07 | MU FAQ |
| RF-13 | Control de ITV | Fecha editable, cuenta atrás, estado y actualización al registrar una inspección | `FormVehiculoFragment.kt`, `FormMantenimientoFragment.kt`, `RecordatorioRepository.kt` | `vehiculos.fecha_itv`, `recordatorios` | `CalculadoraMantenimientoTest.itv*`, `RecordatorioRepositoryTest`, PF-08 | IT 7.4, MU 4 |
| RF-14 | Actualización de kilómetros | Diálogo con validación de retrocesos | `ui/vehiculos/ActualizarKmDialogFragment.kt`, `domain/validacion/ValidadorKilometros.kt`, `VehiculoRepository.kt` | `vehiculos.km_actuales` | `ValidadorKilometrosTest`, PF-09 | IT 7.4, MU 8 |
| RF-15 | Recordatorios sincronizados | Recálculo transaccional tras cada cambio | `data/repository/RecordatorioRepository.kt` | `recordatorios` UNIQUE(`vehiculo_id`,`tipo_id`) | `RecordatorioRepositoryTest`, `RecordatorioDaoTest` | IT 7.5 |
| RF-16 | Notificaciones diarias | `CoroutineWorker` periódico + canal + permiso Android 13 | `worker/RevisionVencimientosWorker.kt`, `worker/PlanificadorRevisiones.kt`, `util/Notificaciones.kt`, `MainActivity.kt` | `recordatorios.avisado` | `EvaluadorAvisosTest`, PF-10 | IT 7.5, MU 11 |
| RF-17 | Evitar avisos duplicados | Nivel avisado (0, 1, 2) por recordatorio e ID de notificación estable | `domain/EvaluadorAvisos.kt`, `RevisionVencimientosWorker.kt` | `recordatorios.avisado` | `EvaluadorAvisosTest` | IT 7.5 |
| RF-18 | Estadísticas de gastos | Total, por tipo, por vehículo, mensual, historial y filtros | `ui/gastos/GastosFragment.kt`, `GastosViewModel.kt`, `data/repository/GastosRepository.kt` | Consultas agregadas en `MantenimientoDao` | `GastosDaoTest`, PF-13 | IT 7.6, MU 9 |
| RF-19 | Gráfica de barras | MPAndroidChart `BarChart` mensual | `ui/gastos/GraficoGastos.kt` | — | PF-13 | IT 7.6 |
| RF-20 | Acceso desde la notificación al detalle | `NavDeepLinkBuilder` hacia `detalleVehiculoFragment` | `util/Notificaciones.kt` | — | PF-10 | IT 7.5 |

## Requisitos no funcionales

| ID | Requisito | Implementación | Pruebas | Documentación |
|---|---|---|---|---|
| RNF-01 | Almacenamiento local sin cuenta | Room/SQLite, sin permiso de Internet | Revisión del manifiesto | IT 6 |
| RNF-02 | Android 8.0 (API 26) mínimo | `minSdk = 26`, `java.time` nativo | Compilación; prueba en API 26 pendiente si no hay imagen | IT 6.1 |
| RNF-03 | Arquitectura MVVM por capas | Paquetes `data`, `domain`, `ui`, `worker`, `util` | Revisión de dependencias entre paquetes | IT 6.2 |
| RNF-04 | Operaciones de datos fuera del hilo principal | Funciones `suspend` y `Flow` de Room | Room lanza excepción si se accede en el hilo principal | IT 6.3 |
| RNF-05 | Consultas parametrizadas | Anotaciones `@Query` con parámetros | Revisión del código | IT 6.3 |
| RNF-06 | Integridad de datos | FK, UNIQUE, triggers y validadores | Pruebas de integración | IT 5.2 |
| RNF-07 | Listas eficientes | `ListAdapter` + `DiffUtil` | Revisión | IT 6.3 |
| RNF-08 | Adaptación a pantallas y orientación | Layouts con `ConstraintLayout`, rejilla en horizontal y tabletas | Emulador vertical/horizontal | IT 8.4 |
| RNF-09 | Accesibilidad del estado | Color + icono + texto | Revisión visual | IT 5.1 |
| RNF-10 | Textos centralizados | `res/values/strings.xml` y `plurals` | Lint | IT 6 |
| RNF-11 | Permisos mínimos | Solo `POST_NOTIFICATIONS` | Revisión del manifiesto | IT 7.5 |
