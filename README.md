# 🔮 FATUM — Tu Segundo Cerebro para Android

> **Versión:** 1.0 · **Plataforma:** Android 8.0+ (API 26+) · **Lenguaje:** Kotlin + Jetpack Compose

FATUM es una aplicación Android nativa de productividad personal que combina micro-logging de vida, motor de hábitos con rachas, planificador sincronizado con Google Calendar, gestión jerárquica de metas, modo Deep Work con bloqueo de redes sociales y analítica cruzada de datos. Todo local, todo privado, con backup automático cifrado en Google Drive.

---

## 📁 Estructura del proyecto

```
FATUM/
├── app/
│   ├── build.gradle                        # Dependencias y configuración del módulo
│   ├── proguard-rules.pro                  # Reglas ProGuard para release
│   └── src/main/
│       ├── AndroidManifest.xml             # Permisos, servicios, receivers
│       ├── java/com/fatum/
│       │   ├── FatumApplication.kt         # Hilt + WorkManager scheduling
│       │   ├── data/
│       │   │   ├── db/
│       │   │   │   ├── FatumDatabase.kt    # Room database (8 entidades)
│       │   │   │   ├── entities/
│       │   │   │   │   └── Entities.kt     # LogEntity, HabitEntity, GoalEntity…
│       │   │   │   └── dao/
│       │   │   │       └── Daos.kt         # LogDao, MoodDao, HabitDao…
│       │   │   ├── repository/
│       │   │   │   └── Repositories.kt     # LogRepository, HabitRepository…
│       │   │   └── preferences/
│       │   │       └── FatumPreferences.kt # DataStore: cuenta Google, ajustes
│       │   ├── di/
│       │   │   └── DatabaseModule.kt       # Hilt: Room + DAOs
│       │   ├── presentation/
│       │   │   ├── MainActivity.kt         # Actividad única + NavHost
│       │   │   ├── navigation/
│       │   │   │   └── Navigation.kt       # Rutas y bottom nav
│       │   │   ├── theme/
│       │   │   │   └── Theme.kt            # Colores, tipografía, dark mode
│       │   │   ├── components/
│       │   │   │   └── SharedComponents.kt # PriorityStars, MoodSelector, HeatMap…
│       │   │   ├── viewmodels/
│       │   │   │   └── ViewModels.kt       # Home, Habits, Planner, Goals, Focus, Analytics
│       │   │   └── screens/
│       │   │       ├── home/HomeScreen.kt
│       │   │       ├── habits/HabitsScreen.kt
│       │   │       ├── planner/PlannerScreen.kt
│       │   │       ├── goals/GoalsScreen.kt
│       │   │       ├── focus/FocusScreen.kt
│       │   │       └── analytics/AnalyticsScreen.kt
│       │   ├── services/
│       │   │   └── Services.kt             # AppBlockerOverlayService + UsageMonitorService
│       │   ├── workers/
│       │   │   └── Workers.kt              # BackupWorker + StreakCheckWorker + BootReceiver
│       │   └── widgets/
│       │       └── FatumWidget.kt          # Glance AppWidget (racha + evento + botón)
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           ├── xml/
│           │   ├── fatum_widget_info.xml
│           │   ├── backup_rules.xml
│           │   └── data_extraction_rules.xml
│           └── layout/
│               └── widget_preview.xml
├── build.gradle                            # Plugins del proyecto
└── settings.gradle                         # Nombre del proyecto e includes
```

---

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM + Repository pattern |
| Base de datos | Room (SQLite) |
| Inyección de dependencias | Hilt (Dagger) |
| Tareas en segundo plano | WorkManager |
| Widgets | Jetpack Glance |
| Preferencias | DataStore |
| Google APIs | Sign-In, Calendar v3, Drive v3 |
| Bloqueo de apps | UsageStatsManager + SYSTEM_ALERT_WINDOW |

---

## 🔑 Configuración previa (OBLIGATORIA antes de compilar)

### 1. Crear proyecto en Google Cloud Console

1. Ve a [console.cloud.google.com](https://console.cloud.google.com)
2. Crea un nuevo proyecto → nómbralo `FATUM`
3. En **APIs y Servicios → Biblioteca**, activa:
   - `Google Calendar API`
   - `Google Drive API`
   - `Google Sign-In`
4. Ve a **APIs y Servicios → Credenciales → Crear credencial → ID de cliente OAuth 2.0**
5. Tipo de aplicación: **Android**
6. Nombre del paquete: `com.fatum`
7. Huella digital SHA-1: obtenla con el comando de abajo
8. Descarga el archivo `google-services.json` y colócalo en `app/`

#### Obtener SHA-1 del keystore de debug:
```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

### 2. Añadir google-services.json
```
app/
└── google-services.json   ← aquí
```

> ⚠️ Sin este fichero la app no compilará porque el plugin de Google Services lo requiere. Añade `id 'com.google.gms.google-services'` al `app/build.gradle` y `classpath 'com.google.gms:google-services:4.4.1'` al `build.gradle` raíz una vez tengas el archivo.

---

## 🚀 Cómo compilar y generar la APK

### Requisitos del sistema

| Herramienta | Versión mínima |
|---|---|
| Android Studio | Hedgehog 2023.1.1+ |
| JDK | 17 |
| Android SDK | API 34 |
| Gradle | 8.2 |

### Pasos para obtener la APK de debug

#### Opción A — Android Studio (recomendado)

1. **Abre** Android Studio → `File → Open` → selecciona la carpeta `FATUM/`
2. Espera a que **Gradle Sync** termine (puede tardar 2–5 min la primera vez descargando dependencias)
3. Conecta tu teléfono Android por USB con **Depuración USB activada**:
   - `Ajustes → Acerca del teléfono → Número de compilación` (pulsa 7 veces)
   - `Ajustes → Opciones para desarrolladores → Depuración USB` ✓
4. Selecciona tu dispositivo en el desplegable superior
5. Pulsa **▶ Run** (o `Shift+F10`)
6. La APK se instala automáticamente en el teléfono

#### Opción B — Línea de comandos

```bash
# 1. Clona / copia el proyecto al ordenador
cd /ruta/al/proyecto/FATUM

# 2. Otorga permisos al wrapper de Gradle
chmod +x gradlew

# 3. Compila el APK de debug
./gradlew assembleDebug

# La APK se genera en:
# app/build/outputs/apk/debug/app-debug.apk

# 4. Instala directamente en el teléfono (con USB o ADB WiFi)
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Compilar APK de Release (firmada)

```bash
# 1. Crear un keystore de producción (solo la primera vez)
keytool -genkey -v \
  -keystore fatum-release.keystore \
  -alias fatum \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# 2. Añadir al app/build.gradle:
# android {
#   signingConfigs {
#     release {
#       storeFile file("fatum-release.keystore")
#       storePassword "TU_CONTRASEÑA"
#       keyAlias "fatum"
#       keyPassword "TU_CONTRASEÑA"
#     }
#   }
#   buildTypes {
#     release { signingConfig signingConfigs.release }
#   }
# }

# 3. Compilar
./gradlew assembleRelease

# APK en: app/build/outputs/apk/release/app-release.apk
```

---

## ⚙️ Permisos de Android que debes aceptar en el teléfono

Al instalar la app, se pedirán los siguientes permisos especiales:

| Permiso | Cómo activarlo | Para qué sirve |
|---|---|---|
| **Uso de apps** | `Ajustes → Apps → Acceso especial → Acceso de uso` → activa FATUM | Detectar apps bloqueadas (Deep Work) |
| **Mostrar sobre otras apps** | `Ajustes → Apps → FATUM → Mostrar sobre otras apps` ✓ | Superponer la pantalla de bloqueo |
| **Notificaciones** | Se pide al primer inicio | Alertas de racha a las 22:00 |
| **Alarmas exactas** | `Ajustes → Apps → FATUM → Alarmas y recordatorios` ✓ | WorkManager a las 03:00 |

---

## 🗄️ Base de datos (Room / SQLite)

8 tablas relacionadas. El archivo SQLite se almacena en:
```
/data/data/com.fatum/databases/fatum.db
```

| Tabla | Descripción |
|---|---|
| `logs` | Entradas del diario con #hashtags y marca de tiempo |
| `daily_mood` | Puntuación de humor 1–10 (una por día, PK = fecha) |
| `habits` | Definición de hábitos y rachas cacheadas |
| `habit_executions` | Registro de cada cumplimiento (retroactivo) |
| `goals` | Metas a largo plazo con % de progreso automático |
| `tasks` | Tareas (hijo de goal o huérfanas) |
| `calendar_events` | Caché local de Google Calendar + eventos propios |
| `focus_sessions` | Historial de sesiones Deep Work con estado |

---

## 🔒 Privacidad y datos

- **100% local por defecto** — ningún dato sale del teléfono sin tu cuenta Google
- **Backup cifrado** — el archivo ZIP de la BD se sube a `appDataFolder` de Drive (invisible para otras apps, no consume cuota pública)
- **Sin backend propio** — FATUM no tiene servidores; las únicas conexiones externas son a las APIs de Google

---

## 🧪 Tests

```bash
# Tests unitarios
./gradlew test

# Tests de instrumentación (requiere dispositivo/emulador)
./gradlew connectedAndroidTest
```

---

## 🗺️ Hoja de ruta

- [ ] Sincronización bidireccional completa con Google Calendar (actualmente local + caché)
- [ ] Gráfico de correlación interactivo con regresión lineal (RF-6.2)
- [ ] Exportación de datos a CSV / JSON
- [ ] Widgets adicionales (hábitos del día, estado de metas)
- [ ] Soporte para cuentas múltiples

---

## 📄 Licencia

Uso personal y privado. Todos los derechos reservados.
