# Catálogo académico versionado — diseño

**Estado:** diseño técnico del siguiente incremento; requiere validación funcional UPTC antes de cargar datos oficiales o habilitar escritura institucional.  
**Alcance inicial:** programas de pregrado presencial, actividades curriculares/asignaturas y versiones de plan de estudios por cohorte.  
**Datos personales:** ninguno.

## Propósito y evidencia

Agregar al monolito un catálogo que permita preparar una malla/plan en borrador desde un archivo CSV, revisar y publicar una versión inmutable, consultar las versiones publicadas y conservar autoría y referencia aprobatoria. La carga no registra estudiantes, grupos, horarios, notas ni matrículas; esos flujos dependen de contratos institucionales posteriores.

El Acuerdo 030 de 2021 define para pregrado una estructura con espacios de formación y componentes, concretada en el plan de estudios. Las páginas académicas UPTC publican planes con asignaturas/actividades, semestre, créditos y organización curricular. En programas con reforma se presentan planes distintos según semestre de admisión y se mantiene el plan anterior para cohortes que ingresaron antes; por eso las versiones se conservan en lugar de sobrescribirse. [Acuerdo 030 de 2021](https://www.uptc.edu.co/export/sites/default/secretaria_general/consejo_superior/acuerdos_2021/Acuerdo_030_2021.pdf) · [Información académica de un programa con planes por cohorte](https://www.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/facultades/fac_educa/preg/lic_108041_t/asp_acad/02_infaca.html) · [Políticas académicas UPTC](https://www.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/vic_acad/polacad/)

## Decisiones de diseño

1. **Modelo versionado y normalizado (recomendado):** programa estable, actividad curricular estable por código institucional, revisiones de nombre/créditos y una versión de plan que referencia esas revisiones y organiza semestre, espacio, componente y orden. Mantiene la historia al actualizarse una asignatura.
2. **Planos CSV desnormalizados:** más sencillo de importar, pero replica datos de programa y asignatura en cada versión y hace difícil mantener una identidad coherente.
3. **Importar documentos PDF directamente:** conserva el original, pero obliga a confiar en extracción/OCR para campos que determinan currículo y no es adecuado para publicar automáticamente.

Se implementa la opción 1, con CSV como formato explícito de intercambio y publicación humana separada de la carga. El archivo original no se almacena; sí su SHA-256 y metadatos de origen para trazabilidad.

## Contrato de importación CSV v1

Un archivo representa exactamente una versión de plan; encabezados RFC 4180, UTF-8 (se acepta BOM), comas como delimitador, una fila por actividad curricular. Encabezados obligatorios y sensibles a nombre:

```text
program_code,academic_level,study_modality,snies_code,program_name,faculty,campus_code,campus_name,curriculum_version,cohort_from,cohort_through,approval_reference,semester,subject_code,subject_name,credits,formation_space,component,choice_group
```

- `program_code`, `academic_level`, `study_modality`, `program_name`, `faculty`, `campus_code`, `campus_name`, `curriculum_version`, `cohort_from`, `approval_reference`, `semester`, `subject_code`, `subject_name`, `credits`, `formation_space` y `component` son obligatorios. `snies_code`, `cohort_through` y `choice_group` pueden quedar vacíos.
- En v1 solo se admiten exactamente `academic_level=PREGRADO` y `study_modality=PRESENCIAL`, tras recortar espacios y normalizar mayúsculas. Se incluyen desde el inicio para que el catálogo no confunda un mismo código en niveles o modalidades diferentes. Los valores futuros requieren contrato y reglas aprobados; no se habilitan por aceptación silenciosa.
- Los códigos de programa, asignatura y sede son claves de integración estables, se recortan y se normalizan a mayúsculas; los nombres visibles de programa, sede, facultad, asignatura, espacio y componente conservan acentos y texto tras recortar espacios. La asignación de `campus_code` a la fuente maestra institucional debe validarse con el dueño de datos antes de la carga oficial.
- `cohort_from` y `cohort_through` usan `YYYY-1` o `YYYY-2`; `cohort_through` puede quedar vacío para una versión sin término final definido y no puede preceder a `cohort_from`.
- `subject_code` es obligatorio en v1 para enlazar la asignatura entre programas/versiones sin deducir identidad a partir del nombre. Su unicidad UPTC debe validarse con el dueño del catálogo antes de una importación institucional.
- Límites por campo en caracteres (además de 2 MiB/10 000 filas): códigos de programa/asignatura/sede 64 con patrón `[A-Z0-9][A-Z0-9._-]*`; nivel/modalidad 20; SNIES 32; nombres de programa/asignatura 240; facultad y sede 160; versión del plan 80; referencia aprobatoria 240; espacio y componente 120; grupo de opciones 100. Se rechaza el exceso, no se trunca. Créditos usan decimal positivo con máximo dos decimales y valor máximo 999.99; semestre es entero entre 1 y 32 767.
- `credits` acepta un decimal positivo; semestre es un entero positivo. Los nombres de espacio y componente se conservan literalmente tras recortar espacios; no se fuerza un vocabulario incompleto. `choice_group` conserva relaciones de alternativas como dato de catálogo, sin inferir cuántas opciones debe elegir un estudiante.
- Los datos de programa y plan repetidos en todas las filas deben coincidir. Se rechazan archivo vacío, columnas faltantes/desconocidas/duplicadas, códigos duplicados dentro del plan, filas incompletas, valores fuera de formato y contenido fuera de límites de tamaño/filas.

Estos nombres y formatos son el contrato de integración de esta versión del producto, no el formato oficial de un sistema legado. Un mapeo institucional posterior puede transformar exportaciones existentes hacia este contrato.

## Flujo y reglas

Antes de importar, cualquier visitante puede descargar la plantilla vacía desde `GET /api/v1/academic-catalog/curriculum-template`. El backend renderiza el encabezado RFC 4180 desde `CurriculumCsvSchema.HEADERS` y responde `text/csv; charset=UTF-8` con `Content-Disposition: attachment`; no consulta ni modifica MySQL. El enlace aparece también cuando el panel de administración está bloqueado. Solo ese `GET` está allowlisted como público; otros métodos en la misma ruta se deniegan.

1. Operador con permiso `academic:catalog:write` carga el CSV.
2. React envía el archivo a `POST /api/v1/admin/academic-catalog/import-previews`. El backend decodifica y valida el archivo completo, devuelve metadata, conteo, semestres y hasta 10 asignaturas de muestra, sin llamar al repositorio ni registrar auditoría. Una fila inválida rechaza la previsualización completa e informa número de fila/campo de forma segura.
3. El operador revisa la muestra y confirma crear el borrador. React vuelve a enviar el CSV a `POST /api/v1/admin/academic-catalog/imports`; el backend vuelve a decodificar y validar el archivo completo antes de abrir la transacción. Una sola fila inválida rechaza la carga e informa número de fila/campo; nunca deja una escritura parcial.
4. Una carga válida crea o reutiliza programa por `(program_code, academic_level, study_modality, campus_code)` y revisiones de asignatura por código, crea una versión `DRAFT` y sus actividades en una sola transacción, y registra actor, fecha, hash, referencia normativa y resumen de auditoría. Reimportar una versión existente produce conflicto, no una segunda copia.
5. El operador inspecciona el borrador en React y solicita publicación. El backend cambia `DRAFT → PUBLISHED` mediante transición condicional y registra auditoría en la misma transacción. Las versiones publicadas y sus datos asociados son inmutables.
6. GET público expone exclusivamente versiones `PUBLISHED`; versiones antiguas permanecen consultables por cohorte. `GET /api/v1/academic-catalog/curricula/{curriculumId}` devuelve solo metadata raíz y `/entries?page=1&pageSize=100&search=&semester=` devuelve una página filtrada de máximo 100 asignaturas ordenadas únicamente si la versión está publicada. Borrador y UUID inexistente responden el mismo 404; sus métodos GET no habilitan otros métodos en esas rutas. Conteo y filas usan parámetros enlazados y transacción de solo lectura; `%`, `_` y `!` se escapan en la búsqueda. Se permiten rangos solapados para no descartar planes de transición; no se inventan reglas de equivalencia u homologación.

Si un código de asignatura ya existe, una combinación idéntica de nombre/créditos reutiliza su revisión. Si nombre o créditos cambian, crea nueva revisión, conservando enlaces de planes previos. No se actualiza silenciosamente una revisión.

Modelo normalizado: `academic_program` conserva identidad por `(program_code, academic_level, study_modality, campus_code)`; `academic_program_revision` conserva nombre, SNIES, facultad y nombre de sede por versión; `academic_subject` conserva identidad por código institucional; `academic_subject_revision` conserva nombre y créditos; `academic_curriculum` conserva versión/cohorte/aprobación/estado y apunta a la revisión de programa; `academic_curriculum_entry` enlaza el plan con revisiones de asignatura y conserva semestre, espacio, componente, grupo de opción y orden. Las claves foráneas evitan versiones huérfanas. Una revisión de programa o asignatura idéntica existente se reutiliza.

## Seguridad, errores y operación

- Rutas administrativas se agregan a la allowlist método/ruta existente; lectura y escritura usan permisos internos separados. Tanto la prevalidación como importación y publicación requieren `academic:catalog:write`; la muestra de prevalidación queda limitada a 10 filas y no persiste datos ni eventos. El conversor traduce roles técnicos provisionales a permisos; los claims/grupos UPTC siguen pendientes. Sin issuer/audience reales no hay publicación desde navegador.
- Límites iniciales: 2 MiB y 10 000 filas, configurables; el límite de bytes se aplica al flujo leído y no confía solo en el `Content-Length`; no se persiste el nombre/ruta del archivo ni se ejecutan fórmulas. SQL parametrizado, claves UUID, restricciones únicas/FK e índices en programa/cohorte y revisiones.
- Errores esperados: 400 para contrato/mapeo inválido, 401/403 para falta de autenticación/permisos, 409 para versión existente o publicación concurrente, 413 para exceso de tamaño, 404 para detalle público de borrador o UUID inexistente. Respuestas localizadas usando el catálogo existente.
- Migración Flyway aditiva; no editar V1 ni la auditoría de identidad visual, cuyo FK actual es específico de branding. El módulo crea sus propias tablas de auditoría.
- Criterio de latencia por definir con dueño de datos y carga representativa; el objetivo general sigue siendo promedio MySQL <50 ms y debe acompañarse de p50/p95/p99.

## Criterios de aceptación

- CSV correcto con espacios/acento/comas entrecomilladas crea un borrador y permite consultar una versión publicada por cohorte; una modalidad/nivel distintos de pregrado presencial se rechazan.
- Error de una fila no persiste programa, asignaturas, plan ni auditoría de carga.
- Mismo código con atributos iguales se reutiliza; cambio de créditos/nombre crea revisión y no muta planes anteriores.
- Duplicados, UTF-8 inválido, BOM, columnas desconocidas, archivo vacío, tamaños/límites, campos ausentes y metadata inconsistente reciben resultados deterministas.
- Publicación sólo ocurre con permiso write y estado `DRAFT`; anónimo recibe 401, lector 403 en comandos, usuario sin permiso 403 y cualquier ruta administrativa no registrada se deniega.
- La plantilla descargada sin autenticación coincide exactamente con `CurriculumCsvSchema.HEADERS`; otros métodos HTTP sobre esa ruta pública siguen denegados.
- El detalle anónimo obtiene metadata y la primera página en paralelo para una versión publicada; un borrador no puede consultarse por las rutas públicas y otros métodos permanecen denegados.
- El detalle permite encontrar asignaturas por código/nombre, sin distinguir diacríticos, y filtrar por semestre; la búsqueda espera 250 ms y envía filtros/página al servidor con cancelación de peticiones obsoletas.
- La tabla muestra como máximo 100 asignaturas en el DOM a la vez; cada avance consulta la página solicitada al servidor y cambia de página reinicia la tabla mientras carga.
- El catálogo filtra programas localmente por código, nombre, facultad y sede; buscar no repite la consulta a la API y el plan seleccionado sigue identificado cuando no coincide con el filtro.
- Reimportación concurrente del mismo código de plan no duplica datos ni auditoría; publicaciones y auditoría son atómicas.
- El front muestra estados vacío, cargando, validación por fila, borrador, publicado, errores de red/permisos y no presenta datos sintéticos como oficiales.

## Fuera de esta entrega

Datos de estudiantes/aspirantes; carga masiva de PDFs; grupos ofertados por periodo, horarios, prerequisitos operativos, matrícula de asignaturas, equivalencias/homologación, notas y expediente. Se construirán como capacidades posteriores tras aprobar procesos, autoridad normativa, fuentes maestras, identificadores y permisos UPTC.
