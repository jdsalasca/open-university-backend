# Directorio público de posgrados UPTC

## Objetivo

Ampliar `/#programas` para consultar pregrado y posgrado con instantáneas públicas atribuidas, filtros locales y enlaces a fichas oficiales. El catálogo académico curricular existente permanece independiente.

## Alcance y decisiones

- Mantener los repositorios frontend y backend como monolitos coordinados en `develop`; este corte modifica solo el frontend y su documentación de integración.
- Registrar los 139 programas de posgrado que aparecen en el directorio oficial consultado el 4 de octubre de 2026. La página declara actualización al 3 de agosto de 2026.
- Mantener instantáneas JSON estáticas en el bundle del frontend. No añadir una API, base de datos, migración, llamada en tiempo de ejecución a la fuente externa ni carga automática.
- Dejar pregrado como vista inicial y cargar el recurso de posgrado al seleccionarlo.
- Filtrar por texto, facultad o unidad publicada, lugar, modalidad y nivel, siempre en memoria.
- Marcar los enlaces externos como UPTC HTTPS y distinguir inclusión en el directorio de oferta, convocatoria, cupos, admisión y matrícula actuales.
- No utilizar estas fichas como afiliaciones del maestro académico ni como datos de disponibilidad o reglas de selección.

## Contratos internos

- `PublicPostgraduateCatalogSnapshot` guarda versión de esquema, URL pública, fecha declarada por la fuente, fecha de captura y programas.
- `PublicPostgraduateProgram` usa el código público como única identidad estable y conserva por separado unidad publicada, nivel, modalidad, lugar, resumen de lugares y enlace de detalle.
- El cliente valida esquema, fechas calendario, códigos únicos y enlaces HTTPS al host oficial antes de mostrar los datos.

## Verificación

1. Escribir primero pruebas AAA para filtros combinados, normalización de tildes, registros duplicados, fechas inválidas, enlaces externos, carga/reintento/cancelación, aviso de límites y selector de nivel.
2. Ejecutar RED antes de implementar, luego GREEN y refactorizar las utilidades compartidas con pregrado.
3. Ejecutar la suite completa, lint y build del frontend; revisar el diff y que el asset nuevo permanezca en el bundle de cliente y no en backend/MySQL.
4. Actualizar procedencia, C4, flujo de proceso, roadmap e instrucciones de ambos repositorios.
5. Integrar el SHA frontend en `develop`, actualizar el gitlink del backend, ejecutar/verificar CI y registrar ambos SHA remotos.

## Riesgos y controles

- Una instantánea se puede desactualizar: mostrar fechas visibles y documentar actualización manual, con revisión de fuente y pruebas de integridad.
- La inclusión en el directorio no demuestra convocatoria o cupos: no mostrar una insignia de disponibilidad y mantener la advertencia junto a los resultados.
- La clasificación publicada puede decir “facultad” o “unidad”: conservar el texto original bajo el rótulo “Facultad o unidad publicada” y no reinterpretarlo como afiliación vigente.
- Los enlaces solo se aceptan si usan `https://www.uptc.edu.co`; abrirlos en pestaña nueva con `rel="noreferrer"`.
