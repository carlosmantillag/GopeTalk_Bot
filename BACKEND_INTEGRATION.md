# Integración con Backend - GopeTalk Bot

## Configuración del Backend

### Endpoint Principal
```
POST http://<backend-host>:8086/audio/ingest
```

### Headers Requeridos
- `Content-Type: audio/wav`
- `X-User-ID: <user_id>` (ID numérico del usuario, ej: "1", "2", etc.)

### Body
Archivo de audio WAV binario (PCM mono 16 kHz recomendado)

## Configuración en Android

### 1. Configurar IP del Backend

En `app/src/main/java/com/example/gopetalk_bot/network/ApiClient.kt`:

```kotlin

// Para dispositivo físico en la misma red
private val backendHost = "192.168.x.x" // IP local de tu máquina

// Para producción
private val backendHost = "tu-dominio.com"
```

### 2. Configurar User ID

En el mismo archivo, actualiza:

```kotlin
private val userId = "1" // Cambiar según el usuario autenticado
```

**Mapeo de usuarios** (según `internal/config/db.go`):
- `usuario-01` → ID: `1`
- `usuario-02` → ID: `2`
- etc.

### 3. Permisos Necesarios

Ya configurados en `AndroidManifest.xml`:
- ✅ `INTERNET` - Para comunicación HTTP
- ✅ `RECORD_AUDIO` - Para grabar audio
- ✅ `usesCleartextTraffic="true"` - Para HTTP (no HTTPS)

## Flujo de Funcionamiento

### 1. Captura de Audio
- `AudioRecordingManager` monitorea continuamente el micrófono
- Detecta voz cuando el nivel supera `silenceThresholdDb` (70 dB)
- Graba hasta detectar 2 segundos de silencio
- Aplica VAD (Voice Activity Detection) para filtrar ruido

### 2. Envío al Backend
```kotlin
// En VoiceInteractionPresenter.kt
private fun sendAudioFileToBackend(audioFile: File) {
    apiService.sendAudioCommand(audioFile, object : ApiService.ApiCallback {
        override fun onSuccess(response: BackendResponse) {
            // Procesar respuesta del backend
        }
        override fun onFailure(e: IOException) {
            // Manejar error
        }
    })
}
```

### 3. Respuestas del Backend

#### Código 200 - Comando Reconocido
El backend procesó el audio como comando y devuelve respuesta textual:
```json
{
  "text": "La lista de canales es: canal-1, canal-2",
  "action": "list_channels",
  "channels": ["canal-1", "canal-2"]
}
```

**Comandos soportados:**
- "dame/tráeme la lista de canales"
- "conéctame al canal-X"
- "salir del canal"
- Confirmaciones: "sí", "no"

#### Código 204 - Audio Relay
El audio no es un comando, se retransmite al canal actual.
La app muestra: "Mensaje enviado al canal"

#### Otros Códigos
- `502/504` - Backend no disponible
- `400` - Request inválido
- `500` - Error interno del servidor

## Pipeline del Backend

Cuando envías audio a `/audio/ingest`:

1. **STT (Speech-to-Text)** - Transcribe el audio
2. **Deepseek AI** - Analiza si es comando o mensaje libre
3. **Ejecución**:
   - Si es comando: ejecuta acción y devuelve respuesta
   - Si no: retransmite audio a usuarios del canal vía WebSocket

## Verificación y Pruebas

### 1. Verificar Backend Activo
```bash
curl http://localhost:8086/health
# O desde Android emulator:
curl http://10.0.2.2:8086/health
```

### 2. Probar Endpoint Manualmente
```bash
curl -X POST http://localhost:8086/audio/ingest \
  -H "Content-Type: audio/wav" \
  -H "X-User-ID: 1" \
  --data-binary @test_audio.wav
```

### 3. Logs del Backend
```bash
# Ver logs del backend
docker logs -f walkie_backend-IA

# Ver logs de STT
docker logs -f stt

# Ver logs de Deepseek
docker logs -f deepseek-local
```

### 4. Logs de Android
En Android Studio, filtrar por:
- `ApiService` - Requests y responses HTTP
- `VoiceInteractionPresenter` - Flujo de interacción
- `AudioRecordingManager` - Captura y VAD

## Troubleshooting

### Error: "Connection refused"
- ✅ Verifica que el backend esté corriendo
- ✅ Usa `10.0.2.2` para emulador, no `localhost`
- ✅ Para dispositivo físico, usa IP local de tu máquina

### Error: "Empty response body"
- ✅ Verifica logs del backend para errores
- ✅ Confirma que el audio sea WAV válido
- ✅ Revisa que el User ID exista en la base de datos

### Audio no se detecta
- ✅ Ajusta `silenceThresholdDb` en `AudioRecordingManager.kt`
- ✅ Verifica permisos de micrófono
- ✅ Revisa logs: "Current audio level (dB): X"

### Backend tarda mucho
- Primera llamada a Deepseek puede tardar ~60s (carga del modelo)
- Considera precalentar el modelo antes de producción

## Próximos Pasos

1. **Autenticación**: Implementar login y obtener User ID dinámicamente
2. **WebSocket**: Conectar para recibir audio de otros usuarios del canal
3. **UI**: Mostrar estado de conexión y respuestas del backend
4. **Producción**: Cambiar a HTTPS y configurar dominio real

## Referencias

- Backend: `internal/http/handlers/audio.go` - Handler `AudioIngest()`
- Comandos: `executeCommand()` en el mismo archivo
- WebSocket: `internal/http/handlers/ws.go` para relay de audio
