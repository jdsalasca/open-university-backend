# Directorio público UPTC de posgrados — captura del 4 de octubre de 2026

## Fuente consultada

- Página oficial: [Programas académicos de posgrados UPTC](https://www.uptc.edu.co/sitio/portal/sitios/programas_ofer/posgrados.html).
- La página consultada muestra 139 programas y declara actualización el 3 de agosto de 2026.
- La captura local se realizó el 4 de octubre de 2026. La fecha de captura identifica el momento de lectura, no una certificación de vigencia institucional.

## Alcance de la instantánea

El JSON público incorpora 139 registros incluidos en la vista oficial al momento de la captura. Los 139 códigos son distintos; los campos visibles del directorio requeridos para búsqueda tienen valor; todos los enlaces de detalle validados usan HTTPS y el host oficial `www.uptc.edu.co`.

La ficha preserva el nombre, código, facultad o unidad publicada, código organizacional publicado, nivel, modalidad, lugar, resumen de lugares y enlace de detalle. Los filtros se procesan en el navegador y no envían las búsquedas al servidor.

El directorio es una fuente informativa separada del maestro curricular del producto. La inclusión en la página no confirma que exista una convocatoria abierta, fechas, cupos, admisión, matrícula, costos, acreditación ni disponibilidad actual. El producto no registra aspirantes ni ejecuta reglas de selección.

## Límites de interpretación

- “Facultad o unidad publicada” conserva el rótulo de la página; puede contener una unidad académica o una seccional. No se trata como afiliación académica vigente.
- No se completa información faltante por inferencia ni se crean categorías institucionales nuevas.
- La instantánea no se importa a MySQL, no genera migraciones y no alimenta la API de estructura u oferta.
- La información debe revisarse manualmente en la página oficial antes de actualizar el JSON. Comparar cantidad, códigos, títulos, fechas y host de cada enlace; actualizar `capturedAt` y mantener `pageUpdatedAt` conforme a lo que declare la página.
- Si la fuente cambia su presentación o campos, detener la actualización de la instantánea y revisar el mapeo antes de publicar.
