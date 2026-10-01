# Ficha de validación institucional: ciclo y expediente estudiantil

**Estado:** plantilla sin diligenciar; no es un contrato aprobado ni autoriza tratamiento de datos personales.<br>
**Uso:** registrar decisiones de alcance, fuentes y responsables antes de implementar inscripción, expediente, soportes, matrícula o migración.<br>
**Regla de trabajo:** usar únicamente metadatos de sistemas y datos sintéticos. No adjuntar expedientes, PIN, documentos, credenciales ni exportaciones reales.

## Identificación y alcance

| Decisión | Respuesta validada | Evidencia / versión / fecha | Responsable que valida |
|---|---|---|---|
| Población y modalidad incluidas |  |  |  |
| Seccionales, sedes y programas piloto |  |  |  |
| Procesos incluidos y límite de cada proceso |  |  |  |
| Convocatoria/cohorte inicial y versión de reglas |  |  |  |
| Procesos explícitamente excluidos |  |  |  |
| Relación con SIRA y la Fase III del nuevo sistema académico |  |  |  |
| Relación con «Inscríbete» y el portal efectivo de la convocatoria |  |  |  |

## Sistemas y autoridad por proceso

Una interfaz pública, un equipo que realiza tareas o una copia documental no bastan para declarar un sistema fuente maestra. Registrar la evidencia que define la autoridad y separar dueño funcional, custodio operativo y responsable técnico.

| Proceso/entidad | Sistema actual identificado | Fuente autoritativa y evidencia | Dueño funcional designado | Custodio operativo | Responsable técnico | Interfaz/export autorizado | Decisión: reutilizar, integrar, coexistir o retirar |
|---|---|---|---|---|---|---|---|
| Identidad de persona |  |  |  |  |  |  |  |
| Aspirante y solicitud |  |  |  |  |  |  |  |
| Programa, plan y currículo |  |  |  |  |  |  |  |
| Admisión y resultado |  |  |  |  |  |  |  |
| Matrícula inicial |  |  |  |  |  |  |  |
| Expediente e historial académico |  |  |  |  |  |  |  |
| Documentos y repositorio de archivo |  |  |  |  |  |  |  |

## Diccionario de campos aprobado

Registrar una fila por campo solamente después de que la autoridad institucional lo apruebe. La columna “evidencia de necesidad” debe explicar por qué el proceso no puede cumplir su propósito con menos información.

| Código estable | Definición institucional | Proceso y población | Finalidad/base aprobada | Evidencia de necesidad | Fuente maestra y regla de precedencia | Clasificación | Formato y validación | Quién consulta/corrige y en qué operación | Auditoría y rectificación | Referencia normativa/archivística | Retención, evento inicial y disposición |
|---|---|---|---|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |  |  |  |  |

Para cada campo derivado, identificar sus entradas, transformación, versión de regla, autoridad que la aprueba y método de reproducción. No guardar como dato fuente un valor que pueda calcularse de forma fiable desde fuentes autorizadas.

## Documentos y soportes

Completar solo para tipos documentales que el proceso vigente requiera y cuya custodia haya sido identificada. La TRD orienta la gestión archivística; no se convierte automáticamente en una política de eliminación de bases activas, almacenamiento de objetos o copias de respaldo.

| Tipo documental | Proceso y finalidad | Sistema/repositorio autorizado | Responsable de validación | Acceso mínimo | Integridad y análisis de archivo | Metadatos autorizados | Hito de retención/disposición | Transferencia/eliminación y evidencia |
|---|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |  |

## Migración y conciliación

| Control | Acuerdo aprobado / evidencia |
|---|---|
| Fuente, fecha de corte y responsable de extraer |  |
| Llaves de conciliación y tratamiento de duplicados |  |
| Conteos por entidad/estado y totales de control |  |
| Transformaciones versionadas y campos descartados |  |
| Casos rechazados y responsable de resolución |  |
| Ensayo con datos sintéticos o anonimizados autorizados |  |
| Reconciliación por dueño y tolerancias aprobadas |  |
| Corte, reversa, conservación del legado y evidencia de aceptación |  |

## Participantes y aprobación

| Área/rol | Nombre institucional del área | Persona delegada | Decisión que puede aprobar | Fecha/evidencia |
|---|---|---|---|---|
| ACRA central — participación funcional sugerida por el patrocinador; designación pendiente |  |  |  |  |
| Registro Académico — participación sugerida por el patrocinador; confirmar si es equipo separado o función de ACRA |  |  |  |  |
| DTIC — arquitectura, seguridad e integración |  |  |  |  |
| Secretaría General / Jurídica — competencia y reglas aplicables |  |  |  |  |
| Gestión Documental — SGDEA, clasificación, transferencia y disposición |  |  |  |  |
| Protección de datos / responsable institucional designado |  |  |  |  |
| Programa/facultad/seccional piloto |  |  |  |  |

## Gate de implementación

No se implementan campos, cargas documentales, identidad de aspirantes/estudiantes ni migraciones hasta que las decisiones anteriores tengan responsables institucionales designados, evidencias vigentes, fuente maestra, propósito y permisos aprobados, y aceptación del proceso. El diseño técnico resultante debe actualizar el modelo de datos, contratos API, pruebas AAA con casos felices y borde, diagramas C4/proceso, `docs/ROADMAP.md` y la guía `AGENTS.md`. La configuración de entorno seguirá cerrada hasta que DTIC entregue y apruebe issuer, audience, claims, grupos y callback.

ACRA y Registro Académico son sugerencias del patrocinador para participar en la validación; esa sugerencia no equivale a designación de dueños funcionales, custodios o sistemas maestros. Véanse los [hallazgos públicos de expediente y gestión documental](uptc-academic-records-management.md), la [identificación de «Inscríbete» y SIRA](uptc-inscribete-2026.md) y el [control de solapamiento de Fase III](uptc-new-academic-system-phase-iii.md).
