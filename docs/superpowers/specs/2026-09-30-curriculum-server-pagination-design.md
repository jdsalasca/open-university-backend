# Paginación del detalle curricular público

**Estado:** diseño v0 para implementación en el preview local. La autorización de continuar de forma autónoma se aplica a este contrato; si UPTC habilita consumidores fuera de estos dos repositorios, se requiere una versión/plan de compatibilidad antes de publicarlo.

## Contexto y resultado

El detalle curricular público actual carga todas las asignaturas (hasta el máximo técnico de 10.000 filas) y la interfaz pagina después de recibirlas. La paginación solo en React no limita el tamaño de respuesta ni el trabajo del navegador. El detalle público debe traer metadatos y las asignaturas deben poder consultarse en páginas filtradas desde MySQL.

El resultado esperado es que el primer detalle y cada cambio de página/filtro transfieran como máximo 100 filas, con conteo filtrado y orden estable. El endpoint público solo puede exponer currículos `PUBLISHED`. La cola administrativa protegida también se pagina para que el servidor y el navegador no carguen todos los borradores a la vez.

## Contrato HTTP

### Metadatos públicos

`GET /api/v1/academic-catalog/curricula/{curriculumId}` devuelve el `AcademicCurriculumResponse` como objeto raíz, sin el envoltorio anterior ni `entries`. Devuelve 404 indistinguible si el currículo no existe o está en borrador.

### Página pública de asignaturas

`GET /api/v1/academic-catalog/curricula/{curriculumId}/entries` acepta:

| Parámetro | Regla |
|---|---|
| `page` | entero de 32 bits, 1-based; opcional, por defecto `1`; mínimo `1` |
| `pageSize` | opcional, por defecto `100`; rango `1..100` |
| `search` | opcional; se recortan espacios exteriores; máximo 120 puntos de código Unicode; busca literalmente en código y nombre, sin distinguir mayúsculas, minúsculas ni tildes según la collation confirmada de MySQL |
| `semester` | opcional; entero `1..32767` |

La respuesta es `{ curriculumId, page, pageSize, totalItems, totalPages, entries }`. `totalItems` es el conteo después de filtros; `totalPages` es `0` cuando no hay coincidencias. Las filas se ordenan de forma determinista por semestre y orden de origen. Una página fuera de rango devuelve `entries: []` y los conteos vigentes. Conteo y filas se leen dentro de la misma transacción de solo lectura.

El backend escapa caracteres de comodín para que `%`, `_` y el carácter de escape se busquen literalmente; no concatena parámetros de usuario en SQL. Una búsqueda inválida o parámetros fuera de rango producen 400 con error localizado y sin consultar un currículo borrador.

### Cola administrativa de borradores

`GET /api/v1/admin/academic-catalog/drafts?pageSize=25&after={cursor}` requiere `academic:catalog:read`. `pageSize` usa 25 por defecto y admite de 1 a 100; `after` es el cursor URL-safe que devuelve la respuesta anterior. La respuesta es `{ pageSize, totalItems, drafts, nextCursor }`. El conteo incluye únicamente `DRAFT`; los elementos se ordenan por creación descendente y UUID descendente. El cursor ancla la consulta al último elemento visible y evita que publicar un borrador de una página anterior desplace y oculte borradores aún no revisados. La cola es viva: borradores creados después de iniciar el recorrido aparecen al volver a cargar desde el inicio. Cursores malformados y tamaños no enteros o fuera de rango responden 400 localizado.

## Persistencia y consultas

- Usar el índice de cobertura `ix_academic_curriculum_entry_search_order_lookup (curriculum_id, semester, row_order, search_subject_code, search_subject_name)`, que soporta orden y búsqueda sobre snapshots derivados; conservar las claves normalizadas como fuente de verdad.
- Filtrar por `status = 'PUBLISHED'` en la consulta de página y su conteo. No cargar la colección completa para luego aplicar `subList`.
- Hacer el conteo y el `LIMIT` con parámetros enlazados. La cola administrativa usa paginación por cursor sobre `(created_at, curriculum_id)` y no escanea ni omite filas mediante `OFFSET`. No se añade caché. El perfil local de 10.000 filas pasa el gate de regresión `<50 ms` promedio; el SLO institucional continúa sin caracterizar.
- El límite de importación continúa en 10.000 filas; el límite de respuesta pública continúa en 100 por página.
- Flyway V5 amplía `ix_academic_curriculum_drafts` a `(status, created_at, curriculum_id)` para filtrar por estado y cubrir el orden estable por fecha y UUID sin crear un segundo índice con prefijo duplicado.

## Interfaz

- El componente de detalle solicita una página de 100 filas y muestra navegación basada en `totalPages`.
- La revisión administrativa solicita 25 borradores por defecto, limita cada respuesta a 100 y muestra navegación accesible con cursores. Después de crear un borrador reinicia desde el más reciente; después de publicar vuelve a consultar la posición actual y, si quedó vacía, reinicia desde el comienzo.
- La búsqueda de asignatura y el semestre consultan el servidor; ambas reinician a página 1. La búsqueda se retrasa 250 ms y las solicitudes anteriores se cancelan para no mostrar resultados obsoletos.
- El filtro de semestre será un campo numérico opcional (1–32767), pues la página ya no recibe todos los semestres antes de dibujar el control.
- El estado vacío distingue currículo sin filas de filtro sin coincidencias. Las filas renderizadas nunca superan 100.
- Los borradores siguen consultándose/revisándose por el endpoint administrativo protegido y no usan la nueva ruta pública.

## Seguridad y datos

- No se agregan estados, atributos personales, permisos UPTC ni reglas del ciclo del estudiante.
- Una respuesta pública de borrador o un `curriculumId` distinto se rechaza en la interfaz; el backend sigue siendo la autoridad.
- No se agrega información oficial ni datos personales a fixtures. Los datos de pruebas son sintéticos y solo de test.

## Criterios de aceptación y casos borde

1. Resumen publicado devuelve metadatos sin `entries`; currículo ausente y borrador devuelven 404.
2. Página devuelve filas correctas en páginas primera, intermedia y final, con conteos exactos y orden estable.
3. Parámetros por defecto, límites mínimo/máximo, página fuera de rango, semestre inválido y búsqueda demasiado larga se comportan como está especificado.
4. Búsqueda coincide con código/nombre sin distinguir tildes, y trata `%`, `_` y `!` como texto literal.
5. Borradores no son visibles ni en resultados ni en conteos públicos; lector anónimo puede consultar solo versiones publicadas.
6. La interfaz nunca renderiza más de 100 filas, resetea página al cambiar filtros, cancela solicitudes antiguas y mantiene carga, error, vacío y reintento accesibles.
7. La cola administrativa pagina únicamente borradores, conserva el permiso de lectura, rechaza parámetros inválidos y no omite borradores pendientes cuando cambia una fila de una página anterior.
8. `npm test`, `npm run build`, `npm run lint` y `mvnw verify` pasan; Compose sigue operativo. El contrato aislado de MySQL 8.4 mide 10.000 filas sintéticas, publica medias y percentiles y aplica el gate local; su resultado no se presenta como rendimiento institucional.

## Decisiones y límites

- Se reemplaza la forma completa del detalle público porque el producto sigue en v0/preview y las únicas aplicaciones consumidoras son los dos repositorios coordinados. La pantalla administrativa conserva su detalle de revisión.
- Esta decisión puede requerir versionar la API si aparece un consumidor externo antes de publicar el preview; no se habilita el módulo institucional con este cambio.
- La collation `utf8mb4_0900_ai_ci` está observada en el MySQL local de desarrollo. La suite automática de backend usa H2 y no prueba la equivalencia de acentos de MySQL; esa semántica se verifica con una consulta temporal en el MySQL de Compose. La búsqueda de producción exige confirmar collation/engine institucional antes de cargar datos.
- La última verificación local posterior de 2026-09-30 registró 13,526 ms sin filtro y 21,470 ms con búsqueda (10.000 filas, concurrencia 1); ejecuciones anteriores observaron p99 de búsqueda de 40,694 y 52,894 ms. Ver [la decisión ADR-0002](../../architecture/decisions/ADR-0002-curriculum-search-snapshots.md). El perfil institucional `<50 ms` permanece pendiente.
