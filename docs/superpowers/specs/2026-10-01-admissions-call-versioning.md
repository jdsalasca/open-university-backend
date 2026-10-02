# Especificación — convocatorias de admisión versionadas

## Propósito y límite de operación

Agregar al monolito de admisiones una capacidad para preparar convocatorias de pregrado presencial en borrador, publicar revisiones con referencia verificable y exponer únicamente la última revisión publicada de cada convocatoria. La agenda pública 2027-I incorporada al frontend queda como respaldo mientras la API no tenga una revisión publicada. Esta entrega organiza información pública; no recibe solicitudes ni toma decisiones de admisión.

La especificación no habilita datos reales. ACRA, DTIC, Jurídica y el responsable de la fuente deben validar el mandato, dueño del calendario, referencias aceptables, roles y configuración antes de mapear permisos institucionales o publicar datos operativos. No crea postulantes, PIN, pagos, documentos, puntajes, cupos, resultados ni integraciones con SIRA, «Inscríbete» o Fase III.

## Modelo y reglas

- `AdmissionsCall` es la identidad estable de una convocatoria y usa una clave única opaca (`callKey`). No infiere cohortes, programas ni periodos académicos.
- `AdmissionsCallRevision` contiene una fotografía completa del contenido: título, nombre mostrado, fechas de consulta/actualización, fuentes HTTPS y hasta 50 hitos. Cada clave de hito es única dentro de la revisión; todos los rangos tienen fin inclusivo y `endsOn >= startsOn`.
- El contenido de una revisión se puede reemplazar solo mientras está en `DRAFT`; `draftVersion` evita sobrescrituras concurrentes. Una revisión publicada es inmutable. Para corregirla se crea otra revisión completa.
- Publicar exige `admissions:calendar:write`, una referencia institucional obligatoria, coincidencia de `draftVersion` y la revisión pública vigente que el administrador revisó. La escritura actualiza la revisión vigente y agrega auditoría en una transacción.
- Auditoría registra `actor_user_id` canónico, `actor_identity_id` federado vinculado, fecha, convocatoria, revisión, acción, referencia y resumen. Un actor no registrado no puede mutar.
- Las consultas públicas devuelven solo convocatorias con una revisión publicada, sin actores ni borradores. La consulta selecciona las 100 convocatorias más recientemente publicadas antes de cargar todos sus hitos para no cortar una revisión. React permite elegir entre ellas; con respuesta vacía o error conserva la agenda 2027-I del frontend.
- La consola administrativa requiere permisos separados de lectura y escritura y limita su lista a 100 convocatorias. Los endpoints quedan cerrados por defecto mediante OIDC/mapeo de permisos vacío.
- URL de fuente acepta HTTPS público sin credenciales incrustadas; no se permiten esquemas ejecutables. Hitos y textos tienen límites explícitos y no aceptan archivos ni datos personales.
- No se crean filas iniciales ni fechas oficiales en Flyway/Compose. La agenda estática actual es el respaldo visible para el público y conserva sus enlaces y fecha de consulta.

## Contratos

- `GET /api/v1/admissions/calls`: lista pública de revisiones vigentes publicadas.
- `GET /api/v1/admin/admissions/calls`: lista administrativa acotada, incluidos borradores y número de revisión vigente.
- `POST /api/v1/admin/admissions/calls`: crea una convocatoria y su primera revisión borrador.
- `POST /api/v1/admin/admissions/calls/{callId}/revisions`: crea una revisión borrador completa para una convocatoria existente.
- `PUT /api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}`: sustituye contenido solo si el borrador y `draftVersion` coinciden.
- `POST /api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}/publish`: exige `draftVersion`, `expectedPublishedRevisionId` nullable y `officialReference`.

El contrato de publicación es idempotentemente seguro frente a solicitudes repetidas: si la revisión ya cambió o publicó otra sesión, el servidor devuelve `409`; el cliente relee y no reintenta la mutación.

## Interfaz

`/#admisiones` conserva la agenda pública existente y, si la API retorna convocatorias publicadas, representa esos datos versionados. La consola de borradores solo aparece con permisos confirmados por `/api/v1/me`; lectura y escritura se controlan por separado. Los formularios usan React Hook Form, SCSS de la capacidad, entradas accesibles y estados de carga, error, vacío y éxito. Al revocar permisos o desmontar la vista se abortan solicitudes y se limpia el contenido administrativo en memoria.

## Aceptación

1. Una convocatoria vacía en BD no cambia la agenda pública estática ni crea datos semilla; más de una convocatoria publicada se puede elegir desde la vista pública.
2. Crear/editar borrador valida clave, fuentes, cardinalidad, claves de hitos, fechas y longitudes; un borrador puede cambiar solo con la versión esperada.
3. Publicar exige referencia no vacía, revisión borrador y valores esperados; conflictos no dejan publicación ni auditoría parcial.
4. Las rutas administrativas devuelven `401` sin sesión y `403` sin permiso; la lectura pública excluye borradores.
5. La auditoría transaccional enlaza actor canónico y vínculo OIDC existentes; usuarios no registrados no pueden publicar.
6. MySQL 8.4 conserva unicidad, FKs e índices de lectura y rechazo de transiciones inconsistentes; el contrato ejecuta la consulta CTE y publicación con contenido sintético y revierte sus filas.
7. React oculta la consola sin permisos, cancela consultas al desmontar/revocar lectura, ofrece edición/publicación sin reintentos automáticos y anuncia errores/estados a tecnologías de asistencia.
8. `mvnw verify`, `npm test`, `npm run lint`, `npm run build`, migración MySQL 8.4 y preview Compose pasan; documentación C4, datos, proceso, plan y roadmap coincide con el comportamiento implementado.

## Decisiones fuera de alcance

- Inscripción, perfiles de aspirante y datos personales.
- PIN, recaudo o gratuidad del derecho de inscripción.
- Reglas de puntaje, cupos, desempates, selección, resultados o reclamaciones.
- Cargas de archivos, integración externa, correo o notificaciones.
- Habilitar identidad OIDC, conceder permisos o retirar los gates institucionales.
