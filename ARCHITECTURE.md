# Arquitectura del Proyecto - GopeTalk Bot

## Resumen
Este proyecto sigue los principios de **Clean Architecture** combinado con el patrón **MVP (Model-View-Presenter)** para lograr un código desacoplado, testeable y mantenible.

## Estructura de Capas

```
app/src/main/java/com/example/gopetalk_bot/
│
├── domain/                          # Capa de Dominio (Lógica de Negocio)
│   ├── entities/                    # Entidades del dominio
│   │   ├── AudioData.kt
│   │   ├── AudioLevel.kt
│   │   ├── ApiResponse.kt
│   │   └── PermissionStatus.kt
│   │
│   ├── repositories/                # Interfaces de repositorios
│   │   ├── AudioRepository.kt
│   │   ├── ApiRepository.kt
│   │   ├── TextToSpeechRepository.kt
│   │   └── PermissionRepository.kt
│   │
│   └── usecases/                    # Casos de uso
│       ├── MonitorAudioLevelUseCase.kt
│       ├── StartAudioMonitoringUseCase.kt
│       ├── StopAudioMonitoringUseCase.kt
│       ├── GetRecordedAudioUseCase.kt
│       ├── SendAudioCommandUseCase.kt
│       └── CheckPermissionsUseCase.kt
│
├── data/                            # Capa de Datos
│   ├── datasources/                 # Fuentes de datos
│   │   ├── local/
│   │   │   ├── AudioDataSource.kt
│   │   │   ├── TextToSpeechDataSource.kt
│   │   │   └── PermissionDataSource.kt
│   │   └── remote/
│   │       ├── ApiService.kt
│   │       └── RemoteDataSource.kt
│   │
│   └── repositories/                # Implementaciones de repositorios
│       ├── AudioRepositoryImpl.kt
│       ├── ApiRepositoryImpl.kt
│       ├── TextToSpeechRepositoryImpl.kt
│       └── PermissionRepositoryImpl.kt
│
└── presentation/                    # Capa de Presentación (MVP)
    ├── main/                        # Módulo Main
    │   ├── MainActivity.kt          # View
    │   ├── MainPresenter.kt         # Presenter
    │   └── MainContract.kt          # Contract (View + Presenter interfaces)
    │
    ├── voiceinteraction/            # Módulo Voice Interaction
    │   ├── VoiceInteractionService.kt      # View (Service)
    │   ├── VoiceInteractionPresenter.kt    # Presenter
    │   └── VoiceInteractionContract.kt     # Contract
    │
    └── common/                      # Utilidades comunes de presentación
        └── AudioRmsMonitor.kt
```

## Principios Aplicados

### 1. Clean Architecture
- **Separación de responsabilidades**: Cada capa tiene una responsabilidad clara
- **Regla de dependencia**: Las dependencias apuntan hacia adentro (Presentation → Domain ← Data)
- **Independencia de frameworks**: La lógica de negocio no depende de Android

### 2. MVP (Model-View-Presenter)
- **View**: Activities, Services (solo UI y eventos)
- **Presenter**: Lógica de presentación, orquesta casos de uso
- **Model**: Casos de uso y repositorios (desde Domain y Data)

### 3. SOLID
- **Single Responsibility**: Cada clase tiene una única responsabilidad
- **Open/Closed**: Extensible mediante interfaces
- **Liskov Substitution**: Las implementaciones son intercambiables
- **Interface Segregation**: Interfaces específicas y pequeñas
- **Dependency Inversion**: Dependencias sobre abstracciones (interfaces)

## Flujo de Datos

### Ejemplo: Enviar Audio al Backend

```
1. AudioDataSource detecta audio → emite AudioLevel
2. AudioRepositoryImpl convierte a entidad de dominio
3. GetRecordedAudioUseCase expone Flow<AudioData>
4. VoiceInteractionPresenter observa el Flow
5. Presenter llama SendAudioCommandUseCase
6. ApiRepositoryImpl envía mediante RemoteDataSource
7. Respuesta regresa como ApiResponse (Success/Error)
8. Presenter actualiza la View con el resultado
```

## Ventajas de esta Arquitectura

### ✅ Desacoplamiento
- Las capas no conocen detalles de implementación de otras capas
- Fácil cambiar implementaciones sin afectar otras capas

### ✅ Testabilidad
- Casos de uso pueden testearse sin Android
- Presenters pueden testearse con mocks
- Repositorios pueden testearse con data sources falsos

### ✅ Mantenibilidad
- Código organizado y fácil de navegar
- Cambios localizados en módulos específicos
- Fácil agregar nuevas funcionalidades

### ✅ Escalabilidad
- Estructura clara para agregar nuevos módulos
- Reutilización de casos de uso
- Inyección de dependencias preparada

## Inyección de Dependencias

Actualmente se usa **inyección manual** en:
- `MainActivity.onCreate()`
- `VoiceInteractionService.onCreate()`

### Migración Futura (Recomendado)
Considerar usar **Hilt/Dagger** para:
- Automatizar la creación de dependencias
- Gestionar scopes (Singleton, Activity, Service)
- Reducir boilerplate

## Casos de Uso Principales

### 1. Monitoreo de Audio
- `StartAudioMonitoringUseCase`: Inicia grabación
- `MonitorAudioLevelUseCase`: Observa niveles de audio
- `GetRecordedAudioUseCase`: Obtiene audio grabado
- `StopAudioMonitoringUseCase`: Detiene y libera recursos

### 2. Comunicación con Backend
- `SendAudioCommandUseCase`: Envía audio al servidor

### 3. Permisos
- `CheckPermissionsUseCase`: Verifica permisos requeridos

## Flujo de Permisos

```
MainActivity (View)
    ↓
MainPresenter
    ↓
CheckPermissionsUseCase
    ↓
PermissionRepository
    ↓
PermissionDataSource (Android APIs)
```

## Flujo de Audio

```
AudioDataSource (AudioRecord)
    ↓
AudioRepositoryImpl
    ↓
MonitorAudioLevelUseCase / GetRecordedAudioUseCase
    ↓
VoiceInteractionPresenter
    ↓
SendAudioCommandUseCase
    ↓
ApiRepositoryImpl → RemoteDataSource → Backend
```

## Notas de Implementación

### AudioRmsMonitor
- Singleton en la capa de presentación
- Usado para actualizar UI en tiempo real
- No viola Clean Architecture (es una utilidad de UI)

### Coroutines y Flow
- `Flow` para streams de datos reactivos
- `CoroutineScope` en Presenters para lifecycle management
- `SupervisorJob` para manejar errores sin cancelar todo

### Threading
- Data sources manejan threading internamente
- Presenters usan `Dispatchers.Main` para UI updates
- Callbacks ejecutados en main thread via `Handler`

## Próximos Pasos Sugeridos

1. **Agregar Hilt/Dagger** para DI automática
2. **Agregar Tests Unitarios** para casos de uso
3. **Agregar Tests de Integración** para repositorios
4. **Implementar manejo de errores** más robusto
5. **Agregar logging** estructurado (Timber)
6. **Implementar retry logic** en llamadas API
7. **Agregar analytics** y crash reporting

## Convenciones de Código

- **Naming**: Clases descriptivas (UseCase, Repository, DataSource)
- **Package**: Por feature y por capa
- **Interfaces**: Siempre en domain/repositories
- **Implementations**: Siempre en data/repositories con sufijo `Impl`
- **Entities**: Solo en domain, inmutables (data class)
- **DTOs**: En data/models si se necesitan (actualmente no hay)

---

**Versión**: 1.0  
**Última actualización**: 2025-10-16  
**Autor**: Refactorización Clean Architecture + MVP
