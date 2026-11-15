# AutoCuida 1.0.1

Versión de mantenimiento de AutoCuida con correcciones de errores y mejoras de fiabilidad. Se instala encima de la 1.0.0 sin perder los datos.

## Correcciones

- Al editar un vehículo ya no se pueden poner menos kilómetros que los del último mantenimiento registrado, igual que al actualizar el cuentakilómetros.
- La fecha elegida en el calendario se guarda aunque se gire la pantalla con el calendario abierto.
- El calendario de la fecha de un mantenimiento ya no permite elegir el día de mañana.
- Registrar una ITV antigua ya no adelanta la fecha de la próxima ITV del vehículo: solo se actualiza si la nueva fecha es posterior.
- Pulsar dos veces seguidas un botón ya no puede cerrar la aplicación ni guardar un mantenimiento repetido.
- Un coste escrito con punto de miles, como «1.500», se interpreta como 1.500 € y no como 1,50 €.
- Las notificaciones desaparecen cuando el mantenimiento vuelve a estar al día o cuando se elimina el vehículo.
- En Gastos, si se elimina el vehículo filtrado, la pantalla vuelve a mostrar todos los vehículos.
- El aviso más urgente de cada vehículo se elige comparando por igual kilómetros y días restantes.
- La copia de seguridad de Android guarda solo los datos de AutoCuida.

## Archivo

| Archivo | Tamaño | SHA-256 |
|---|---|---|
| `AutoCuida-1.0.1.apk` | 2.413.748 bytes (2,4 MB) | `6520e5bba1efb43ca1ddf6bccecd74957d26690a67a17ea11e7f40e220270bc1` |

Requisitos: Android 8.0 o superior. Instrucciones de instalación en `documentacion/manual_instalacion`.

## Publicación

Publicada en https://github.com/LilShaolin/AutoCuida/releases/tag/v1.0.1 con el APK adjunto.
