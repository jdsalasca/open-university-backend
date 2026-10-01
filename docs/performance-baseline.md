# Línea base local de latencia

## Medición del 1 de octubre de 2026

Se midieron las rutas públicas del snapshot académico y de periodos mediante Docker Compose local: backend Spring Boot con Temurin 25 y MySQL 8.4. El snapshot consultado estaba vacío. El cliente reutilizó una conexión HTTP, hizo 15 solicitudes de calentamiento y luego 100 solicitudes secuenciales por ruta. Cada muestra mide desde antes de `GET` hasta que se leyó el cuerpo completo.

| Endpoint | Muestras | Promedio | Mediana | P95 | Máximo |
|---|---:|---:|---:|---:|---:|
| `/api/v1/academic-structure` | 100 | 5,38 ms | 4,81 ms | 9,36 ms | 14,25 ms |
| `/api/v1/academic-periods` | 100 | 2,41 ms | 2,23 ms | 3,06 ms | 6,19 ms |

La media observada en esta corrida local quedó por debajo de 50 ms. Esta línea base no mide carga concurrente, tamaño real del catálogo, equipos institucionales, red de producción, autenticación OIDC ni horas de pico; no establece un SLA ni confirma la meta en producción. Repetirla con datos sintéticos de volumen aprobado y carga concurrente antes de hacer esa afirmación.

## Repetir la corrida

Con Docker Compose arriba:

```powershell
powershell -NoProfile -File .\tools\measure-api-latency.ps1
```

El script imprime JSON y permite ajustar `-BaseUri`, `-Paths`, `-WarmupRequests`, `-Samples` y `-TimeoutSeconds`. Úsalo con rutas de lectura públicas; no acepta tokens ni ejecuta mutaciones.
