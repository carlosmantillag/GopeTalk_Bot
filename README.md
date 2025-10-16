# GopeTalk Bot - Voice Interaction App

## 📱 Descripción
Aplicación Android de interacción por voz que detecta comandos de audio, los procesa y los envía a un backend para su análisis.

## 🏗️ Arquitectura

Este proyecto implementa **Clean Architecture + MVP** para lograr un código:
- ✅ **Desacoplado**: Separación clara de responsabilidades
- ✅ **Testeable**: Cada capa puede testearse independientemente
- ✅ **Mantenible**: Fácil de entender y modificar
- ✅ **Escalable**: Preparado para crecer

### Capas de la Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER (MVP)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  MainActivity │  │   Service    │  │  Presenters  │      │
│  │    (View)    │  │    (View)    │  │              │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Entities   │  │  Use Cases   │  │ Repositories │      │
│  │              │  │              │  │ (Interfaces) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Repositories │  │ Data Sources │  │ Data Sources │      │
│  │    (Impl)    │  │   (Local)    │  │   (Remote)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

## 📂 Estructura del Proyecto

```
app/src/main/java/com/example/gopetalk_bot/
│
├── domain/                          # Lógica de Negocio (independiente de Android)
│   ├── entities/                    # Modelos de dominio
│   ├── repositories/                # Contratos de repositorios
│   └── usecases/                    # Casos de uso de la aplicación
│
├── data/                            # Implementación de datos
│   ├── datasources/
│   │   ├── local/                   # Fuentes de datos locales (Android APIs)
│   │   └── remote/                  # Fuentes de datos remotas (API)
│   └── repositories/                # Implementaciones de repositorios
│
└── presentation/                    # Capa de UI (MVP)
    ├── main/                        # Pantalla principal
    ├── voiceinteraction/            # Servicio de voz
    └── common/                      # Utilidades de UI
```

## 🎯 Funcionalidades

- 🎤 **Monitoreo de audio en tiempo real**
- 🔊 **Detección automática de sonido/silencio**
- 📝 **Grabación de comandos de voz**
- 🌐 **Envío de audio al backend**
- 📊 **Visualización de niveles de audio**
- 🔔 **Servicio en foreground persistente**
- 🔐 **Gestión de permisos**

## 🔧 Tecnologías Utilizadas

- **Kotlin** - Lenguaje principal
- **Jetpack Compose** - UI moderna y declarativa
- **Coroutines & Flow** - Programación asíncrona y reactiva
- **Retrofit** - Cliente HTTP para API
- **OkHttp** - Cliente HTTP base
- **Gson** - Serialización JSON
- **Android AudioRecord** - Grabación de audio
- **Android TTS** - Text-to-Speech

## 📋 Requisitos

- Android SDK 24+
- Kotlin 2.0.21+
- Gradle 8.13.0+

## 🚀 Instalación

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd GopeTalk_Bot
```

2. **Abrir en Android Studio**
```
File → Open → Seleccionar carpeta del proyecto
```

3. **Sincronizar Gradle**
```
File → Sync Project with Gradle Files
```

4. **Ejecutar la app**
```
Run → Run 'app'
```

## 🔑 Permisos Requeridos

- `RECORD_AUDIO` - Para grabar comandos de voz
- `INTERNET` - Para comunicación con backend
- `FOREGROUND_SERVICE` - Para servicio persistente
- `FOREGROUND_SERVICE_MICROPHONE` - Para acceso al micrófono en foreground
- `POST_NOTIFICATIONS` - Para notificaciones (Android 13+)

## ⚙️ Configuración

### Backend URL
Editar en `RemoteDataSource.kt`:
```kotlin
private val backendHost = "159.223.150.185"
private val backendPort = 8086
```

### User ID
Editar en `VoiceInteractionPresenter.kt`:
```kotlin
private val userId: String = "1" // Cambiar por ID real
```

### Umbrales de Audio
Editar en `AudioDataSource.kt`:
```kotlin
private val silenceThresholdDb = 70f      // Umbral de silencio
private val silenceTimeoutMs = 2000L      // Timeout de silencio
```

## 🧪 Testing

### Tests Unitarios (Recomendado implementar)
```kotlin
// Ejemplo para Use Cases
class SendAudioCommandUseCaseTest {
    @Test
    fun `should send audio successfully`() {
        // Test implementation
    }
}
```

### Tests de Integración (Recomendado implementar)
```kotlin
// Ejemplo para Repositories
class AudioRepositoryImplTest {
    @Test
    fun `should monitor audio levels`() {
        // Test implementation
    }
}
```

## 📖 Documentación Adicional

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Documentación completa de arquitectura
- **[REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md)** - Resumen de refactorización

## 🔄 Flujo de Datos Principal

```
1. Usuario abre la app
   ↓
2. MainActivity verifica permisos (CheckPermissionsUseCase)
   ↓
3. Si OK → Inicia VoiceInteractionService
   ↓
4. Service inicia monitoreo de audio (StartAudioMonitoringUseCase)
   ↓
5. AudioDataSource detecta niveles de audio
   ↓
6. Cuando detecta sonido → Graba audio
   ↓
7. Cuando detecta silencio → Finaliza grabación
   ↓
8. GetRecordedAudioUseCase emite AudioData
   ↓
9. Presenter envía audio (SendAudioCommandUseCase)
   ↓
10. RemoteDataSource envía a backend vía Retrofit
    ↓
11. Respuesta regresa a Presenter
    ↓
12. UI se actualiza con resultado
```

## 🎨 UI/UX

- **Gradiente animado** de fondo (púrpura a azul)
- **Partículas flotantes** para efecto visual
- **Esfera central** que crece con niveles de audio
- **Animaciones fluidas** con Jetpack Compose

## 🐛 Debugging

### Logs importantes
```kotlin
// Tag: AudioDataSource
"Started audio monitoring"
"Sound detected, starting recording"
"Silence detected, stopping recording"

// Tag: RemoteDataSource
"Sending audio to backend via Retrofit"
"Backend error {code}: {message}"

// Tag: VoiceInteractionService
"Presenter started"
"Audio enviado correctamente"
```

### Verificar servicio activo
```bash
adb shell dumpsys activity services | grep VoiceInteractionService
```

## 🔮 Próximas Mejoras

- [ ] Implementar Hilt/Dagger para DI
- [ ] Agregar tests unitarios
- [ ] Agregar tests de integración
- [ ] Implementar retry logic en API calls
- [ ] Agregar manejo de errores robusto
- [ ] Implementar logging con Timber
- [ ] Agregar analytics y crash reporting
- [ ] Soporte para múltiples idiomas
- [ ] Configuración de backend desde UI
- [ ] Historial de comandos

## 👥 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la licencia MIT.

## 📞 Contacto

Para preguntas o soporte, contactar al equipo de desarrollo.

---

**Versión**: 1.0  
**Última actualización**: 2025-10-16  
**Estado**: ✅ Producción
