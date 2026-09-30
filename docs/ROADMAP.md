# Cronograma inicial del programa

Duraciones relativas; no son una fecha contractual. Se recalibran cuando UPTC entregue inventario de aplicaciones y bases, calidad y volumen de datos, responsables de dominio, contratos, dependencias de identidad, restricciones de contratación y tamaño de equipos. El inventario público DTIC de 2024 evidencia variedad de procesos, pero no sirve como cronograma actualizado.

## Etapas y actividades

| Etapa | Actividades principales | Duración indicativa | Resultado verificable |
|---|---|---:|---|
| 0. Descubrimiento y gobierno | Inventariar sistemas, dueños, bases e interfaces; talleres por proceso; clasificación de datos; reglas actuales; identidad institucional; seguridad, continuidad y contratación; mapa de dependencias | 4–6 semanas | Catálogo vigente, dueños por dominio, riesgos, alcance priorizado y cronograma base aprobado |
| 1. Plataforma y diseño transversal | Repos separados y coordinados; Java 25/SDKMAN; React/Vite/SCSS; monolito modular; API con i18n; Compose Watch; MySQL; migraciones; auditoría; observabilidad; C4; Centro de Identidad Visual | 8–12 semanas | Aplicaciones arrancables, configuración de marca gobernada, permisos de ejemplo no productivos, métricas de latencia instrumentadas |
| 2. Identidad y ciclo del estudiante | Descubrir primero pregrado presencial; validar responsables, norma compilada, calendario y convocatoria, sistemas maestros, identificadores, datos mínimos, permisos y excepciones. Después implementar por cortes aprobados: admisión/matrícula, trámites de estudiante y expediente | 3–5 meses (referencial, tras acceso a dueños y sistemas) | Primer recorrido presencial aprobado y probado con datos sintéticos; conciliación y rollback definidos. Sin modelar FESAD/virtual o posgrado por analogía |
| 3. Oferta académica y currículo | Programas, sedes/modalidades, versiones de malla, currículo, asignaturas, prerrequisitos, créditos y equivalencias | 3–5 meses | Catálogo versionado y migración de ensayo conciliada |
| 4. Operación académica | Periodos, grupos, matrícula, carga de cursos, programación, horarios, calificaciones, certificados y grados | 5–9 meses | Flujo académico completo por cohortes piloto y criterios de corte |
| 5. Servicios universitarios | Bienestar, salud, restaurante, residencias, biblioteca y otros servicios confirmados; pagos e integraciones donde aplique | 4–9 meses por corrientes paralelas | Flujos de servicio integrados y responsables operativos formados |
| 6. Procesos institucionales | Talento humano, finanzas, presupuesto, adquisiciones, inventarios, investigación, extensión, gestión documental, planeación y reportes | 6–12+ meses por corrientes paralelas | Procesos inventariados implementados, conciliados y aceptados por sus dueños |
| 7. Migración, corte y retiro | Ensayos repetibles, calidad y conciliación, pruebas de reversa, paralelo controlado, aceptación, corte y retiro por dominio | 3–6+ meses por dominio, solapados | Acta de corte, fuentes oficiales actualizadas, operación estabilizada y legado retirado según retención |

## Vista temporal de alto nivel

```mermaid
gantt
  title Plan referencial, sujeto al inventario y capacidad institucional
  dateFormat  YYYY-MM-DD
  axisFormat  %b %Y
  section Programa
  Descubrimiento y gobierno          :a1, 2026-10-01, 6w
  Plataforma y centro de identidad   :a2, after a1, 12w
  Identidad y ciclo del estudiante   :a3, after a2, 20w
  Oferta académica y currículo       :a4, after a3, 20w
  Operación académica                :a5, after a4, 36w
  section Corrientes en paralelo
  Servicios universitarios           :b1, after a3, 36w
  Procesos institucionales           :b2, after a3, 48w
  Migración y cortes por dominio     :b3, after a2, 70w
```

Las fechas del diagrama son una ilustración relativa iniciada el 1 de octubre de 2026, no una fecha autorizada de inicio. El programa completo podría abarcar aproximadamente 18–36+ meses con equipos de dominio y trabajo paralelo; un único equipo, integraciones complejas o datos de baja calidad pueden ampliarlo. Cada hito depende de pruebas de aceptación y ventanas de corte aprobadas, no solo del calendario.

## Línea base de entregas locales v0 — 30 de septiembre de 2026

| Entrega | Estado en los repositorios de desarrollo | Alcance y gate restante |
|---|---|---|
| Frontend y backend coordinados | Implementación local en dos repositorios separados, ambas ramas `develop`; Compose integra los servicios para vista previa. | Sigue siendo entorno de desarrollo. No es un despliegue UPTC ni tiene SSO institucional. |
| Identidad y permisos | Endpoint de identidad propia y autorización del catálogo implementados con permisos internos separados. | Emisor, audience, grupos y roles UPTC siguen sin mapear; no hay credenciales de prueba. |
| Catálogo de pregrado presencial | CSV validado a borrador, revisión de entradas, publicación protegida e interfaces públicas para versiones publicadas; actualmente vacío. | Formato y códigos deben mapearse con la fuente maestra y los responsables antes de cargar la oferta oficial. |
| Vista de React del catálogo | Ruta `/#programas` con estados vacío/red/error, lista de versiones publicadas y panel administrativo condicionado a autorización. | Es preview local. El flag autoritativo `programs.available` sigue en `false` y la pantalla no habilita operación institucional. |
| Ciclo de vida del estudiante | Descubrimiento de fuentes públicas para **pregrado presencial**. | No se implementan aspirantes, expediente, admisión ni matrícula. Proceso, cohorte, autoridad normativa, fuente y permisos requieren aprobación. |
| Latencia MySQL | No medida con carga representativa. | La meta promedio `<50 ms` permanece pendiente; una consulta local vacía no es evidencia de desempeño. |

La plantilla sin filas [academic-curriculum-template.csv](templates/academic-curriculum-template.csv) documenta el contrato de importación técnico; no es un formato exportado de un legado ni una lista de programas aprobada. El hito H5 requiere validación del catálogo y no se considera cumplido solo porque exista el endpoint o la interfaz.

## Desglose del siguiente hito: pregrado presencial

Estimación de trabajo posterior a que UPTC designe responsables y facilite documentación no productiva. No incluye espera de aprobaciones ni contratación y no constituye fecha comprometida.

| Orden | Actividad | Duración estimada | Dependencia / salida de control |
|---|---|---:|---|
| 1 | Consolidar Acuerdo 130, modificaciones, reglas por cohorte y resoluciones/calendarios aplicables con autoridad normativa | 1–2 semanas | Inventario de fuentes aprobado por Secretaría General y dueño del proceso |
| 2 | Recorrer procesos con ACRA, programas/facultades y las áreas que UPTC designe: admisión/matrícula inicial, renovación, registro de asignaturas, cancelación/aplazamiento y reingreso | 2–3 semanas | BPMN/procesos, actores, excepciones, plazos, evidencias y alcance de primera entrega aceptados |
| 3 | Confirmar sistemas fuente, identificadores y contratos para persona, aspirante y estudiante; perfilar calidad y volumen sin copiar datos personales a desarrollo | 2–3 semanas | Mapa de sistemas e interfaces, catálogo minimizado y plan de ensayo/conciliación |
| 4 | Aprobar autenticación, matriz actor-permiso, retención, trazabilidad, criterios de aceptación y escenarios sintéticos AAA | 1–2 semanas | Contratos de API y pruebas de aceptación firmados por responsables |
| 5 | Implementar una primera ruta vertical, aún por seleccionar con los responsables, con TDD, migración Flyway, observabilidad y diagramas actualizados | 4–6 semanas | Flujo funcional en entorno no productivo; casos felices, errores, permisos y concurrencia cubiertos |
| 6 | Ensayar integración/migración, reversa y carga representativa; medir API y SQL por separado (promedio, p50/p95/p99) | 2–3 semanas | Informe de conciliación/performance y decisión de avanzar, corregir o no cortar |

Las actividades 2 y 3 pueden solaparse parcialmente. La duración nominal resultante es de unas 12–19 semanas, pero el acceso a documentación/ambientes, las aprobaciones y los hallazgos pueden ampliarla. No se ejecuta corte ni se usa una fuente productiva antes de aceptación explícita del dueño de dominio.

## Cadencia y gates por dominio

1. Descubrimiento y propietario: proceso, estados, reglas, fuente oficial, datos y usuarios.
2. Especificación de aceptación y contrato del dominio.
3. Implementación en TDD y revisión de impacto arquitectónico, privacidad y rendimiento.
4. Perfilado, mapeo, limpieza autorizada y ensayos de migración con datos ficticios o debidamente anonimizados.
5. Aceptación funcional y reconciliación de control totals con el responsable de datos.
6. Operación paralela de lectura o sombra; una sola fuente de escritura oficial.
7. Corte reversible, monitorización, estabilización y retiro de legado según archivo/retención.

## Hitos próximos

- H0: inventario institucional vigente y propietarios de dominios identificados.
- H1: frontend y backend reproducibles desde repos separados con SDKMAN Java 25 y Compose local.
- H2: Centro de Identidad Visual con administración autorizada, vista previa, publicación y auditoría.
- H3: autenticación federada y roles UPTC confirmados.
- H4: identidad, expediente estudiantil y ciclo básico de **pregrado presencial** probados en un entorno UPTC no productivo, solo después de que sus responsables aprueben procesos, normas vigentes, datos, permisos y reglas por cohorte. La decisión de ruta está tomada; el subproceso/cohorte y contrato institucional siguen pendientes. Ver [descubrimiento del ciclo](discovery/student-lifecycle-baseline.md).
- H5: catálogo de programas, mallas, versiones curriculares y asignaturas aprobado.
- H6: primer corte de dominio ejecutado con conciliación y reversa ensayada.
