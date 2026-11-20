# AutoCuida 1.0.2

Versión de mantenimiento de AutoCuida con correcciones de errores y mejoras de fiabilidad y accesibilidad. Se instala encima de la 1.0.1 sin perder los datos.

## Correcciones

- Un vehículo sin ningún mantenimiento registrado ya no aparece como «Al día»: muestra «Sin datos» en la lista y «Registra mantenimientos para calcular vencimientos» en el detalle. La ITV conserva su propio estado y su fecha.
- El resumen de la pantalla principal cuenta los avisos pendientes de verdad (cada mantenimiento o ITV próximo o vencido), no los vehículos con avisos.
- Si al registrar una ITV la próxima fecha indicada no es posterior a la que ya tiene el vehículo, se avisa de que esa fecha se mantiene y de que se puede cambiar con «Cambiar fecha».
- Al borrar el registro de una ITV, el aviso de confirmación explica que la fecha de la próxima ITV del vehículo no cambia.
- La próxima ITV que eliges o borras a mano ya no se sustituye por la propuesta al cambiar la fecha de la inspección.
- Al editar un vehículo no se pueden poner menos kilómetros que los actuales; para corregir una lectura errónea se usa «Actualizar km».
- Un mantenimiento con fecha anterior a hoy no puede tener más kilómetros que los actuales del vehículo, para que un error al teclear no adelante el cuentakilómetros.
- En el tipo ITV solo se configura el intervalo en meses, que es el único que se usa.
- Ya no se pueden crear dos tipos con el mismo nombre escrito con distintas mayúsculas o tildes, como «Neumáticos» y «NEUMATICOS».
- Si las notificaciones de vencimientos están desactivadas en los ajustes de Android, el aviso no se da por enviado y llega cuando se vuelven a activar.
- Al conceder el permiso de notificaciones se revisan los vencimientos en ese momento, sin esperar a la revisión diaria.
- El mensaje de permiso de notificaciones denegado ya no aparece en cada arranque.
- Un mismo aviso repetido no vuelve a sonar; el paso de próximo a vencido sí avisa.
- Los títulos de las notificaciones son más claros: «Mantenimiento próximo: Frenos» o «Mantenimiento vencido: Neumáticos».
- Los estados y los días restantes se actualizan al cambiar de día aunque la aplicación siga abierta, y se respeta el cambio de zona horaria.
- Al abrir la notificación de un vehículo que ya se ha eliminado, se indica que el vehículo ya no existe.
- Los formularios conservan la fecha, el tipo y los datos escritos si Android cierra la aplicación en segundo plano.
- Los lectores de pantalla leen correctamente el número de avisos pendientes de la pantalla principal.

## Archivo

| Archivo | Tamaño | SHA-256 |
|---|---|---|
| `AutoCuida-1.0.2.apk` | 2.421.344 bytes (2,4 MB) | `b2cd69559edfa0e5eaac840bee64c663817cd897bf7bfa60d2dd55397d728841` |

Requisitos: Android 8.0 o superior. Instrucciones de instalación en `documentacion/manual_instalacion`.

## Publicación

Publicada en https://github.com/LilShaolin/AutoCuida/releases/tag/v1.0.2 con el APK adjunto.
