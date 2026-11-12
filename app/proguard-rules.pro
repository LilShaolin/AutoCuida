# Reglas de ofuscación de AutoCuida.
# Room, WorkManager y Navigation incluyen sus propias reglas de consumidor.

# MPAndroidChart utiliza reflexión en algunos renderizadores.
-keep class com.github.mikephil.charting.** { *; }

# Conservar información de línea para trazas de error legibles.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
