# Reglas para el optimizador R8 en la version de release.
#
# Casi todo se configura solo:
#  - Activities y servicios declarados en el AndroidManifest se conservan siempre.
#  - Room, Compose, CameraX y las demas librerias AndroidX traen sus propias reglas.
#
# Por prudencia conservamos ademas los nombres de las entidades de Room y de
# los estados de sesion (se usan como claves y en el historial).
-keep class com.focuszone.app.datos.** { *; }
-keep class com.focuszone.app.logica.** { *; }

# Mantener numeros de linea en los errores, para poder entenderlos si algun dia
# hay que leer un informe de error de la version final.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
