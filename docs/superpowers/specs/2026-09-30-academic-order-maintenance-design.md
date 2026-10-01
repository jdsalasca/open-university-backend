# Mantenimiento auditable del orden académico

**Estado:** implementación local; OIDC institucional sigue pendiente.
**Alcance:** corregir prioridades visibles de unidades, lugares, relaciones y adscripciones a programas con autorización del backend.

## Problema y resultado

El modelo persiste prioridades al crear facultades/unidades, sedes, relaciones y adscripciones de programas, pero no permite corregirlas después. La estructura oficial no debe cargarse mientras un error de orden solo pueda corregirse con SQL manual.

El incremento añade comandos explícitos para cambiar cada prioridad. Cada comando requiere el valor actual esperado, el valor nuevo, un actor autenticado y una referencia de procedencia. El backend aplica el cambio y escribe su auditoría en una transacción; la prioridad queda consistente en las consultas públicas ordenadas.

## Diseño

- Crear rutas `PATCH` separadas para orden de unidad, sede, relación organizacional, relación de sede y afiliación de programa.
- Reutilizar un contrato pequeño con `expectedDisplayOrder`, `displayOrder` y `sourceReference`; no crear una entidad genérica de reordenamiento ni duplicar identidades.
- El orden es metadato de presentación vigente y el cambio aplica al confirmar la petición; la auditoría conserva hora, actor y valores anterior/nuevo. No se crea una segunda vigencia efectiva hasta que el dueño de datos la solicite.
- Aceptar solo entidades/relaciones vigentes a la fecha institucional. Si falta el elemento, responder 404; si ya no está vigente o cambió desde la lectura del operador, responder 409; validar números y referencia antes de escribir.
- Usar el bloqueo transaccional de estructura existente más una comparación condicional con el valor esperado. Una repetición que ya encuentra el orden solicitado es idempotente y no agrega otra auditoría.
- Registrar el actor, referencia y transición `orden anterior → orden nuevo` en el evento existente. Para relaciones y afiliaciones, incluir además los identificadores de ambos extremos o de la afiliación para que el evento no sea ambiguo. Añadir claves de auditoría permitidas mediante una migración Flyway aditiva.
- Mantener las mutaciones bajo `academic:structure:write`; un permiso de lectura, un método HTTP no registrado o una sesión ausente no pueden cambiar el orden.
- En React, mostrar el control de edición solo cuando `/api/v1/me` otorgue `academic:structure:write`. Editar una prioridad numérica por comando, pedir una referencia institucional, enviar el orden observado como `expectedDisplayOrder` y no actualizar optimistamente el árbol.
- Tras una respuesta aceptada, volver a consultar la estructura para reflejar el valor persistido. Ante 409, consultar el valor vigente y pedir al operador que revise antes de repetir; ante error de recarga, no presentar la vista anterior como actual.
- No añadir movimientos en lote ni intercambios de varias posiciones; cada elemento o relación usa su ruta y transacción auditada existente.
- Mantener `/#academia` en consulta mientras SSO y los grupos/permisos UPTC no estén configurados. No sembrar datos institucionales.

## Fuera de alcance

Cierre/reasignación de relaciones, altas/bajas de maestros, carga del maestro oficial, configuración OIDC, periodos y apertura de grupos. Estas capacidades necesitan su propio contrato y validación funcional.

## Aceptación

1. Los cinco tipos de prioridad existentes se pueden corregir por API protegida y su orden actualizado aparece en la consulta pública vigente.
2. Una referencia obligatoria, orden fuera de rango, entidad ausente, registro inactivo, cambio concurrente o permiso insuficiente no deja datos ni auditoría parcial.
3. Cada cambio aceptado deja una auditoría con actor, tipo e identificador concreto del elemento o vínculo, referencia y valores anterior/nuevo; un reintento idéntico no duplica el evento.
4. Flyway conserva los catálogos existentes y no inserta datos oficiales.
5. La interfaz ofrece controles solo a usuarios autorizados, exige una referencia, recarga después de escribir y refresca al detectar 409; sin OIDC los controles siguen ocultos.
6. Las pruebas usan datos sintéticos y AAA; la vista no simula datos ni permisos institucionales.
