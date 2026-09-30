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
| `media_asset` | Metadatos y clave generada para imagen almacenada | Nombre interno UUID; hash SHA-256; MIME detectado; tamaño máximo; no guardar bytes en MySQL |
| `administrative_audit_event` | Actor, acción, entidad, revisión y cambio relevante | Inserción únicamente; retención/consulta definida por política institucional |

El esquema detallado se concreta con la primera migración y las pruebas. No se duplican `updated_by` y `updated_at` en cada fila si el registro de auditoría ya cubre la autoría y secuencia de cambio. La configuración pública lleva un número de revisión y ETag para caché e invalidación. Volver a una revisión previa crea un nuevo snapshot, no mueve el puntero hacia atrás.

## Presupuesto de latencia

- Definir un conjunto versionado de consultas críticas, tamaño de dataset y carga de referencia por dominio.
- Registrar latencia de statement y de API por separado; medir promedio, p50, p95 y p99.
- Usar pool de conexiones acotado, paginación, índices respaldados por `EXPLAIN ANALYZE` y consultas sin N+1.
- Añadir caché solo tras identificar lectura repetida, clave, expiración e invalidación en los cortes de configuración.
- No afirmar `<50 ms` promedio hasta publicar los datos de prueba, volumen, hardware y periodo de observación.
