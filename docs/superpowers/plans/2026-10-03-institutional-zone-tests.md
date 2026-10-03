# Plan: estabilizar pruebas con la zona institucional

## Objetivo

Eliminar dos fallos intermitentes de CI causados porque las pruebas calculan fechas con la zona por defecto de la JVM, mientras el backend usa `America/Bogota` para decidir la vigencia de asignaciones y avisos. El cambio se limita a las pruebas; no modifica reglas productivas.

## Alcance

- `InstitutionalNoticeAudienceTest`: fijar las fechas del escenario con la zona institucional.
- `RoleAssignmentControllerTest`: crear la asignación desde la fecha institucional para que `/api/v1/me` la reconozca vigente.
- No modificar endpoints, permisos, repositorios, esquema ni migraciones.

## Secuencia TDD

1. **RED:** ejecutar ambas pruebas con `-Duser.timezone=UTC` en una hora en que UTC y Bogotá tengan fechas distintas; confirmar los fallos observados en CI.
2. **GREEN:** sustituir `LocalDate.now()` de las pruebas afectadas por la fecha derivada de `ZoneId.of("America/Bogota")`; repetir las pruebas enfocadas en UTC.
3. **REFACTOR/validación:** ejecutar las mismas pruebas con la zona por defecto, la suite backend y los contratos MySQL usados por CI.
4. Revisar diff y `git diff --check`, integrar el pequeño arreglo en `develop`, verificar el SHA remoto y el resultado de CI.

## Criterios de aceptación

- La prueba de avisos encuentra el aviso institucional publicado para la fecha local de Bogotá, también cuando la JVM está en UTC.
- La prueba de asignaciones observa el perfil vigente desde `/api/v1/me` bajo ambas zonas.
- Las dos pruebas enfocadas y la suite backend pasan.
- El diff contiene solo estas pruebas y este plan; no se altera comportamiento de producción.

## Riesgos y decisión

- El reloj institucional existente está definido en `AcademicTimeConfiguration` como `America/Bogota`. Mantener esa misma zona explícita en las pruebas evita que el resultado dependa del sistema operativo del runner.
- Si los tests enfocados no reproducen el fallo en UTC, detener la edición y volver a comparar su reloj con el timestamp exacto del CI.
- No introducir fechas reales de estudiantes ni de procesos académicos.

## Integración

Después de las verificaciones locales, publicar el commit de pruebas en `develop` mediante fast-forward y confirmar la ejecución de Backend CI para el SHA final. La ventana de preview existente debe seguir activa.
