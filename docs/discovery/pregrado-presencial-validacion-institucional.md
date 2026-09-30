# Mesa de validación institucional — pregrado presencial

**Estado:** plantilla vacía para revisión con responsables UPTC; no es una especificación funcional aprobada.<br>
**Propósito:** recopilar decisiones, normas, fuentes de datos, permisos y criterios de aceptación antes de implementar admisión, matrícula o expediente estudiantil.<br>
**Datos:** no copiar documentos, nombres, identificadores ni expedientes reales a este archivo o al entorno de desarrollo.

La ruta priorizada es pregrado presencial. Las páginas públicas de ACRA ayudan a preparar la mesa, pero no prueban la secuencia interna de procesos ni sustituyen una decisión de la autoridad normativa. Ver [descubrimiento y fuentes públicas](student-lifecycle-baseline.md).

## 1. Participantes y autoridad

| Responsabilidad | Dependencia/persona designada | Alcance de decisión | Evidencia de designación | Fecha |
|---|---|---|---|---|
| Patrocinador institucional |  |  |  |  |
| Dueño del proceso ACRA |  |  |  |  |
| Autoridad normativa |  |  |  |  |
| Dueño de los datos de aspirantes/estudiantes |  |  |  |  |
| Responsable del sistema fuente |  |  |  |  |
| Seguridad e identidad institucional |  |  |  |  |
| Gestión documental y retención |  |  |  |  |
| Programa/facultad piloto |  |  |  |  |

## 2. Alcance de la primera ruta

Los nombres siguientes provienen del inventario público de ACRA y son temas por decidir, no pasos ya aprobados.

| Decisión | Aprobación/respuesta | Responsable | Acta o referencia |
|---|---|---|---|
| Proceso que abre el primer corte: inscripción/selección, matrícula inicial, renovación, registro de asignaturas u otro |  |  |  |
| Sede(s), programa(s), modalidad presencial y cohorte(s) incluidas |  |  |  |
| Población fuera del primer corte y motivo |  |  |  |
| Ruta de ingreso normalista: programas/sedes, semestre, convenios vigentes, reconocimiento/evaluación, selección y canal documental, separada de ingreso ordinario y de cupos especiales |  |  |  |
| Procesos que seguirán operando en el sistema actual |  |  |  |
| Fuente de verdad durante el piloto y dueño de escritura |  |  |  |
| Autoridad para aceptar resultados y autorizar un eventual corte |  |  |  |

## 3. Inventario normativo aprobado

Registrar actos originales y modificaciones con vínculo institucional. No derivar reglas solo del resumen de una página web.

En la revisión pública preliminar se localizaron, entre otros, el Acuerdo 031/2021 (deroga artículo 17 del Acuerdo 130), el Acuerdo 015/2021 (deroga expresamente Acuerdos 017/2001 y 120/2006), Resoluciones 026/2009, 1577/2019 y 3418/2019 para normalistas, y Ley 2481/2025. Marcar cada efecto como confirmado por acto o pendiente de ratificación institucional; conservar por separado la ruta normalista y los seis cupos del artículo 7 del Acuerdo 015.

| ID | Acto/documento y emisor | Publicación/vigencia | Proceso, modalidad y cohortes aplicables | Modifica/deroga | URL o repositorio oficial | Confirmado por / fecha |
|---|---|---|---|---|---|---|
| N-01 |  |  |  |  |  |  |
| N-02 |  |  |  |  |  |  |
| N-03 |  |  |  |  |  |  |

## 4. Proceso y excepciones

Llenar una fila por actividad confirmada por los participantes. Registrar excepciones, correcciones y reversas como escenarios explícitos; no inferir una máquina de estados única.

| ID | Disparador y actividad | Actor responsable | Regla/acto aplicable | Datos mínimos y propósito | Sistema que lee/escribe | Resultado/evidencia | Excepciones, corrección y reversa |
|---|---|---|---|---|---|---|---|
| P-01 |  |  |  |  |  |  |  |
| P-02 |  |  |  |  |  |  |  |
| P-03 |  |  |  |  |  |  |  |

## 5. Sistemas, identificadores y datos

| Entidad/campo confirmado | Sistema dueño | Identificador estable y unicidad | Clasificación/propósito | Calidad, corrección y conciliación | Retención/eliminación | Responsable |
|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |
|  |  |  |  |  |  |  |

Antes de cualquier copia, el dueño de datos y privacidad debe aprobar campos mínimos, propósito, entorno, anonimización, acceso y retención. Los ambientes locales usan únicamente fixtures sintéticos.

## 6. Identidad y permisos

| Grupo/claim institucional confirmado | Actor institucional | Recurso y acción | Alcance por sede/programa/cohorte | Permiso interno | Aprobador |
|---|---|---|---|---|---|
|  |  |  |  |  |  |
|  |  |  |  |  |  |

Probar por separado acceso anónimo, identidad válida sin permiso, permiso de lectura, permiso de escritura, actor fuera de alcance y acceso revocado. Los nombres internos no se deben presentar como roles UPTC hasta aprobar su mapeo.

## 7. Aceptación y ensayo

| ID | Historia/resultado observable | Fixture sintético aprobado | Caso feliz y borde | Verificación de permiso/auditoría | Responsable funcional | Aceptado / referencia |
|---|---|---|---|---|---|---|
| A-01 |  |  |  |  |  |  |
| A-02 |  |  |  |  |  |  |

| Conciliación/migración | Criterio aprobado |
|---|---|
| Totales de control y atributos a conciliar |  |
| Tolerancia para diferencias |  |
| Ensayo, responsables y entorno |  |
| Evidencia y aprobación para avanzar |  |
| Rollback y tiempo máximo de recuperación |  |
| Ventana, paralelo y fuente oficial de escritura |  |

## 8. Rendimiento y operación

| Medición | Dataset y volumen aprobados | Carga concurrente/mezcla de consultas | Umbral | p50 / p95 / p99 / promedio | Instrumento y responsable |
|---|---|---|---|---|---|
| Consulta SQL crítica |  |  | Promedio < 50 ms (objetivo por validar con dueño) |  |  |
| API completa |  |  |  |  |  |
| Carga de página/interacción |  |  |  |  |  |

Una medición con base vacía, fixtures pequeños o solo una consulta no demuestra capacidad institucional ni cumplimiento del objetivo.

## 9. Aprobación de entrada a implementación

| Gate | Evidencia | Aprobador institucional | Fecha/acta |
|---|---|---|---|
| Dueño de proceso y autoridad normativa designados |  |  |  |
| Alcance, sede(s), programa(s), cohorte(s) y proceso inicial aprobados |  |  |  |
| Compilación normativa y reglas por cohorte verificadas |  |  |  |
| Fuentes maestras, identificadores y contratos de integración confirmados |  |  |  |
| Datos mínimos, privacidad, retención y ambiente aprobados |  |  |  |
| Matriz actor-permiso y claims confirmados |  |  |  |
| Aceptación, conciliación, rollback y rendimiento definidos |  |  |  |

Este formato solo recopila decisiones. La aprobación debe quedar en el mecanismo institucional definido por la UPTC y cubrir expresamente el alcance correspondiente. El archivo por sí solo no autoriza acceso a producción, migración real, cambio de fuente oficial ni retiro de legado.
