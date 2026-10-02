# Plan: administración de borradores de oferta académica

> **Para agentes:** TDD obligatorio (AAA, RED observado, GREEN mínimo, REFACTOR). Cada tarea debe conservar contratos internos explícitos, datos ficticios solamente y el límite DEV/institucional descrito en la especificación.

**Objetivo:** habilitar borradores persistentes y auditables de grupos por periodo y asignatura curricular publicada, visibles desde `/#academia`.

**Arquitectura:** dominio `academics` existente dentro del monolito Spring; puertos de aplicación primero, adaptador JDBC y migración Flyway después. React recibe permisos de `/api/v1/me`, usa un cliente tipado y SCSS en un panel acotado. Dos permisos específicos, sin otorgamientos productivos por defecto.

**Herramientas:** Java 25/SDKMAN, Spring Boot, MySQL/Flyway, JUnit/MockMvc, React/Vite/TypeScript, Vitest/Testing Library/SCSS.

**Revisión:** comprobar que no se duplica el catálogo curricular ni la transición de periodos; validar bloqueo por defecto, transacción auditoría/dato, concurrencia optimista, estados vacíos y límites de exportación/paginación. Mantener Compose y volúmenes actuales; preservar archivos untracked existentes.

## Tareas

### Tarea 1 — Contratos y reglas de dominio

**Archivos:** `backend/src/main/java/.../academics/application/AcademicOffering*`, `.../academics/domain/AcademicOfferingDraft.java`, pruebas de dominio.

- [x] Escribir pruebas AAA para creación, normalización de código, capacidad positiva, fechas coherentes, referencia, versión y ausencia de publicación/matrícula.
- [x] Ejecutar pruebas focalizadas y observar RED.
- [x] Añadir tipos de dominio inmutables y puertos de servicio/repositorio; las opciones referencian IDs ya existentes.
- [x] GREEN y refactor manteniendo reglas pequeñas y sin lógica de negocio en controlador.

**Criterio:** oferta inválida no llega al adaptador; las reglas no dependen de Spring/JDBC.

### Tarea 2 — Persistencia, auditoría y API protegida

**Archivos:** Flyway V23, adaptador JDBC, controller/requests/responses/handler, seguridad, pruebas de repositorio/API.

- [x] Escribir primero pruebas AAA de autorización, creación/edición, filtro por periodo, cursores/límite máximo 100, duplicados, versión obsoleta, referencias/FK y rollback sin auditoría parcial.
- [x] Ver RED en backend antes de implementar.
- [x] Agregar tabla `academic_offering_draft` con FK a periodo y fila curricular; tabla append-only `academic_offering_draft_audit_event`; índice estable de lectura; no insertar fixtures/semillas.
- [x] Añadir `GET /api/v1/admin/academic-offerings?periodId=...`, `POST /api/v1/admin/academic-offerings` y `PUT /api/v1/admin/academic-offerings/{id}`. El historial se consulta por un recurso seleccionado.
- [x] Añadir `academic:offerings:read/write` al enum/security y cubrir falta de autenticación/permisos. No asignar roles productivos.
- [x] GREEN, refactor y contrato de persistencia/cursores sobre MySQL 8.4 desechable; Compose conservó su propia base.

**Criterio:** eventos y borrador se confirman juntos; periodos/currículos se consultan en sus tablas existentes; escritura concurrente obsoleta devuelve conflicto determinista.

### Tarea 3 — Interfaz React integrada a academia

**Archivos:** cliente/contratos/tests, `AcademicOfferingDraftPanel.tsx/.scss`, integración y contratos de permisos en App/Academia.

- [x] Escribir pruebas AAA para permisos, selección de periodo/currículo/asignatura, alta, edición, confirmación de conflicto, historial, validación temprana, carga/error/vacío y cancelación/revocación; observar RED.
- [x] Implementar cliente con `AbortSignal`, token en memoria, sin reintento automático y sin almacenar estado sensible.
- [x] Montar panel dentro de `/#academia`; mostrar solo con lectura confirmada y habilitar cambios solo con escritura.
- [x] Presentar capacidad como propuesta y borradores claramente rotulados; no mostrar cupos disponibles ni enlace de inscripción.
- [x] GREEN/refactor; una prueba de teclado verifica el orden y acceso a los controles de creación.
- [ ] Revisión visual del panel en preview autenticado, modo oscuro y ancho móvil queda como UAT manual; el entorno no se sembró con periodos ni currículos.

**Criterio:** el panel no consulta sin lectura, el backend deniega aun si se invoca la API manualmente, y perder permiso elimina de inmediato los datos administrativos.

### Tarea 4 — Diagramas, roadmap, preview e integración

**Archivos:** `docs/architecture/c4.md`, `data-model.md`, `process-flows.md`, `docs/ROADMAP.md`, `docs/PROJECT.md` si corresponde.

- [x] Documentar el corte como borrador técnico; registrar gates antes de una fuente real o publicación.
- [x] Ejecutar `mvnw verify`, `npm test`, `npm run build` y `npm run lint` según instrucciones repo.
- [x] Revisar `git diff`, confirmar migración sin seeds, revisar el manifiesto productivo y actualizar el gitlink del frontend.
- [x] Integrar los dos repositorios en `develop`, conservar Compose/MySQL y abrir `http://localhost:5173/#academia`; el preview local muestra su acceso sintético y la API confirma permisos. La inspección visual autenticada queda como UAT manual indicada en la tarea 3.

**Criterio:** verificaciones limpias observadas; commits de frontend/backend en sus ramas `develop`; preview local accesible solo en loopback; documentación distingue prototipo de operación UPTC.

## Auto-revisión

- No se calcula disponibilidad: capacidad propuesta queda explícita.
- No hay efecto colateral al abrir/cerrar periodo.
- No se crea un catálogo paralelo: una fila curricular publicada identifica la versión exacta.
- No se migran datos, se crean cuentas, ni se otorga autorización institucional.
- El modo intersemestral usa el tipo existente y no infiere reglas especiales.
