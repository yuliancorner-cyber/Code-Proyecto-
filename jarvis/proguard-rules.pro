# Reglas para el optimizador R8 en la version de release de JARVIS.
# Room y Compose traen sus propias reglas. Por prudencia conservamos los
# nombres de las tablas (data) y de la logica (se guardan valores como texto,
# p. ej. "DAILY" o "INBOX", y la copia de seguridad usa esos nombres).
-keep class com.jarvis.app.data.** { *; }
-keep class com.jarvis.app.logic.** { *; }

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
