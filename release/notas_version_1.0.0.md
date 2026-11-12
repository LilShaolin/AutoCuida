# AutoCuida 1.0.0

Primera versión de AutoCuida, aplicación Android para gestionar el mantenimiento, la ITV y los gastos de tus vehículos.

## Novedades

- Gestión de varios vehículos con matrícula única.
- Registro, edición e historial filtrable de mantenimientos.
- Seis tipos de mantenimiento iniciales con intervalos configurables por kilómetros, por meses o ambos.
- Cálculo de vencimientos con estados verde, ámbar y rojo, y estado «Sin referencia» cuando no hay historial.
- Control de la ITV con cuenta atrás y actualización tras cada inspección.
- Actualización validada del cuentakilómetros.
- Notificaciones diarias sin duplicados que abren el detalle del vehículo.
- Estadísticas de gastos por año, tipo y vehículo con gráfica de barras.
- Tema claro y oscuro, orientación vertical y horizontal.

## Archivo

| Archivo | Tamaño | SHA-256 |
|---|---|---|
| `AutoCuida-1.0.0.apk` | 2.411.736 bytes (2,4 MB) | `7dce9a4e7fc2f0bdc3d5b38e1808d8ffaad1eb84ba6532418da00778b2ead329` |

Requisitos: Android 8.0 o superior. Instrucciones de instalación en `documentacion/manual_instalacion`.

## Publicación en GitHub (pasos pendientes)

1. Crear el repositorio público y subir la rama `main` (`git remote add origin <url>` y `git push -u origin main`).
2. Crear la etiqueta: `git tag -a v1.0.0 -m "AutoCuida 1.0.0"` y `git push origin v1.0.0`.
3. En GitHub, **Releases › Draft a new release**, elegir la etiqueta `v1.0.0`, pegar estas notas y adjuntar `AutoCuida-1.0.0.apk` (el archivo APK no se versiona en el repositorio).
4. Sustituir `[URL del repositorio en GitHub]` en la diapositiva 12 y en el README por la dirección real.
