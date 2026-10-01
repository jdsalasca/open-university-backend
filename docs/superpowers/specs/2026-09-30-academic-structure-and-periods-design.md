# Estructura académica y apertura de periodos — diseño

**Estado:** diseño de implementación para el siguiente hito local; los catálogos oficiales, actores, permisos institucionales y reglas de operación requieren validación con sus dueños UPTC antes de usarse como fuente oficial.
**Fecha de contraste público:** 30 de septiembre de 2026.
**Datos personales:** ninguno.

## Propósito

Agregar al monolito académico una base ordenada para facultades, escuelas, otros tipos de unidad y lugares donde se desarrolla la oferta. Enlazar los programas existentes a esos maestros mediante códigos estables y exponer un árbol navegable. Añadir el ciclo auditable de periodos regulares e intersemestrales, con calendarios versionados, actos y modificaciones conservados y comandos protegidos para aprobar, abrir, cerrar o cancelar un periodo.

“Semestre” tiene dos significados en el producto y el modelo los mantendrá separados: el entero de una fila curricular expresa la posición de una asignatura en un plan; el periodo académico es una instancia fechada con alcance, calendario y estado propios. Un intersemestral será un periodo de tipo propio que ocurre durante receso, sin cambiar la posición curricular ni reutilizar el número del semestre del plan.

## Evidencia y límites de interpretación

- El Acuerdo 067 de 2005 establece facultades, sedes seccionales y escuelas en la estructura académica; la página de Compilación Normativa muestra vigencias y modificaciones parciales. [Compilación de estructura académica](https://pagos.uptc.edu.co/DocCompNormativa/637169474139066250.pdf) y [listado oficial de Acuerdos 2005](https://www.uptc.edu.co/secretaria_general/consejo_superior/acuerdos_2005/).
- El Acuerdo 003 de 2024 modifica parcialmente esa estructura e incorpora título para sedes regionales; la página institucional de regionalización confirma su relación con el Acuerdo 067. [Acuerdo 003 de 2024](https://www.uptc.edu.co/export/sites/default/secretaria_general/consejo_superior/acuerdos_2024/Acuerdo_003_2024.pdf) y [Regionalización UPTC](https://dsp.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/vic_acad/regi/index.html).
- El directorio público de programas presenta filtros separados de facultad, nivel, lugar de desarrollo y modalidad, evidencia suficiente para no combinar programa y lugar en una cadena de texto. [Directorio de pregrado presencial](https://uptc.edu.co/sitio/portal/sitios/programas_ofer/pregrado.html?id_campus=06).
- El Acuerdo 035 de 2017 regula cursos intersemestrales durante el receso y remite sus fechas de programación al calendario aprobado por el Consejo Académico. El Acuerdo 017 de 2023 modifica el mínimo de matrícula por curso a 20 y el máximo a 35. El Acuerdo 027 de 2024 fue una excepción transitoria para junio-julio de 2024 y no se codificará como regla permanente. [Acuerdos del Consejo Superior 2017](https://www.uptc.edu.co/secretaria_general/consejo_superior/acuerdos_2017/index.html), [Acuerdo 017 de 2023](https://www.uptc.edu.co/export/sites/default/secretaria_general/consejo_superior/acuerdos_2023/Acuerdo_017_2023.pdf), [Acuerdos 2024](https://www.uptc.edu.co/secretaria_general/consejo_superior/acuerdos_2024/index.html).
- ACRA publica el calendario 2026-2 con varias resoluciones modificatorias y la página de estudiantes indica modificaciones hasta septiembre de 2026. Esto justifica conservar versiones y referencias; las fechas anuales no serán constantes de código. [ACRA — estudiantes de pregrado](https://reportes.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/adm_reg/2estu/est_pre.html).

Las páginas y acuerdos públicos son insumos de descubrimiento, no un inventario actualizado completo, un esquema de autorizaciones, una exportación maestra ni una aprobación de operación. Se conservarán las referencias normativas y los enlaces para que el responsable institucional confirme consolidación, ámbito y reglas antes de cargar datos oficiales.

## Opciones consideradas

1. **Campos libres repetidos en cada programa:** menor esfuerzo inicial, pero duplica facultades y sedes, dificulta orden, renombres e historial. Se descarta.
2. **Un solo árbol para unidades académicas y ubicaciones:** simple de mostrar, pero confunde la adscripción de una escuela con el lugar donde un programa se ofrece; no representa bien las estructuras de seccionales y regionales. Se descarta.
3. **Maestro relacional de unidades académicas y maestro separado de lugares, ambos con jerarquía/relaciones fechadas:** agrega llaves y asociaciones explícitas, pero conserva cada dimensión una vez y permite consultas y cambios auditables. Elegido.

## Diseño

### Estructura académica

- Unidades académicas estables con código institucional, tipo, nombre, estado y vigencia. El tipo inicia con facultad, escuela y unidad académica; no se inferirá que cada facultad seccional es una sede ni que toda sede es una facultad.
- Relaciones fechadas entre unidades permiten ordenar y filtrar escuela bajo facultad, conservando historia al cambiar adscripciones. Una unidad no puede ser su propio ancestro, tener ciclos ni tener dos padres del mismo tipo de relación en el mismo intervalo.
- Lugares de desarrollo forman otro maestro con código estable, tipo (central, seccional, regional, CREAD u otro confirmado), estado y vigencia. Las relaciones lugar-padre permiten representar campus y centros cuando el inventario oficial lo confirme.
- El programa del catálogo conserva su identidad existente. Una afiliación fechada enlaza el programa con su unidad responsable y lugar de desarrollo; nombres de facultad, escuela y sede se consultan desde los maestros para el árbol, no se vuelven nuevos campos independientes. Los campos libres actuales del CSV se conservan solo como snapshot histórico mientras se valida el mapeo de una futura importación oficial.
- Las consultas devuelven un árbol ordenado determinista: raíces de unidades y lugares por orden de nodo; hijos por orden de su relación y luego orden/código del hijo; programas por orden explícito de afiliación y luego código/nombre. Los catálogos sin datos muestran estado vacío; no se incorporan nombres o códigos institucionales supuestos como seed.

### Calendario y periodo

- Periodo académico con código único, año, tipo (regular o intersemestral), número/identificador, inicio y fin académicos, estado y autoría.
- Ciclo explícito: DRAFT → APPROVED → OPEN → CLOSED. DRAFT o APPROVED pueden pasar a CANCELLED; OPEN no se cancela retroactivamente. Transiciones condicionales y auditadas evitan que dos operadores abran o cierren a la vez.
- Aprobar requiere una revisión de calendario publicada, referencia del acto aprobatorio y actor. Abrir requiere estado APPROVED y esa referencia persistida. Cerrar requiere OPEN. Una revisión publicada es inmutable; una modificación crea una revisión nueva con su referencia y el periodo apunta a la versión seleccionada. Las anteriores permanecen consultables sin reescribirse. Cada transición comprueba también que la revisión activa no haya cambiado desde que se leyó el estado; una solicitud obsoleta falla en conflicto y conserva el puntero y la auditoría actuales.
- La revisión contiene actividades con clave, etiqueta, fecha/hora local `America/Bogota` inicial y final, y referencia de alcance. El API convierte las horas locales a instantes para las columnas `TIMESTAMP` y recupera la hora institucional al leer; el resultado no depende de la zona horaria de la JVM. La ventana administrativa de una actividad puede anteceder o suceder al rango de instrucción del periodo; su fin no precede a su inicio. La futura implementación de matrículas leerá estas ventanas y rechazará acciones fuera de ellas.
- No se ejecutan aperturas por cron ni se infieren fechas desde el año. Una persona con permiso autorizado ejecuta la transición explícita después de aprobar calendario y alcance.
- Los periodos regulares e intersemestrales comparten el flujo y el repositorio, pero difieren por tipo, ventanas y referencia regulatoria. La futura oferta de cursos intersemestrales aplicará el mínimo 20/máximo 35 por curso conforme al Acuerdo 017 de 2023; este hito no contará matrículas ni publicará grupos porque aún no existe matrícula estudiantil.

### Contratos y permisos

- Lecturas públicas solo de estructura y periodos publicados/abiertos, sin borradores ni eventos administrativos privados.
- Escritura bajo rutas REST allowlisted y permisos del servidor separados: academic structure read/write y academic period read/write. Permisos internos no equivalen todavía a grupos UPTC.
- Crear/editar relaciones de estructura, cargar una revisión de calendario, publicarla, aprobar, abrir, cerrar y cancelar son comandos distintos; cada uno valida estado, actor, motivo/referencia y versión esperada.
- Cada cambio administrativo escribe el evento de auditoría en la misma transacción que la transición/datos. Respuestas localizadas mediante el catálogo de mensajes existente.
- Los cambios de catálogo académico consumirán IDs/códigos maestros. La migración conserva las columnas y filas existentes con una ruta explícita de transición; no borra ni inventa registros. La importación de un catálogo oficial exige mapeo validado.

### Interfaz

La ruta nueva del espacio académico muestra secciones “Estructura” y “Periodos”. El árbol deja reconocer facultades, escuelas, programas y lugares de desarrollo sin repetirlos como texto. La pantalla de periodos distingue regular/intersemestral y expone calendario, revisión, acto de aprobación y estado. Los comandos administrativos quedan deshabilitados/explicados cuando no hay un principal autenticado. Se implementan estados cargando, vacío, error, falta de autorización y conflicto; no se presentan datos demo como oficiales.

## Persistencia propuesta

- `academic_organization_unit` y `academic_organization_relation` como maestro y relación fechada; V10 añade el orden entre hermanos y lo backfillea desde el orden previo del nodo hijo.
- `academic_site` y `academic_site_relation` para el eje geográfico, con el mismo orden de relación independiente del orden global del nodo.
- `academic_program_affiliation` enlaza programa existente con unidad responsable y lugar por vigencia, con referencia de origen; la modalidad continúa en la identidad actual del programa.
- `academic_structure_audit_event` registra cambios de unidades, sedes, relaciones y afiliaciones.
- `academic_period`, `academic_calendar_revision`, `academic_calendar_activity` y `academic_period_audit_event` para el ciclo y la historia normativa.
- Índices por código único, padre/tipo/orden, vigencia, periodo/tipo/estado y calendario/clave/fecha; FKs compuestas para evitar relaciones huérfanas.
- Migraciones aditivas Flyway V6 para estructura, V7 para periodos, V9 para referencias/afiliación y V10 para orden de relaciones; ninguna operación DDL a demanda ni migración de datos oficiales en Compose.

## Seguridad, confiabilidad y rendimiento

- Verificar permisos por método y ruta; probar 401, 403, ruta o método no allowlisted y token insuficiente.
- Evitar datos personales. La auditoría guarda actor, transición, fecha, código de periodo/unidad y referencia, sin almacenar documentos de estudiantes.
- Rechazar códigos duplicados, intervalos inválidos, ciclos, referencias inexistentes, actividades con fin anterior a inicio, publicación de calendario vacío, referencia de aprobación/cancelación ausente y comandos contra estados no permitidos. Aprobar o abrir con una revisión publicada superada devuelve conflicto.
- Una operación concurrente produce un solo cambio; la segunda responde conflicto y no duplica la auditoría.
- Las lecturas son paginadas o acotadas, con orden explícito e índices. El objetivo promedio `<50 ms` requiere dataset y concurrencia representativos; las pruebas de contrato no certificarán ese SLO por sí solas.

## Criterios de aceptación

1. Una unidad académica y un lugar de desarrollo tienen identidades separadas; la misma facultad o sede no se duplica al asociar varios programas.
2. El árbol respeta relaciones y orden estable —raíces por nodo, hijos por relación y programas por afiliación—, filtra inactivos para vistas actuales y conserva vigencias históricas.
3. No se crean ciclos, relaciones huérfanas ni asociaciones duplicadas en fechas solapadas.
4. Un calendario en borrador no se aprueba si está vacío o carece de referencia; publicado y modificado permanece versionado e inmutable.
5. Un periodo regular puede pasar por DRAFT, APPROVED, OPEN y CLOSED, y un intersemestral usa el mismo ciclo identificado por su tipo.
6. Un periodo no abre sin calendario publicado/referencia/actor; cierres no son duplicables y un estado inválido produce conflicto sin auditoría parcial.
7. Fechas inicial/final inconsistentes y actividades con intervalo propio inválido reciben errores localizados; ventanas previas a la instrucción se aceptan.
8. No se exponen borradores ni endpoints administrativos a lectura anónima; los permisos de lectura no habilitan mutaciones.
9. La interfaz distingue semestre curricular de periodo académico y muestra correctamente carga, vacío, error, sin permiso y conflicto.
10. Todas las pruebas usan datos sintéticos y AAA; backend, frontend, build, lint y Compose quedan verificables en el checkout local.

## Fuera de este hito

Inscripción de estudiantes, matrícula, apertura de grupos, selección de cursos, asignación docente, cupos de matrícula, horarios, notas, pagos, cálculo de derechos pecuniarios y decisiones académicas automáticas. Esas capacidades dependen del dueño del proceso, la fuente de matrícula, el modelo de programas/cohortes y los permisos institucionales. El umbral de 20–35 queda documentado como regla candidata para el submódulo de cursos, no usado sobre periodos.

El diseño inicial limitó la vista React al modo consulta y no integró OIDC ni formularios de administración. **Addendum V11, 30 de septiembre de 2026:** se agregaron cinco editores en `/#academia` y rutas `PATCH` protegidas para corregir prioridades de unidades, sedes, relaciones y afiliaciones con valor esperado, vigencia validada y auditoría transaccional; ver el [diseño complementario](2026-09-30-academic-order-maintenance-design.md). **Addendum: alta raíz de unidades:** `/#academia` incluye un formulario que crea únicamente una unidad tipo `FACULTY` sin padre; el backend valida el comando, requiere `academic:structure:write` y registra `UNIT_CREATED` en la transacción. La escritura continúa oculta hasta configurar OIDC y grupos autorizados. No se crea estructura oficial ni se puebla el maestro con datos de ejemplo; altas de escuelas/unidades relacionadas, relaciones de sedes y afiliaciones requieren flujos compuestos posteriores. Estos ajustes de presentación surten efecto inmediatamente y el evento conserva los valores anterior/nuevo; todavía no hay consulta de prioridad histórica por fecha. La decisión de no modelar un calendario efectivo separado se limita a metadatos de presentación y queda sujeta a confirmación del dueño de datos. OIDC continúa sin configuración institucional; cierres/reasignaciones y los demás formularios administrativos siguen pendientes. No cargar ni editar el maestro oficial con SQL manual.
