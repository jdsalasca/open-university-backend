# Directorio público de servicios estudiantiles UPTC

- **Consulta de fuentes:** 3 de octubre de 2026 (America/Bogota)
- **Ruta:** `/#estudiantes`
**Estado:** incremento informativo local; no es un catálogo institucional administrable ni un canal de trámites.

## Propósito y alcance

La pantalla ayuda a encontrar información pública de Bienestar Universitario y del Sistema de Bibliotecas. La persona filtra cuatro enlaces editoriales en React y continúa en el portal de la UPTC. La búsqueda y los filtros solo viven en memoria durante la visita.

No consulta al backend, no persiste búsquedas, no autentica y no ofrece reservas, préstamos, pagos, disponibilidad, inscripción ni acceso a información personal. La descripción de una ficha es una orientación breve; la fuente oficial enlazada determina requisitos y condiciones vigentes.

## Fuentes públicas revisadas

El lector web devolvió 502/timeouts al abrir las cuatro URL, pero una verificación GET desde la estación de desarrollo el 3 de octubre de 2026 obtuvo HTTP 200 y HTML en cada página. Se revisaron los títulos y las fechas que el propio portal muestra. Esto confirma que las páginas respondían durante la consulta; no confirma vigencia operativa, disponibilidad de servicios, requisitos ni condiciones de préstamo. La ficha mantiene el enlace original de UPTC para que la persona consulte la fuente vigente.

| Ficha | Fuente | Evidencia pública revisada | Actualización mostrada |
|---|---|---|---|
| Bienestar Universitario | [Portal de Bienestar](https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/bie_uni/index.html) | Información general y sedes publicadas por Bienestar Universitario. | 1 oct 2026 |
| Bienestar Virtual | [Portal de Bienestar Virtual](https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/bie_uni/bieVir.html) | La página enumera salud mental, Ruta Violeta, cultura, desarrollo humano, actividad física, deporte, salud y apoyo socioeconómico. | 11 sep 2026 |
| Préstamo y consulta bibliográfica | [Servicios de Biblioteca Jorge Palacios Preciado](https://www.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/bibl/7secc/01jpp/serv.html) | El portal enumera consulta en sala, préstamo de recursos/equipos/salas, referencia, formación de usuarios y servicios de patrimonio. La ficha solo orienta hacia esos servicios; no los ejecuta. | 10 oct 2024 |
| Biblioteca digital y catálogo | [Sistema de Bibliotecas y buscador](https://www.uptc.edu.co/sitio/portal/sitios/universidad/vic_aca/bibl/0_busq/index.html) | El portal publica búsqueda de artículos, libros y publicaciones, y acceso a recursos digitales por áreas de conocimiento. | 8 sep 2026 |

## Comportamiento implementado

- Filtra en el navegador por categoría, nombre, descripción y palabras clave.
- Normaliza mayúsculas y diacríticos (`PRESTAMO` coincide con «Préstamo»).
- Anuncia el total en una región viva y, si no hay coincidencias, explica el estado y permite reiniciar texto y categoría.
- Abre cada destino HTTPS del dominio `www.uptc.edu.co` en otra pestaña con `rel="noopener noreferrer"`.
- Conserva visible la advertencia de que no se ingresen contraseñas ni datos personales.

## Límites y validación pendiente

- Las áreas de Bienestar y Biblioteca deben validar selección, redacción, audiencia, vigencia y responsable editorial antes de usar el directorio como servicio institucional mantenido.
- Revisar periódicamente las fuentes y sus redirecciones. No inferir acceso, cupos, requisitos ni disponibilidad a partir de este resumen.
- Si se solicitan una reserva, préstamo o consulta personalizada, definir primero autoridad, contrato, privacidad, seguridad y relación con los sistemas vigentes. Esta página no crea esos flujos.
- No se agregan datos personales, categorías oficiales, campos de expediente, backend, tablas, migraciones ni contenido semilla.

## Pruebas relacionadas

Las pruebas de `frontend/src/features/students/StudentServicesPage.test.tsx` cubren las cuatro fichas atribuidas, destinos HTTPS seguros, búsqueda insensible a tildes, filtros combinados, contador, estado vacío y restablecimiento. `frontend/src/App.test.tsx` verifica la navegación a `/#estudiantes` y su estado activo.
