# Reasignación atómica de afiliaciones de programas

**Estado:** propuesta funcional revisada por código y lista para revisión del patrocinador
**Fecha:** 1 de octubre de 2026
**Capacidad:** monolito académico, módulo de estructura
**Contexto:** el usuario aprobó priorizar la reasignación compuesta de programas entre unidades y sedes, después de cerrar altas/cierres separados y antes de cargar maestros oficiales.

## Resultado esperado

Un operador autorizado puede cambiar, desde una fecha efectiva, la unidad académica responsable y/o el lugar de desarrollo de un programa. La operación conserva la afiliación anterior hasta el día inclusivo previo, crea la nueva afiliación con el resto de la vigencia anterior y registra un evento único de auditoría, todo en una transacción. El programa mantiene una sola identidad en el catálogo y en la estructura, y ninguna parte puede quedar aplicada si falla otra.

## Alcance

### Incluye

- Endpoint administrativo explícito para reasignar una afiliación existente.
- Validación optimista de identidad y fechas esperadas de la afiliación de origen.
- Cambio de unidad, sede o ambas; se conserva el intervalo final existente y se proporciona explícitamente un nuevo orden.
- Actualización del intervalo anterior y creación de la nueva afiliación en una transacción MySQL, bajo el bloqueo de estructura existente.
- Un evento `PROGRAM_AFFILIATION_REASSIGNED` con programa, identidad anterior/nueva, fecha efectiva, actor y referencia institucional.
- Formulario React protegido y responsive, implementado con React Hook Form, validación temprana, vista previa de la fecha de corte, confirmación, resultados de error y recarga de vistas luego de éxito/conflicto.
- Pruebas AAA backend y frontend para éxito, autorización, concurrencia/versionado, fechas inválidas, relaciones incompatibles y rollback.
- Actualización de C4, modelo de datos, flujo de proceso, `AGENTS.md` y `ROADMAP.md`.

### No incluye

- Carga de facultades, sedes, programas o afiliaciones oficiales.
- Creación o eliminación de identidades de programa, unidades o sedes.
- Corrección retroactiva que reemplace una afiliación desde su mismo `validFrom`; debe tratarse como corrección gobernada aparte.
- Reasignación de programas entre códigos, cambios de nombre o conversión de planes curriculares.
- Publicación de oferta, cursos, cupos, matrícula, cambios de estudiantes o migración de datos.
- Activación de SSO, administración de permisos institucionales o carga de tokens/usuarios semilla.

## Modelo de dominio y reglas

1. La afiliación origen se identifica por `programId`, `affiliationId` y el par esperado `validFrom`/`validThrough`. Si su identidad o fechas difieren de lo que cargó la UI, el backend responde `409 Conflict` y no escribe cambios.
2. `effectiveFrom` debe ser estrictamente posterior a `validFrom` de la afiliación origen y estar dentro de su intervalo inclusivo. No se permite corregir o reescribir el día inicial de una relación existente.
3. La afiliación origen termina el día anterior a `effectiveFrom`; la nueva inicia ese mismo `effectiveFrom`. Ambos extremos son inclusivos, de modo que no queda hueco ni solapamiento.
4. La nueva afiliación hereda `validThrough` de la afiliación origen. Así, la operación mantiene el fin que ya tenía registrado, incluso si es una vigencia finita. El operador elige la unidad, lugar, `displayOrder` entre 0 y 100000 y una referencia no vacía de hasta 240 caracteres.
5. La unidad y el lugar destino deben existir, tener estado `ACTIVE` y cubrir toda la vigencia de la nueva afiliación. La nueva relación no puede solaparse con otra afiliación del programa distinta del origen; se conserva una sola adscripción efectiva por fecha. El chequeo se hace excluyendo el origen, cuya vigencia se trunca en el día anterior.
6. La operación exige cambiar la unidad académica o el lugar de desarrollo. Si ambos permanecen iguales, responder `409` sin auditoría, incluso si cambia el orden; el cambio de solo orden corresponde al endpoint `changeProgramAffiliationOrder` existente.
7. La operación reusa `academic_program_affiliation`; no crea otro programa, una tabla paralela ni texto canónico de facultad/sede. No modifica la referencia de origen de la afiliación anterior.
8. Cualquier incumplimiento revierte la actualización anterior, el alta nueva y la auditoría. Repetir una solicitud después de un resultado ambiguo no puede generar una segunda afiliación: la identidad/fechas esperadas ya no coinciden y la solicitud responde conflicto.

## Contrato HTTP propuesto

`POST /api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/reassign`

```json
{
  "expectedValidFrom": "2027-01-01",
  "expectedValidThrough": null,
  "effectiveFrom": "2027-06-01",
  "organizationUnitId": "<uuid>",
  "siteId": "<uuid>",
  "displayOrder": 4,
  "sourceReference": "Acto institucional autorizado"
}
```

El backend valida autenticación/autorización `academic:structure:write`, requeridos/tipos/formato y reglas de dominio. Una reasignación completada responde `201 Created` con el identificador de la afiliación nueva en el cuerpo estándar `{ "id": "<affiliationId>" }`; el servicio actual devuelve `void` al crear afiliaciones, por lo que esta nueva operación devolverá la identidad generada sin cambiar el contrato de afiliación inicial. Programa, afiliación o destino inexistente responden `404`; versión origen obsoleta, solapamiento, unidad/sede inactiva o con vigencia insuficiente, fechas incompatibles o ausencia de cambio de destino responden `409`; solicitud mal formada responde `400`. El endpoint mantiene la semántica inclusiva usada por las relaciones actuales y no calcula fechas con zona horaria.

## Flujo de interfaz

1. La consola existente obtiene el snapshot administrativo con `academic:structure:read` y los programas publicados; no carga todas las entidades si el permiso de lectura falta.
2. El operador selecciona la afiliación existente, destino, fecha efectiva, orden y referencia. Los selectores muestran códigos, nombres e intervalo esperado para reducir errores de identidad.
3. Antes de enviar, una confirmación muestra que la relación anterior terminará en `effectiveFrom - 1 día` y la nueva conservará el fin de vigencia anterior. No hay reintento automático.
4. El servidor decide el resultado. En éxito React relee el árbol público y el snapshot administrativo; en `409` vuelve a leerlos, descarta el formulario viejo y pide revisar el estado actual; ante `401/403` revalida `/api/v1/me` y mantiene suspendido el token rechazado.
5. El formulario no aparece hasta que React confirma lectura y escritura de estructura. La API autoriza la operación independientemente del estado visual.

## Auditoría y persistencia

- La actualización de `valid_through` de origen, inserción de la nueva fila y evento `PROGRAM_AFFILIATION_REASSIGNED` comparten la transacción y el bloqueo global de estructura ya existente.
- El evento usa la afiliación nueva como `entity_id`, conserva el actor y `sourceReference` del nuevo acto, y su resumen identifica programa, afiliaciones anterior/nueva y fecha efectiva sin guardar información personal.
- La siguiente migración Flyway (V19 en la línea base actual) amplía la restricción de acciones de auditoría, sin modificar las tablas de dominio ni recrear identidades.
- No se amplían permisos: se reutiliza `academic:structure:write`; la consola requiere además lectura para escoger y mostrar el origen.

## Pruebas de aceptación

- Reasignar solo unidad, solo sede y ambos conserva la identidad del programa, fecha final, continuidad inclusiva y una única auditoría.
- La nueva unidad/sede inactiva, inexistente o con vigencia insuficiente falla sin cambios parciales.
- Rechazar `effectiveFrom` igual/anterior al inicio origen, posterior al fin origen, fuera de rango, solapada con otra afiliación, o si no cambia unidad ni sede aunque cambie el orden.
- Una versión origen obsoleta responde conflicto después de un cierre/reasignación concurrente y conserva los datos de la primera operación.
- El usuario no autenticado o sin `academic:structure:write` no modifica datos; lectura no sustituye escritura.
- La UI valida selección, fecha, orden, referencia y confirmación; solo envía una vez; actualiza la estructura al terminar; mantiene ocultos los controles tras autorización rechazada.
- Ante cualquier fallo de persistencia, no queda una afiliación nueva, cierre parcial ni evento huérfano.

## Límites institucionales

La capacidad técnica no autoriza operar el maestro UPTC. Antes de cargar afiliaciones reales se requiere fuente/código oficial, mapeo de jerarquía y sede, fecha efectiva aprobada, acto de referencia, dueño del dato y SSO/perfil autorizados. Las decisiones sobre mallas y estudiantes siguen dependiendo de sus contratos propios.
