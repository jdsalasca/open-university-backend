# Plan — convocatorias versionadas

> Ejecutar por TDD AAA. Mantener cerrado el uso con datos oficiales hasta los gates institucionales descritos en la especificación.

**Especificación:** [convocatorias versionadas](../specs/2026-10-01-admissions-call-versioning.md).

## Historias ejecutables

### 1. Dominio y puertos

- Definir `AdmissionsCall`, `AdmissionsCallRevision`, `AdmissionsMilestone`, actor y estados.
- Validar clave, contenido HTTPS, tamaños, máximo 50 hitos, IDs duplicados, fechas inclusivas, publicación con referencia y revisión inmutable.
- Crear puertos para carga pública/administrativa, crear/editar borradores y publicación con precondiciones de concurrencia.
- Probar primero dominio y servicios: casos felices, campos ausentes, fuera de límites, no registrado, conflicto de revisión y doble publicación.

### 2. Persistencia MySQL/Flyway

- Agregar V22 con convocatoria, revisión, hito y auditoría. No insertar calendarios semilla.
- Agregar claves foráneas del actor a `university_user` y a la identidad federada; imponer relación actor–identidad, unicidad/versionado, tipos/fechas y orden de lectura.
- Crear el adaptador JDBC con transacciones para creación, nueva revisión, reemplazo de borrador y publicación/bitácora atómica.
- Añadir contratos H2 y MySQL 8.4, incluidos rollback por conflicto, restricción de duplicado y lectura que excluye borradores.

### 3. API y autorización

- Añadir permisos `admissions:calendar:read` y `admissions:calendar:write` sin asignarlos a perfiles ni claims locales.
- Exponer lectura pública de vigentes y rutas administrativas de lectura/escritura en `SecurityConfiguration`; mantener `denyAll` como cierre final.
- Resolver actor OIDC existente a `user_id` canónico; fallar cerrado si falta el vínculo.
- Añadir respuestas, validación, manejo i18n y pruebas `MockMvc` para 200/201/400/401/403/404/409, campos e información pública mínima.

### 4. Cliente React y consola

- Añadir contratos/clientes validados para listados, creación, edición y publicación.
- Escribir primero pruebas AAA de respaldo estático, selector de versiones publicadas, permisos divididos, cancelación, formulario, validación, conflicto y no reintento.
- Añadir formulario accesible con React Hook Form y SCSS. Separar vista pública de edición; mantener visible la agenda 2027-I mientras la API responda vacía o falle.
- No almacenar tokens; el cliente usa el token en memoria y permisos confirmados desde `/api/v1/me`.

### 5. Integración y entrega

- Actualizar `AGENTS.md`, `docs/architecture/c4.md`, `data-model.md`, `process-flows.md`, `ROADMAP.md` y esta ejecución con evidencia y gates vigentes.
- Ejecutar Maven, frontend, contratos MySQL 8.4 y comprobaciones del Compose sin borrar volúmenes.
- Revisar diff/duplicación/regresiones; integrar frontend y backend en `develop` con avance fast-forward, actualizar el gitlink del submódulo y empujar a `origin` sin sobrescribir historia.
- Verificar Compose, rutas públicas, guardas 401/403 y árbol limpio excepto los archivos del usuario previamente inventariados.

## Registro de ejecución

- [x] Rama de trabajo creada desde `develop` verificado.
- [x] Especificación y este plan registrados antes del código.
- [x] Dominio, puertos y pruebas (incluye metadatos parciales, 50/51 hitos, claves duplicadas, borrador vacío e inmutabilidad).
- [x] Flyway/JDBC y contrato MySQL (H2 pasa; V22 migró MySQL 8.4 desechable y el flujo de borrador→publicación→auditoría pasó).
- [x] API, permisos e i18n (8 pruebas de controlador pasan en H2).
- [x] React, consola y accesibilidad (301 pruebas Vitest y 4 pruebas de presupuesto, lint limpio y build dentro del presupuesto).
- [x] Documentación y revisión (diagramas C4/proceso, modelo de datos, gates institucionales y bitácora aprobada revisados).
- [ ] Integración fast-forward a `develop`, actualización de Compose y comprobación de rutas.

### Registro incremental

- RED→GREEN: `AdmissionsCallRevisionTest.draft_rejects_partial_publication_metadata` falló porque una revisión borrador aceptaba `publishedByUserId` sin los otros metadatos; el dominio ahora exige que todos estén vacíos o completos y acordes al estado.
- RED→GREEN: el cliente aceptaba `currentPublishedRevisionId` sin snapshot publicada; se añade prueba de respuesta malformada y se valida la pareja puntero/revisión y coherencia de la revisión más reciente.
- RED→GREEN: la experiencia pública no ofrecía selector para varias convocatorias; la prueba `lets visitors choose between multiple published admissions calls` condujo al selector accesible.
- Evidencia intermedia: `AdmissionsCallControllerTest`, `AdmissionsCallRevisionTest` y `AdmissionsCallContentTest`: 13 pruebas, cero fallos en H2. El flujo largo de formulario pasó aislado en 1,80 s tras sustituir cientos de eventos de tecleo sintético por cambios directos de campo; la prueba conserva clics y envío del formulario.
- RED→GREEN: una convocatoria versionada distinta a la agenda de respaldo aún mostraba la insignia fija `2027-I`; la tarjeta ahora distingue una revisión publicada del calendario de referencia y no infiere el periodo desde texto libre.
- La bitácora aprobada ya existe en `/#academia`: reutiliza la auditoría relacional, requiere `academic:structure:read`, pagina con cursor, filtra por entidad/acción, expone actor opaco y cancela al perder lectura. Verificación dirigida: 8 pruebas frontend y 4 escenarios de API (autorización, filtros/paginación y entradas inválidas).
- Verificación final del frontend tras la corrección de la tarjeta: 33 archivos / 301 pruebas Vitest y 4 pruebas Node del presupuesto; `npm run lint` limpio; `npm run build` correcto dentro del presupuesto.
