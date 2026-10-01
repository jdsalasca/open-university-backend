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

## Revalidación desechable MySQL 8.4 — 1 de octubre de 2026, 05:47 (UTC-5)

Una segunda ejecución de `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verify-mysql-curriculum.ps1` usó Java 25.0.4-tem de SDKMAN y un contenedor MySQL 8.4 sin volumen persistente. Los siete contratos MySQL terminaron sin fallos, errores ni omisiones. Cada medición usó 10.000 filas sintéticas, 10 calentamientos, 50 muestras y concurrencia 1:

| Escenario de repositorio | Filas | Tamaño de página | Promedio | P50 | P95 | P99 nearest-rank |
|---|---:|---:|---:|---:|---:|---:|
| Cola de borradores | 10.000 | 25 | 12,789 ms | 12,682 ms | 13,900 ms | 14,099 ms |
| Currículo sin filtro | 10.000 | 100 | 11,149 ms | 11,130 ms | 12,050 ms | 13,161 ms |
| Currículo con filtro por subcadena | 10.000 | 100 | 18,432 ms | 18,005 ms | 21,597 ms | 21,951 ms |

Las tres medias quedan por debajo del presupuesto local de desarrollo `<50 ms`. El script retiró el contenedor desechable y la base persistente de Compose siguió activa. Son consultas del repositorio con datos sintéticos y un cliente; no prueban concurrencia representativa, rendimiento de producción ni un SLA institucional.

## Snapshot público de estructura con volumen sintético — 1 de octubre de 2026

En MySQL Compose 8.4 y Java 25.0.3, el contrato ejecutó 10 calentamientos y 50 muestras secuenciales con 100 unidades, 20 sedes y 1.000 adscripciones sintéticas. `repository-jdbc` promedió 16,083 ms (P50 14,881; P95 24,715; P99 29,440); `mockmvc-json-api` promedió 30,425 ms (P50 29,549; P95 36,099; P99 42,907). Las dos capas quedaron bajo el presupuesto local promedio de 50 ms. El fixture se revirtió. Este perfil de una máquina, una corrida y concurrencia 1 no representa tamaños de UPTC, tráfico concurrente, red/TLS, navegador, OIDC ni un SLO institucional; el método y sus límites están en [la especificación de rendimiento de estructura](superpowers/specs/2026-10-01-academic-structure-read-performance.md).

La repetición en MySQL 8.4 temporal sin volumen persistente con SDKMAN Java 25.0.4 promedió 12,745 ms en `repository-jdbc` (P50 12,376; P95 16,537; P99 17,218) y 27,597 ms en `mockmvc-json-api` (P50 26,976; P95 29,835; P99 47,407). Los ocho contratos MySQL del script terminaron sin fallos; el test comprobó el rollback de unidades, sedes, relaciones, programas y afiliaciones sintéticos. Las dos corridas siguen siendo de concurrencia 1 en esta máquina.

Una revalidación adicional contra MySQL Compose 8.4 con Java 25.0.3 promedió 14,208 ms en `repository-jdbc` (P50 13,236; P95 18,402; P99 20,906) y 27,675 ms en `mockmvc-json-api` (P50 27,568; P95 30,224; P99 30,927).

La cuarta corrida repitió el script completo sobre un MySQL 8.4 temporal con SDKMAN Java 25.0.4. Los ocho contratos terminaron sin fallos; la prueba verificó rollback de unidades, sedes, programas, afiliaciones y relaciones. `repository-jdbc` promedió 12,201 ms (P50 11,938; P95 14,886; P99 17,337) y `mockmvc-json-api` 26,970 ms (P50 26,812; P95 29,234; P99 30,480). El contenedor temporal se eliminó y Compose permaneció activo y sin filas sintéticas. Las cuatro corridas son perfiles de una máquina y concurrencia 1; además, las lecturas comparten la transacción de prueba que creó el fixture y no incluyen el ciclo de transacción ni el checkout de conexión por solicitud. No certifican un SLO institucional.

## Repetir la corrida

Con Docker Compose arriba:

```powershell
powershell -NoProfile -File .\tools\measure-api-latency.ps1
```

El script imprime JSON y permite ajustar `-BaseUri`, `-Paths`, `-WarmupRequests`, `-Samples` y `-TimeoutSeconds`. Úsalo con rutas de lectura públicas; no acepta tokens ni ejecuta mutaciones.
