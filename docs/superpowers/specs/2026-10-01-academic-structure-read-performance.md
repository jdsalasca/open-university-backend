# Lectura pública de estructura académica — perfil local MySQL 8.4

**Fecha:** 1 de octubre de 2026<br>
**Estado:** contrato opcional de MySQL; cuatro corridas locales verificadas<br>
**Propósito:** medir la lectura que usa el catálogo para resolver la adscripción académica vigente por `programId`.

## Escenario

`AcademicStructureMySqlPerformanceContractTest` genera filas sintéticas dentro de una transacción: 100 unidades, 20 sedes, relaciones jerárquicas activas y 1.000 programas con 1.000 afiliaciones vigentes. Ejecuta 10 calentamientos y 50 muestras secuenciales por escenario. Los percentiles usan nearest-rank; con 50 muestras el p99 equivale a la observación máxima. La transacción se revierte y el test comprueba que no queden programas, unidades, sedes, afiliaciones ni relaciones sintéticos.

Se cronometran dos capas por separado:

- `repository-jdbc`: llamada de servicio sobre MySQL/JDBC y materialización del snapshot completo (cinco lecturas JDBC).
- `mockmvc-json-api`: `GET /api/v1/academic-structure` con controladores, seguridad, consultas y serialización JSON mediante MockMvc. Incluye MVC, pero no red local/TLS ni transferencia del navegador.

Las inserciones sintéticas y ambas mediciones comparten la transacción de prueba. Por eso los tiempos no incluyen el checkout de conexión ni el inicio/cierre de una transacción nueva por solicitud; `repository-jdbc` mide las lecturas con una transacción ya activa y MockMvc añade el despacho, controlador y serialización dentro de ese mismo límite.

La consulta nueva se añade al catálogo descargando programas publicados y estructura pública en paralelo. Esta medición abarca únicamente la lectura de estructura; no es una medición de navegador ni del tiempo completo de la pantalla.

## Resultado observado

Cuatro corridas locales con MySQL 8.4 y concurrencia 1. Dos usaron la base de Compose y Java 25.0.3. Las otras dos usaron MySQL temporal sin volumen persistente y Java 25.0.4 seleccionado por SDKMAN. Todas las medias cumplen el presupuesto local de desarrollo `<50 ms`.

| Ejecución | Escenario | Unidades | Sedes | Adscripciones | Calentamientos | Muestras | Promedio | P50 | P95 | P99 nearest-rank |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Compose, Java 25.0.3 | Repositorio + JDBC | 100 | 20 | 1.000 | 10 | 50 | 16,083 ms | 14,881 ms | 24,715 ms | 29,440 ms |
| Compose, Java 25.0.3 | JSON API con MockMvc | 100 | 20 | 1.000 | 10 | 50 | 30,425 ms | 29,549 ms | 36,099 ms | 42,907 ms |
| MySQL temporal, SDKMAN Java 25.0.4 | Repositorio + JDBC | 100 | 20 | 1.000 | 10 | 50 | 12,745 ms | 12,376 ms | 16,537 ms | 17,218 ms |
| MySQL temporal, SDKMAN Java 25.0.4 | JSON API con MockMvc | 100 | 20 | 1.000 | 10 | 50 | 27,597 ms | 26,976 ms | 29,835 ms | 47,407 ms |
| Compose, Java 25.0.3 (repetición) | Repositorio + JDBC | 100 | 20 | 1.000 | 10 | 50 | 14,208 ms | 13,236 ms | 18,402 ms | 20,906 ms |
| Compose, Java 25.0.3 (repetición) | JSON API con MockMvc | 100 | 20 | 1.000 | 10 | 50 | 27,675 ms | 27,568 ms | 30,224 ms | 30,927 ms |
| MySQL temporal, SDKMAN Java 25.0.4 (repetición) | Repositorio + JDBC | 100 | 20 | 1.000 | 10 | 50 | 12,201 ms | 11,938 ms | 14,886 ms | 17,337 ms |
| MySQL temporal, SDKMAN Java 25.0.4 (repetición) | JSON API con MockMvc | 100 | 20 | 1.000 | 10 | 50 | 26,970 ms | 26,812 ms | 29,234 ms | 30,480 ms |

Son mediciones de una máquina con fixture sintético y concurrencia 1. El conjunto no representa los tamaños de UPTC; no incluye carga concurrente, red, TLS, navegador, OIDC ni hardware institucional. Cuatro corridas no acreditan SLA productivo ni estabilidad estadística de p99. Repetir con el volumen y mezcla de carga que acuerden las áreas institucionales antes de formular una meta de producción.

## Repetir

Desde PowerShell, con Docker disponible:

```powershell
powershell -NoProfile -File .\tools\verify-mysql-curriculum.ps1
```

El script inicia MySQL 8.4 desechable sin volumen persistente, selecciona Java desde `.sdkmanrc` con SDKMAN, ejecuta el contrato de catálogo, periodos y estructura, y elimina el contenedor temporal. La suite CI activa los contratos MySQL con `universiry.mysql-contract.enabled=true`.
