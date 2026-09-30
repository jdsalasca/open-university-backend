# Estructura académica y apertura de periodos — plan de implementación

> **Para agentes de implementación:** ejecutar este plan por tareas pequeñas con RED-GREEN-REFACTOR, pruebas AAA y commits revisables. Mantener alcance solo con datos sintéticos.

**Objetivo:** ordenar facultades, escuelas, lugares y programas mediante identidades relacionales y permitir preparar, aprobar, abrir, cerrar o cancelar periodos regulares e intersemestrales a partir de calendarios versionados.

**Arquitectura:** extender el monolito modular actual dentro de academics, mediante puertos de aplicación explícitos, adaptadores JDBC/Flyway y controladores REST con permisos por endpoint. React presenta el árbol y la lista de periodos publicados y ofrece controles administrativos solo cuando se suministra una autorización real; la fuente oficial no se supone disponible.

**Stack:** Java 25, Spring Boot 4, Spring Security, JDBC, MySQL 8.4, Flyway, React 19, TypeScript, Vite, SCSS, Vitest y Maven.

**Especificación:** docs/superpowers/specs/2026-09-30-academic-structure-and-periods-design.md

## Restricciones globales

- Mantener los monolitos y ambos repositorios; integrar backend y submódulo frontend en develop.
- Migrar el esquema solo con Flyway, sin DDL de arranque.
- Usar permisos académicos internos separados y denegación por defecto en rutas administrativas.
- Guardar actor, instante y referencia por cada cambio administrativo sensible.
- No incluir datos personales, catálogos oficiales no verificados ni roles UPTC asumidos.
- Mantener la posición curricular de asignaturas distinta del periodo académico.
- Aplicar TDD en cada cambio de comportamiento y escribir pruebas AAA.
- Mantener C4, modelo de datos, procesos, ROADMAP y AGENTS.md sincronizados.
- No afirmar el SLO MySQL promedio menor a 50 ms sin medición con perfil de carga definido.

## Focos de revisión

- Un ciclo de unidades organizacionales debe rechazarse aunque los nodos no sean padre e hijo directos; probar intento de ciclo indirecto.
- El cambio concurrente de estado del periodo debe dejar una transición y un único evento; probar dos órdenes sobre el mismo estado.
- Una actividad puede preceder o suceder el rango lectivo del periodo; rechazar solamente si su fin precede al inicio y documentar el calendario ACRA que separa inscripción y clases.
- Un calendario publicado no debe mutar al ingresar una modificación; probar revisión anterior inmutable y vínculo al acto modificatorio.
- Una ruta de lectura no debe poder crear, aprobar ni abrir periodos; probar lector, anónimo y método no permitido.

---

### Task 1: Unidades académicas, lugares y orden relacional

**Archivos:**
- Crear migración Flyway V6 para academic_organization_unit, academic_organization_relation, academic_site, academic_site_relation, academic_program_affiliation, academic_structure_audit_event y el bloqueo transaccional de topología.
- Crear records de dominio para unidad, lugar, relación y afiliación, con validaciones de código, tipo, rango de vigencia y orden.
- Crear puertos AcademicStructureRepository y AcademicStructureService dentro de academics.
- Crear adaptador JDBC transaccional y pruebas de integración con H2 para el árbol, orden, vigencia, afiliación, duplicados y ciclos.

**Interfaces:**
- Produces: AcademicStructureService.publicStructure() devuelve unidades y lugares activos agrupados y ordenados; createUnit, relateUnits, createSite, relateSites y assignProgram validan y persisten cambios con actor y referencia.
- Consume: los programas se identifican mediante programId UUID del catálogo ya existente; la afiliación no crea una segunda identidad de programa.

- [x] Escribir pruebas AAA para árbol facultad → escuela → programa, prioridad explícita y desempate por código/nombre, relación fechada, programa sin afiliación y referencias inexistentes.
- [x] Ejecutar las pruebas nuevas primero y observar fallos esperados antes de implementar contratos y persistencia.
- [x] Implementar migración, modelos y puertos con claves foráneas, códigos únicos, índices de lectura y campos de vigencia.
- [x] Implementar adaptador JDBC y validar ciclos indirectos consultando el grafo vigente dentro de la transacción.
- [x] Ejecutar pruebas del módulo y revisar consultas, claves e índices para evitar identidades duplicadas.

### Task 2: Contratos REST y permisos de estructura

**Archivos:**
- Crear controladores, requests, responses y manejador de errores en academics.infrastructure.web.
- Modificar SecurityConfiguration y ApplicationPermission para permisos de lectura y escritura de estructura.
- Crear AcademicStructureControllerTest de integración.

**Interfaces:**
- GET /api/v1/academic-structure devuelve solo estructura pública vigente con orden estable.
- GET /api/v1/admin/academic-structure devuelve la instantánea administrativa con historial/futuro y requiere academic:structure:read.
- POST de unidades, lugares, relaciones y afiliaciones requiere academic:structure:write.

- [x] Probar lectura pública, lectura administrativa, 401, 403, ruta administrativa no allowlisted y método no registrado.
- [x] Probar respuestas deterministas de duplicado, ciclo, vigencia inválida, actor ausente y programa inexistente.
- [x] Implementar endpoints y serialización DTO sin exponer entidades JDBC ni claims completos.
- [x] Ejecutar las pruebas web y confirmar que el backend aplica permisos en servidor.

### Task 3: Periodos y calendarios versionados

**Archivos:**
- Crear migración Flyway V7 con academic_period, academic_calendar_revision, academic_calendar_activity y academic_period_audit_event.
- Crear tipos de dominio AcademicPeriodKind, AcademicPeriodStatus, AcademicCalendarRevision y AcademicCalendarActivity.
- Crear AcademicPeriodRepository y AcademicPeriodService con operaciones para crear periodo, guardar/publicar revisión de calendario y transiciones de estado.
- Crear adaptador JDBC y AcademicPeriodServiceTest más pruebas de integración del controlador.

**Interfaces:**
- Tipo: REGULAR o INTERSEMESTRAL.
- Estados: DRAFT, APPROVED, OPEN, CLOSED, CANCELLED.
- Transiciones: DRAFT → APPROVED → OPEN → CLOSED; DRAFT o APPROVED → CANCELLED.
- Approve recibe revisionId y approvalReference; las transiciones bloquean el periodo, verifican el estado vigente y guardan actor/evento atómicamente.
- Publicación de calendario crea versión inmutable; una enmienda crea una nueva versión y referencia el acto modificatorio.

- [x] Probar máquina de estados y fechas borde, incluyendo regular e intersemestral con calendarios distintos.
- [x] Probar ventanas administrativas antes de instrucción, intervalo inválido, calendario vacío, referencia ausente, revisión superada e idempotencia concurrente.
- [x] Implementar lógica de dominio mínima con pruebas rojas primero para cada caso de negocio.
- [x] Añadir pruebas AAA de persistencia Flyway, concurrencia y auditoría atómica en estado válido/inválido.
- [x] Ejecutar pruebas, revisar índices de consulta de lista y mantener el histórico inmutable.

### Task 4: API de calendario/periodo y autorización

**Archivos:**
- Crear AcademicPeriodController, DTOs y respuestas de error localizadas.
- Añadir permisos academic:period:read y academic:period:write a ApplicationPermission y SecurityConfiguration.
- Crear AcademicPeriodControllerTest para casos públicos y administrativos.

**Interfaces:**
- GET /api/v1/academic-periods lista periodos abiertos con calendario publicado, sin borradores.
- GET /api/v1/admin/academic-periods requiere academic:period:read y limita la respuesta a 100 periodos; filtrado y paginación se completarán antes de catálogos operativos mayores.
- GET /api/v1/admin/academic-periods/{periodId}/history requiere academic:period:read y devuelve periodos de calendario con actividades y auditoría.
- POST /api/v1/admin/academic-periods crea borrador; rutas de calendarios crean y publican revisiones; rutas /approve, /open, /close y /cancel ejecutan comandos condicionales.

- [x] Probar que anónimo solo ve periodos abiertos y nunca ve borradores, historiales ni actividades de borradores.
- [x] Probar lectura frente a escritura en cada ruta y denegar cualquier método no listado.
- [x] Implementar controladores delgados que deleguen las decisiones al servicio de aplicación.
- [x] Ejecutar la suite de seguridad; confirmar 401/403/404/409 y mensajes es-CO/en según contrato.

### Task 5: Experiencia React para estructura y periodos

**Archivos:**
- Crear frontend/src/features/academics/AcademicOperationsPage.tsx y SCSS.
- Crear contracts, parser y academicOperationsClient, con pruebas en Vitest.
- Modificar App.tsx, App.scss y App.test.tsx para la ruta #academia y navegación.

**Interfaces:**
- La pantalla muestra árbol de unidades/programas/lugares y lista de periodos abiertos ordenados por fecha/tipo.
- Los paneles distinguen semestre curricular, periodo regular e intersemestral.
- Una autorización nula muestra lectura previa y explica que la administración requiere sesión institucional; no crea token de demostración.

- [x] Probar parseo, orden, estado vacío, calendarios duplicados/erróneos, rechazo de formato inválido y respuesta inválida del servidor.
- [x] Probar interacción de navegación y etiquetas accesibles mediante React Testing Library.
- [x] Implementar componentes accesibles con carga, vacío, error y explicación de que la administración requiere autorización real.
- [x] Ejecutar npm test, npm run build y npm run lint en el submódulo frontend.

### Task 6: Diagramas, cronograma y verificación integrada

**Archivos:**
- Actualizar docs/architecture/c4.md, data-model.md, process-flows.md, docs/ROADMAP.md, raíz AGENTS.md y frontend/AGENTS.md.
- Actualizar tabla de entregas locales v0 con lo realmente disponible y gates restantes.

- [x] Añadir C4, secuencias de estructura/apertura de periodo y flujo de enmienda de calendario con actores y permisos.
- [x] Distinguir reglas identificadas en fuentes públicas, diseño implementado y decisiones que requieren validación institucional.
- [x] Ejecutar Maven test, frontend test/build/lint, docker compose config y smoke HTTP local con bases vacías.
- [ ] Revisar los diffs con el revisor de código y corregir hallazgos críticos/importantes.
- [ ] Commit separado en cada repo `develop`, actualizar el submódulo y subir a upstream tras cerrar la revisión.

## Verificación final esperada

- Backend: mvn -f backend/pom.xml test termina con BUILD SUCCESS.
- Frontend: npm test, npm run build y npm run lint terminan con código cero.
- Operación: docker compose config valida; /api/v1/academic-structure y /api/v1/academic-periods responden sin exponer borradores.
- Seguridad: sin OIDC no hay escrituras administrativas autenticadas; no se añade usuario semilla.
- La evaluación de carga informa medidas observadas y no las presenta como SLA de producción.

## Continuación necesaria antes de operar estructura oficial

- Añadir comandos auditables para cerrar y corregir relaciones/afiliaciones vigentes y para cambiar el orden de unidades, sedes y programas ya creados.
- Añadir formularios administrativos solo después de conectar OIDC y confirmar los grupos/permisos UPTC.
- No cargar el maestro oficial ni editar estas tablas directamente mientras esos comandos no existan.
