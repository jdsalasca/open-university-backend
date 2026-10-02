# Administración de borradores de oferta académica

**Estado:** corte técnico local; no habilitado como fuente institucional
**Fecha:** 2026-10-02

## Objetivo

Permitir que un operador autorizado prepare y revise borradores de grupos asociados a un periodo académico existente y a una fila de un currículo publicado. Cada borrador conserva código de grupo, fechas de ejecución, capacidad propuesta, versión y referencia. Las mutaciones dejan un historial append-only en la misma transacción.

El corte solo crea y actualiza borradores. No publica oferta, abre/cierra periodos, matricula personas ni comunica cupos disponibles. El cupo se presenta como capacidad propuesta. Los datos oficiales, reglas académicas y la relación operativa con SIRA/Fase III/UPTConecta siguen pendientes de validación institucional.

## Contrato funcional

- La oferta referencia `academic_period` y `academic_curriculum_entry`; el detalle de la asignatura se resuelve desde la revisión curricular inmutable existente. No duplica códigos, nombres, créditos ni datos de programas.
- Se admiten borradores para periodos regulares e intersemestrales. La operación es independiente del estado del periodo; crear o editar un borrador no cambia el estado del periodo.
- Los campos editables son `sectionCode`, `startsOn`, `endsOn`, `proposedCapacity` y una `sourceReference` acotada. El rango del borrador no puede invertirse; no se derivan ventanas de inscripción ni reglas particulares intersemestrales.
- `sectionCode` se normaliza en mayúsculas y es único por periodo, fila curricular y oferta. Capacidad debe ser un entero positivo. No se fija mínimo/máximo institucional.
- Cada alta produce `DRAFT`, versión 1 y evento `OFFERING_DRAFT_CREATED`. Cada edición compara `expectedVersion`, incrementa la versión y registra `OFFERING_DRAFT_UPDATED` con snapshot anterior/nuevo. No hay borrado físico, publicación ni transición de estado en este corte.
- Lectura y escritura requieren `academic:offerings:read` y `academic:offerings:write`, respectivamente. El servidor valida cada ruta. Los permisos locales sirven únicamente al perfil de preview en loopback; no se asignan permisos en perfiles productivos.
- La pantalla aparece en `/#academia`, permite seleccionar periodo y currículo publicado, administrar borradores, resolver conflicto de versión y consultar historial. Incluye carga, error, lista vacía, falta de permisos y cancelación al desmontar/perder lectura.

## Exclusiones y gates

- Sin matrícula, listas de estudiantes, docente asignado, aula, horario semanal, prerrequisitos, choques, prioridad, lista de espera, pago, publicación ni disponibilidad efectiva.
- Sin importación ni semillas oficiales, datos personales o conexión a SIRA, Fase III o UPTConecta.
- Antes de convertir este corte en fuente operativa se requiere decisión formal sobre frontera e integración, dueño del proceso/datos, ventanas, capacidad y reglas por modalidad, permisos y aceptación con ACRA/Registro Académico y DTIC.
- El periodo mantiene su ciclo actual. El borrador puede asociarse a cualquier periodo existente, incluido intersemestral; no infiere reglas a partir del tipo.

## Calidad

- Pruebas AAA desde dominio hasta API/React: permisos, referencias ausentes/inválidas, duplicado, capacidad inválida, periodo inexistente, fila curricular no publicada, cursor/límite, conflicto concurrente, auditoría atómica, cancelación y estados de interfaz.
- Flyway añade tablas versionadas y restricciones/FK/índices; no edita migraciones existentes ni inserta registros.
- Actualizar C4, modelo de datos, proceso y hoja de ruta. Mantener React/Vite/TypeScript/SCSS y backend Spring Boot/Java 25/i18n.
