# Directorio público UPTC de pregrado

## Contexto

La vista `/#programas` ya contiene una capacidad interna para administrar currículos, pero comienza sin programas institucionales cargados. Las personas necesitan consultar la oferta pública de pregrado mientras la institución valida el maestro y su contrato. Mezclar las dos fuentes haría que una página pública pareciera una estructura académica ya aprobada en la plataforma.

## Decisión

Agregar un directorio de solo lectura, respaldado por una instantánea JSON atribuida y fechada de la publicación pública UPTC. Vite emite la instantánea como un asset estático con hash y React lo solicita desde el mismo origen; cada programa conserva un enlace a su ficha oficial. El catálogo interno de currículos permanece en una sección visual separada y no recibe datos del directorio.

## Interacción

- Mostrar fecha de actualización declarada por la página y fecha de captura de la instantánea.
- Buscar por texto y filtrar por facultad, lugar, modalidad, nivel y marca de la fuente.
- Mostrar el total de registros y el total con el marcador textual «Programa ofertado».
- Mantener enlaces a las fichas UPTC y un acceso al calendario público de admisiones.
- Informar que el marcador no confirma convocatoria abierta, fechas, cupos ni admisión.
- Mantener carga, búsqueda y vacíos sin API ni escritura.

## Datos y límites

La captura del 2 de octubre de 2026 contiene 79 registros en 11 facultades; 72 llevan la marca de oferta de la fuente, cuya página declara actualización al 15 de septiembre de 2026. La ficha de procedencia y actualización es [uptc-undergraduate-directory-snapshot-2026-10.md](../../discovery/uptc-undergraduate-directory-snapshot-2026-10.md).

Este directorio no es el maestro académico de la plataforma, una fuente de convocatorias o cupos, ni un sistema de inscripción/selección. No agrega tablas, roles, API, PII o dependencias backend.

## Criterios de aceptación

1. Se muestran los 79 registros y se cuentan 72 marcadores afirmativos de la instantánea, sin deduplicación silenciosa.
2. Búsqueda ignora tildes/mayúsculas y encuentra términos distribuidos entre campos; filtros se pueden combinar y limpiar.
3. Cada resultado conserva facultad, lugar/modalidad y enlace oficial válido.
4. La procedencia muestra ambas fechas y el significado limitado del marcador.
5. El directorio está separado del catálogo curricular interno, que continúa vacío cuando no hay versiones publicadas.
6. Las pruebas cubren fuente/conteos, unicidad, búsqueda, filtros, carga, error, reintento, cancelación, vacíos y contenido de la interfaz.
7. La documentación describe un asset estático local y deja claro que no se sincroniza con UPTC durante runtime.
