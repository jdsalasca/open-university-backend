# Plan — inscripción y selección de aspirantes de pregrado presencial

> Plan de ejecución sujeto a aprobación institucional. Aplicar `superpowers:executing-plans` y `superpowers:test-driven-development` por cada historia una vez se cumplan los gates de entrada.

**Objetivo:** implementar una ruta vertical de inscripción y selección de aspirantes para una cohorte presencial aprobada, reproducible y auditable, sin convertir reglas públicas incompletas en decisiones automáticas.

**Especificación:** [descubrimiento inicial de admisiones](../specs/2026-09-30-pregrado-admissions-process-discovery.md).

**Estimación:** 12–19 semanas netas desde disponibilidad de responsables y documentación. Esperas de aprobación, contratación, acceso a ambientes e integraciones no están incluidas. No hay fecha contractual.

## Secuencia y cronograma relativo

| Ventana estimada | Actividad | Entrega comprobable | Gate |
|---|---|---|---|
| Semanas 1–2 | Validar normativa con ACRA/Secretaría General: Acuerdo 130 y modificaciones, Resolución 111 de 2026 y anexos, calendario, vigencia y alcance por cohorte. Resolver diferencias públicas sobre número de opciones y pruebas. | Matriz de actos, reglas, cambios y responsables con enlaces oficiales y confirmación institucional. | G0: autoridad normativa acepta la matriz. |
| Semanas 2–4 | Taller del proceso actual con ACRA y programas piloto: PIN, registro, verificación, novedades, selección, resultados, recursos y opcionados. | BPMN/diagrama de flujo aprobado, actores, estados o hitos, excepciones, salidas y límites con matrícula. | G1: dueño del proceso acepta flujo y alcance. |
| Semanas 2–5, en paralelo | Confirmar personas/aspirantes, sistema maestro, identificadores, interfaces, campos mínimos, clasificación, retención, auditoría y grupos/claims. | Contratos de datos e integración, matriz actor-permiso y política de pruebas/retención. | G2: datos, seguridad y privacidad autorizan contrato y ambientes. |
| Semanas 5–6 | Diseñar interfaces de aplicación y puertos de dominio; fijar API, migración Flyway, idempotencia, concurrencia, auditoría y escenarios AAA. | Especificación API, modelo y ejemplos sintéticos aceptados por ACRA; revisión C4 y datos. | G3: criterios funcionales y técnicos aprobados. |
| Semanas 7–12 | Implementar por cortes TDD: gestión versionada de convocatoria/reglas; captura mínima autorizada; validación y consulta; selección reproducible solo con regla aprobada; publicación de resultados. | Ruta vertical local con historial, permisos, i18n y pruebas de integración. Las subtareas pueden dividirse en commits/hitos sobre `develop`. | Cada historia exige RED → GREEN → REFACTOR y revisión de código. |
| Semanas 13–15 | Integrar interfaces autorizadas y probar escenarios sintéticos, fallos, reintentos, permisos, conflictos, accesibilidad y seguridad. | Evidencia de contrato y pruebas E2E; sin conexión a producción. | G4: UAT técnico con responsables funcionales. |
| Semanas 16–19 | Ensayo con volumen/carga aprobados, conciliación, restauración y reversa; decidir si se avanza a piloto no productivo. | Informe con conteos, discrepancias, latencias promedio/p50/p95/p99, rollback y aceptación de dueños. | G5: aprobación institucional específica de piloto/corte. |

Las tareas entre semanas 2 y 5 pueden solaparse; las tareas que dependen de decisiones normativas no comienzan antes de G0. Si las fuentes de verdad o los contratos no se confirman, se mantiene el trabajo documental y se excluye el procesamiento real de aspirantes.

## Reglas de ejecución

- Backend Java 25/Spring Boot dentro del monolito modular; frontend React/Vite/TypeScript/SCSS; MySQL/Flyway.
- Diseñar puertos e interfaces antes de adaptadores. Mantener la selección separada de formularios de inscripción y evitar reutilización de modelos de catálogo académico.
- TDD AAA: escribir primero el caso feliz y casos de borde aprobados, comprobar RED por la razón esperada, implementar lo mínimo y refactorizar en GREEN.
- Probar como mínimo permisos denegados, errores de validación, reintentos idempotentes, dos cambios concurrentes de convocatoria/reglas y datos de examen discordantes, si aplican a los contratos aprobados.
- Nunca usar datos reales de estudiantes o aspirantes en pruebas, capturas, fixtures, Compose o benchmarks.
- No publicar automáticamente un ranking si falta una regla, cupo, excepción o evidencia requerida; el sistema debe fallar cerrado y conservar actor, versiones y motivo.
- Medir statement MySQL y API por separado con volumen y concurrencia aprobados. El objetivo local `<50 ms` de promedio no constituye SLA de UPTC.
- Actualizar C4, flujo de proceso, esquema de datos, ROADMAP y guía `AGENTS.md` si el módulo añade componentes, fuentes o contratos nuevos.
- Cada milestone verificable se integra y publica en `develop` en ambos repositorios cuando tenga cambios en frontend y backend; actualizar el gitlink del frontend desde el repo backend.

## Orden de milestones técnicos

1. **A0 — Contrato funcional y de fuentes:** G0–G3 aprobados; no altera datos personales.
2. **A1 — Convocatoria versionada:** alta, edición, publicación y lectura de convocatorias y calendario con referencia normativa, si ACRA confirma que hace parte de la primera entrega.
3. **A2 — Inscripción:** captura mínima y estados aprobados, prevención de duplicados y consulta/corrección según permisos.
4. **A3 — Selección reproducible:** cupos y reglas versionadas, pruebas por programa si aplican, resolución de empates y resultado trazable.
5. **A4 — Integración y ensayo:** sistemas autorizados, resultados/recursos, conciliación, rendimiento, aceptación y reversa.

Los hitos A1–A4 son una propuesta de secuencia para revisar; los responsables institucionales pueden ajustar alcance y orden antes de iniciar las historias funcionales.
