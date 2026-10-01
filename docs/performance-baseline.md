# Línea base local de latencia

## Medición del 1 de octubre de 2026

Se midieron las rutas públicas del snapshot académico y de periodos mediante Docker Compose local: backend Spring Boot con Temurin 25 y MySQL 8.4. El snapshot consultado estaba vacío. El cliente reutilizó una conexión HTTP, hizo 15 solicitudes de calentamiento y luego 100 solicitudes secuenciales por ruta. Cada muestra mide desde antes de `GET` hasta que se leyó el cuerpo completo.

| Endpoint | Muestras | Promedio | Mediana | P95 | Máximo |
|---|---:|---:|---:|---:|---:|
| `/api/v1/academic-structure` | 100 | 5,38 ms | 4,81 ms | 9,36 ms | 14,25 ms |
| `/api/v1/academic-periods` | 100 | 2,41 ms | 2,23 ms | 3,06 ms | 6,19 ms |

La media observada en esta corrida local quedó por debajo de 50 ms. Esta línea base no mide carga concurrente, tamaño real del catálogo, equipos institucionales, red de producción, autenticación OIDC ni horas de pico; no establece un SLA ni confirma la meta en producción. Repetirla con datos sintéticos de volumen aprobado y carga concurrente antes de hacer esa afirmación.

## Contrato MySQL del catálogo — 1 de octubre de 2026

Se ejecutó `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify-mysql-curriculum.ps1`. El script inició MySQL 8.4 temporal, sin volumen persistente, seleccionó Java 25.0.4-tem desde SDKMAN y retiró el contenedor al terminar. Flyway validó y aplicó 14 migraciones; 7 pruebas de contrato terminaron sin fallos ni omisiones. Las tres consultas medidas trabajaron con 10.000 filas sintéticas, 10 calentamientos, 50 muestras y concurrencia 1:

| Escenario de repositorio | Filas | Tamaño de página | Promedio | P50 | P95 | P99 nearest-rank |
|---|---:|---:|---:|---:|---:|---:|
| Cola de borradores | 10.000 | 25 | 13,185 ms | 12,951 ms | 15,176 ms | 20,097 ms |
| Currículo sin filtro | 10.000 | 100 | 10,789 ms | 10,885 ms | 11,825 ms | 11,978 ms |
| Currículo con filtro por subcadena | 10.000 | 100 | 17,842 ms | 17,643 ms | 19,256 ms | 20,795 ms |

Estas duraciones rodean las consultas del repositorio dentro de la JVM e incluyen MySQL/JDBC; no son mediciones HTTP ni de navegador y no equivalen a carga concurrente. Las medias de esta corrida cumplen el umbral de desarrollo `<50 ms` para los tres escenarios observados, no un SLO de producción.

## Repetir la corrida

Con Docker Compose arriba:

```powershell
powershell -NoProfile -File .\tools\measure-api-latency.ps1
```

El script imprime JSON y permite ajustar `-BaseUri`, `-Paths`, `-WarmupRequests`, `-Samples` y `-TimeoutSeconds`. Úsalo con rutas de lectura públicas; no acepta tokens ni ejecuta mutaciones.
