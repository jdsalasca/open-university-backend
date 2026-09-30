# Datos, propiedad y rendimiento

## Reglas

- MySQL es el almacén transaccional compartido del monolito; no fragmentar en bases por microservicio.
- Un solo dominio es dueño de escritura para cada grupo de tablas. Otros dominios consumen contratos de aplicación.
- Preferir modelo relacional normalizado, claves y restricciones explícitas, fechas UTC y tablas de asociación donde exista relación muchos-a-muchos.
- Evitar columnas JSON para datos que requieren búsqueda, claves, restricciones o joins; reservar JSON para snapshots de auditoría autocontenidos cuando el esquema de evento lo justifique.
- Flyway migra el esquema; la aplicación no altera tablas durante la ejecución. El adaptador JDBC usa consultas explícitas; las migraciones se verifican en H2 y el smoke final en MySQL sigue siendo necesario.
- Datos históricos mantienen la clave de procedencia durante migraciones; la identidad canónica nunca depende de un correo que puede cambiar.

## Modelo inicial del Centro de Identidad Visual

| Tabla | Responsabilidad | Restricciones relevantes |
|---|---|---|
| `institution_branding_current` | Puntero a la revisión visual publicada | Fila singleton; FK a snapshot existente; cambio transaccional |
| `institution_branding_revision` | Snapshot inmutable de nombre, actor, fecha y revisión de origen | Revisión monotónica única; sirve auditoría y reversión sin destruir historia |
| `institution_color_token` | Valor HEX por token de diseño y revisión | Clave compuesta `(revision_id, token_key)`; allowlist de `primary`, `ink`, `surface`, `text`, `accent`, `focus` |
| `institution_module_label` | Etiqueta editable y orden por revisión | `module_key` estable en catálogo; no permitir claves desconocidas; disponibilidad la determina backend |
| `institution_banner` | Banner, texto alternativo, orden, vigencia y ubicación por revisión | Requiere activo raster y texto alternativo; fechas coherentes; inicio anterior a fin |
| `media_asset` | Metadatos y clave generada para imagen almacenada | UUID; hash SHA-256; MIME detectado; tamaño/dimensiones acotados; actor y fecha de carga; bytes fuera de MySQL |
| `administrative_audit_event` | Actor, acción, entidad, revisión y cambio relevante | Inserción únicamente; registrar publicación de identidad y carga de medios; retención por política institucional |

La carga guarda metadatos y un evento de auditoría en la misma transacción que los asocia a la revisión vigente; una reversión elimina el archivo creado si la transacción falla. El archivo se almacena fuera de MySQL con una clave aleatoria y nunca con su nombre entregado por el navegador. No se guardan bytes, rutas locales ni nombres originales en la configuración pública. La configuración pública lleva un número de revisión y ETag para caché e invalidación. Volver a una revisión previa crea un nuevo snapshot, no mueve el puntero hacia atrás.

## Catálogo académico versionado v1

El dominio `academics` es dueño de escritura de estas siete tablas. La migración `V2__academic_catalog.sql` es aditiva y no reutiliza la auditoría de identidad visual, cuyas claves foráneas son propias de branding.

| Tabla | Responsabilidad e identidad | Restricciones / lectura |
|---|---|---|
| `academic_program` | Identidad estable por código, nivel, modalidad y sede | Único `(program_code, academic_level, study_modality, campus_code)`; v1 restringe nivel a `PREGRADO` y modalidad a `PRESENCIAL`. |
| `academic_program_revision` | Nombre, facultad, nombre de sede y código SNIES del programa a lo largo del tiempo | Cada revisión pertenece al programa; huella SHA-256 única por programa para reutilizar contenido idéntico sin mutar historia. |
| `academic_subject` | Identidad estable por código de asignatura | El código es único en este contrato; su unicidad y correspondencia con el maestro UPTC requiere confirmación antes de importar oferta oficial. |
| `academic_subject_revision` | Nombre y créditos de una asignatura en una revisión concreta | Revisión inmutable enlazada con FK compuesta al código estable; contenido idéntico se reutiliza por su huella. |
| `academic_curriculum` | Versión de plan, revisión de programa, rango de cohorte, referencia aprobatoria, estado y actores | Único `(program_id, curriculum_version)`; transición `DRAFT` a `PUBLISHED`; los campos de publicación deben estar completos solo al publicar. Índice para consultas públicas por estado/programa/cohorte y para borradores. |
| `academic_curriculum_entry` | Asignatura/revisión ubicada por semestre, espacio, componente, grupo opcional y orden de origen | FKs validan pertenencia de revisión a asignatura; no se repiten asignatura ni orden dentro del plan; índice por plan/semestre/orden. |
| `academic_catalog_audit_event` | Actor, acción, hash del archivo de origen, fecha y resumen de carga/publicación | Solo `CURRICULUM_IMPORTED` y `CURRICULUM_PUBLISHED`; FK al plan e índice por plan/fecha. No almacena el archivo ni filas CSV. |

`cohort_from` y `cohort_through` usan el código académico `YYYY-1` o `YYYY-2`; el final nulo indica que no se ha definido término. Las versiones publicadas permanecen inmutables y se consultan por cohorte; pueden existir rangos solapados mientras los responsables institucionales no determinen una regla de precedencia. La aplicación no deduce equivalencias ni qué plan corresponde a una matrícula individual.

La lectura pública de detalle resuelve una versión por UUID y devuelve sus entradas ordenadas desde las tablas de currículo existentes, sin crear una copia de asignaturas para consulta. El caso de uso exige `PUBLISHED`; borradores y UUID inexistentes producen el mismo 404. La consulta administrativa conserva su ruta autenticada para revisar borradores.

El contrato de importación actual requiere las columnas exactas y ordenadas que aparecen en [la especificación de diseño](../superpowers/specs/2026-09-29-academic-catalog-design.md) y en la [plantilla CSV vacía](../templates/academic-curriculum-template.csv). `GET /api/v1/academic-catalog/curriculum-template` genera el archivo desde `CurriculumCsvSchema.HEADERS`, su única fuente de verdad; el endpoint es público porque entrega solo nombres técnicos de columnas, no lee ni escribe la base de datos. Cada archivo contiene una versión de plan; admite UTF-8 con o sin BOM y comas RFC 4180. Los límites compartidos son 2 MiB y 10 000 filas, además de cotas por campo en `AcademicCatalogLimits`. Se valida el flujo completo antes de abrir la transacción; una fila inválida no deja programas, revisiones, entradas ni eventos parciales.

El catálogo no es propietario de aspirantes, personas, expedientes, matrícula, horarios, grupos ofertados, calificaciones, prerrequisitos operativos, equivalencias ni grados. No hay PII de estudiante en el esquema v1. La ruta visual React sirve como vista previa local; la disponibilidad autoritativa del módulo sigue apagada en branding y no se deriva de que la API responda.

## Mensajes del backend

Los textos de respuesta se mantienen en `messages.properties` (español predeterminado) y `messages_en.properties`. El API negocia `Accept-Language`; los catálogos son recursos de aplicación, no filas de configuración institucional ni datos de negocio.

## Presupuesto de latencia

- Definir un conjunto versionado de consultas críticas, tamaño de dataset y carga de referencia por dominio.
- Registrar latencia de statement y de API por separado; medir promedio, p50, p95 y p99.
- Usar pool de conexiones acotado, paginación, índices respaldados por `EXPLAIN ANALYZE` y consultas sin N+1.
- Añadir caché solo tras identificar lectura repetida, clave, expiración e invalidación en los cortes de configuración.
- No afirmar `<50 ms` promedio hasta publicar los datos de prueba, volumen, hardware y periodo de observación.
