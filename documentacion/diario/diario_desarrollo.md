# Diario de desarrollo de AutoCuida

Registro de las tareas realizadas, los problemas encontrados y las soluciones aplicadas. Los tiempos son aproximados. La tabla final queda preparada para seguir registrando entradas en futuras versiones.

## Entradas registradas

| Fecha | Tarea | Problema encontrado | Solución | Tiempo empleado |
|---|---|---|---|---|
| 12/11/2026 | Configuración de Gradle (AGP 9.2.0, KSP, Room) | `settings.gradle.kts` no compilaba: «Unsupported escape sequence» en las expresiones regulares de los repositorios. | Escapar la barra invertida (`"com\\.android.*"`). | 5 min |
| 12/11/2026 | Firma de la versión release | Dentro del bloque `android {}`, `java.util.Properties` se resolvía contra la extensión `java` de Gradle. | Importar `java.util.Properties` al inicio del script y definir `signingConfigs` fuera de `buildTypes`. | 5 min |
| 12/11/2026 | Sistema de diseño (estilos) | Error de enlace de recursos: `attr/fontFeatureSettings` no encontrado. | Usar el atributo de plataforma `android:fontFeatureSettings`. | 3 min |
| 12/11/2026 | Diálogos (actualizar km, editar tipo) | Un `DialogFragment` que crea un `AlertDialog` y devuelve además una vista en `onCreateView` provoca que la vista tenga dos padres. | Observar el estado con el ciclo de vida del propio fragmento (`recolectarEnDialogo`) sin sobrescribir `onCreateView`. | 10 min |
| 12/11/2026 | Prueba Espresso del flujo principal | El mensaje de confirmación (Snackbar) tapaba el botón flotante de la pantalla siguiente, que no recibía el clic. | Anclar el Snackbar al botón flotante o a la barra inferior visible tras la navegación. | 10 min |
| 12/11/2026 | Prueba Espresso del flujo principal | Las listas de «Próximos vencimientos» e «Historial» aparecían vacías aunque había datos: los `RecyclerView` anidados no tenían `LayoutManager`. Detectado con una captura de pantalla tomada al fallar la prueba. | Declarar `app:layoutManager` en los layouts del detalle, gastos y tipos. | 10 min |
| 12/11/2026 | Prueba Espresso del flujo principal | Las comprobaciones se ejecutaban antes de que Room terminase la escritura en su hilo de fondo. | Función de reintento acotado (`esperar`) en la prueba de interfaz. | 5 min |
| 12/11/2026 | Prueba Espresso del flujo principal | El importe «75,50 €» no coincidía: el formato español de moneda usa un espacio no separable antes de «€». | Comparar con el texto generado por `Formatos.euros()`. | 3 min |
| 12/11/2026 | Revisión de capturas | Tras cerrar el diálogo de kilómetros, la barra superior volvía a mostrar «Detalle del vehículo» en lugar del nombre del vehículo. | Restablecer el título en un `OnDestinationChangedListener` del fragmento. | 8 min |
| 12/11/2026 | Análisis estático (Lint) | 1 error (`PropertyEscape` en `local.properties`) y avisos de plurales, recursos sin uso, accesibilidad y simetría RTL. | Corregir el archivo, convertir textos a `plurals`, eliminar recursos sin uso y completar atributos. Resultado: 0 errores. | 10 min |
| 12/11/2026 | Compatibilidad con Android 8.0 (API 26) | No había imagen de sistema de API 26 instalada. | Descarga de la imagen oficial `x86_64-26_r16` desde el repositorio del SDK de Google y creación de un AVD. | 10 min |
| 12/11/2026 | Compatibilidad con Android 8.0 (API 26) | La prueba que toma las capturas de la aplicación fallaba porque el permiso `POST_NOTIFICATIONS` no existe en API 26. | Conceder el permiso solo en API 33 o superior. Las 38 pruebas del producto se superan en API 26. | 5 min |
| 15/11/2026 | Revisión final del código | Al editar un vehículo se podían bajar los kilómetros por debajo del mayor kilometraje del historial. | Validar con el mismo criterio que el diálogo «Actualizar km». | 10 min |
| 15/11/2026 | Revisión final del código | Tras girar la pantalla con el selector de fecha abierto, la fecha elegida no se aplicaba. | Etiqueta propia por campo y volver a enlazar el selector al recrear la vista. | 10 min |
| 15/11/2026 | Revisión final del código | Registrar una ITV antigua adelantaba la fecha de la próxima ITV del vehículo. | Actualizarla solo si la nueva fecha es posterior a la guardada. | 5 min |
| 15/11/2026 | Revisión final del código | Una doble pulsación rápida podía navegar dos veces o guardar dos registros, y un coste «1.500» se leía como 1,50 €. | Comprobar el destino antes de navegar, bloquear el botón mientras se guarda y tratar el punto seguido de tres cifras como separador de miles. | 15 min |
| 15/11/2026 | Revisión final del código | Las notificaciones de un vehículo borrado o de un aviso ya resuelto seguían en la bandeja. | Retirar en cada revisión las notificaciones que ya no corresponden. Publicar la versión 1.0.1. | 15 min |

## Plantilla para nuevas entradas

| Fecha | Tarea | Problema encontrado | Solución | Tiempo empleado |
|---|---|---|---|---|
| | | | | |
| | | | | |
| | | | | |
