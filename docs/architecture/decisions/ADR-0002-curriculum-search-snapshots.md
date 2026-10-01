# ADR-0002: Proyección de búsqueda para entradas curriculares

- **Estado:** Aceptado para el preview v0
- **Fecha:** 2026-09-30
- **Alcance:** Consultas públicas paginadas del módulo `academics`

## Contexto

El contrato mantiene búsqueda literal por subcadena sobre código o nombre. En un MySQL 8.4 aislado con 10.000 entradas sintéticas, `EXPLAIN ANALYZE` mostró hasta 10.000 búsquedas por clave primaria en `academic_subject` y `academic_subject_revision` para el conteo y nuevamente para la página. El promedio filtrado local medido estuvo entre 116 y 128 ms. Quitar las uniones innecesarias del conteo sin filtros redujo ese caso de 63–64 ms a 19–21 ms. Un CTE compartido no evitó el trabajo repetido en el plan de MySQL.

Cuatro revalidaciones del 2026-09-30 con SDKMAN Java 25.0.4, MySQL 8.4, 10.000 filas, 10 calentamientos, 50 muestras y concurrencia 1 obtuvieron medias entre 10,964 y 25,258 ms. Dos corridas tuvieron máximos/p99 aislados de 170,006 ms (cola) y 175,024 ms (búsqueda); no reaparecieron en la tercera corrida con registro GC ni en la cuarta. Con 50 muestras el p99 nearest-rank es la observación máxima. Las pausas G1 visibles fueron de aproximadamente 4,6–7,7 ms, sin confirmar la causa de los outliers. Los detalles por consulta están en [la especificación del perfil](../../superpowers/specs/2026-09-30-curriculum-mysql-search-performance.md). `EXPLAIN ANALYZE` mostró que la búsqueda filtrada recorre el índice de entradas y consulta sujeto/revisión para las coincidencias retornadas. El contenedor desechable y perfil sintético no demuestran un SLA institucional.

## Decisión

- `academic_subject` y `academic_subject_revision` siguen siendo la fuente de verdad normalizada.
- Cada `academic_curriculum_entry` conserva dos valores de búsqueda derivados e inmutables: `search_subject_code` y `search_subject_name`. El adaptador los genera en la misma transacción desde la fila validada y Flyway los reconstruye para entradas anteriores.
- Un índice de cobertura `(curriculum_id, semester, row_order, search_subject_code, search_subject_name)` reemplaza el índice angosto de orden. El motor puede filtrar la subcadena desde el índice en el orden de página y hacer las uniones de lectura solo para coincidencias.
- El endpoint mantiene la misma respuesta y sigue escapando `%`, `_` y `!`. La nueva proyección no es editable ni se expone por la API.

## Consecuencias

- Se duplica una representación de código y nombre por entrada curricular y aumenta el tamaño del índice. Es un costo deliberado del modelo de lectura, derivado desde las entidades normalizadas, con un gate de regresión `<50 ms` promedio en el escenario local de 10.000 filas; la prueba impone el gate y la medición lo pasó.
- La proyección se debe poblar en la única ruta de escritura del catálogo y backfillarse antes de exigir `NOT NULL`.
- La prueba sintética mide una máquina, un contenedor y concurrencia 1; no acredita rendimiento representativo de producción ni reemplaza la aprobación del conjunto de carga institucional, del hardware destino o de un SLO.
- Si la búsqueda cambia de subcadena a otra semántica o si el volumen esperado supera el límite de importación actual, se vuelve a perfilar el diseño antes de extender el índice.
