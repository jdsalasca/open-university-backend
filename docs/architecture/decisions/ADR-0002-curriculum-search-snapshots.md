# ADR-0002: Proyección de búsqueda para entradas curriculares

- **Estado:** Aceptado para el preview v0
- **Fecha:** 2026-09-30
- **Alcance:** Consultas públicas paginadas del módulo `academics`

## Contexto

El contrato mantiene búsqueda literal por subcadena sobre código o nombre. En un MySQL 8.4 aislado con 10.000 entradas sintéticas, `EXPLAIN ANALYZE` mostró hasta 10.000 búsquedas por clave primaria en `academic_subject` y `academic_subject_revision` para el conteo y nuevamente para la página. El promedio filtrado local medido estuvo entre 116 y 128 ms. Quitar las uniones innecesarias del conteo sin filtros redujo ese caso de 63–64 ms a 19–21 ms. Un CTE compartido no evitó el trabajo repetido en el plan de MySQL.

Después de aplicar la proyección y el índice, la última ejecución repetible del 2026-09-30 obtuvo promedios de 13,526 ms para la primera página sin filtro y 21,470 ms para búsqueda por subcadena (ambos con 10 calentamientos, 50 muestras y concurrencia 1). Los p99 de esa ejecución fueron 17,897 y 27,199 ms; ejecuciones anteriores midieron 40,694 y 52,894 ms para búsqueda. Las medias pasan el gate solicitado, mientras la variación de cola queda visible en los percentiles. `EXPLAIN ANALYZE` mostró que la búsqueda filtrada recorre el índice de entradas; la consulta de página hace las búsquedas de sujeto/revisión solo para las coincidencias retornadas. El contenedor desechable y perfil sintético no demuestran un SLA institucional.

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
