# Comparación informativa de versiones curriculares

## Estado

Diseño aprobado por el usuario el 3 de octubre de 2026. Este cambio amplía únicamente la previsualización administrativa sin escritura del CSV curricular.

## Objetivo

Antes de que el operador cree un borrador, comparar las filas validadas del CSV con el currículo publicado más reciente que tenga exactamente la misma identidad: código de programa, nivel académico, modalidad y sede. La comparación informa diferencias para revisión humana; no determina equivalencias ni autoriza cambios curriculares.

## Comportamiento

- Reutilizar el parser y las reglas actuales de validación y normalización. Emparejar asignaturas por código normalizado.
- Seleccionar únicamente currículos `PUBLISHED` de la identidad exacta. Ordenar por `publishedAt` descendente y, en empate, por UUID ascendente para obtener un único resultado determinista.
- Clasificar filas del CSV como nuevas, modificadas o sin cambios, y filas exclusivas del referente como retiradas.
- Una fila es modificada si difiere su nombre, créditos, semestre, orden, espacio de formación, componente o grupo de opción. Comparar créditos numéricos sin considerar diferencias de escala decimal.
- Entregar conteos completos y hasta diez códigos de ejemplo por categoría. En las muestras modificadas, incluir los campos que difieren. Mantener un orden estable: orden de fila del CSV para nuevas/modificadas/sin cambios y orden del referente para retiradas.
- Mostrar los metadatos del referente: versión, cohorte y fecha de publicación. Rotular el resultado como comparación informativa.
- Si no hay referente, devolver el estado `NO_REFERENCE` sin conteos, y explicar en la interfaz que no se realizó una comparación. No mostrar ceros como si se hubiera comparado.

## Contrato propuesto

El endpoint `POST /api/v1/admin/academic-catalog/import-previews` conserva ruta, autorización y cancelación. Agrega una propiedad `comparison` con una unión discriminada:

- `NO_REFERENCE`: `reference` y `counts` son `null`; las listas de muestras están vacías.
- `COMPARED`: contiene `reference` (`curriculumId`, `curriculumVersion`, `cohortFrom`, `cohortThrough`, `publishedAt`), `counts` (`added`, `removed`, `modified`, `unchanged`) y muestras `added`, `removed`, `modified` (código y campos cambiados) y `unchanged`.

Para una comparación válida, `added + modified + unchanged` equivale al total de filas entrantes; `removed + modified + unchanged` equivale al total del referente. Cada conteo es mayor o igual a cero; cada muestra contiene como máximo diez filas. Un referente ausente no tiene conteos.

## Persistencia y seguridad

- Solo lectura en la consulta al catálogo; ninguna nueva tabla, migración, semilla o evento de auditoría.
- El endpoint mantiene `academic:catalog:write` y la validación de servidor existentes. La ruta de importación vuelve a validar el archivo completo y solo crea un borrador tras la acción explícita actual.
- El archivo y el resultado siguen siendo efímeros en el cliente; se conserva `AbortSignal` y la limpieza actual al retirar permisos, cerrar o cambiar de archivo.
- No agregar equivalencias, homologaciones, reglas de cohortes, campos personales, publicación ni datos oficiales.

## Experiencia de interfaz

Dentro de la tarjeta existente «Validación sin guardar», mostrar un bloque accesible titulado «Comparación informativa». Para `NO_REFERENCE`, indicar con claridad que no existe una versión publicada comparable. Para `COMPARED`, mostrar versión/cohorte/fecha, los cuatro conteos, códigos de ejemplo y etiquetas en español para los campos modificados. Cuando el total exceda las muestras, indicar que se muestran ejemplos, no la lista completa. La tabla actual del CSV y el botón para crear borrador conservan su flujo.

## Criterios de aceptación

1. Solo se considera la identidad exacta indicada y el estado publicado.
2. El referente se selecciona por fecha de publicación más reciente; el empate se resuelve por UUID de forma estable.
3. Las reglas de conteo y diferencia se prueban con asignaturas sintéticas, incluidos orden y precisión decimal.
4. Cada lista de muestra está acotada a diez, mientras los conteos reflejan todas las filas.
5. La respuesta sin referente se presenta sin conteos falsos.
6. El preview sigue siendo de solo lectura y conserva autorización, cancelación y flujo actual de importación.
7. Pruebas de contrato e interfaz cubren ambos estados y la presentación de campos modificados.
8. Se actualiza el diagrama del flujo de importación; C4 no cambia porque no aparecen componentes o límites nuevos.

## Ampliación de ejemplos — 4 de octubre de 2026

Cada muestra incluye `subjectCode`, `subjectName`, `semester` y `changedFields`. Para asignaturas nuevas, modificadas o sin cambio, nombre y semestre provienen de la carga entrante; para una asignatura retirada, provienen de la versión publicada de referencia. El backend conserva el máximo de diez ejemplos por categoría y valida nombres de hasta 240 caracteres y semestres entre 1 y 32767.

La ampliación es aditiva. El cliente acepta respuestas de backend anteriores que omitan ambos campos descriptivos; si recibe uno, exige el otro y valida ambos. La interfaz presenta código, nombre y semestre cuando están disponibles; con una respuesta anterior muestra el código que ya entregaba la API. No cambia conteos, reglas de comparación, persistencia, auditoría, permisos ni flujo de importación. Como no modifica los pasos del proceso ni incorpora componentes nuevos, esta ampliación no requiere otro cambio en C4 o en el diagrama de proceso.
