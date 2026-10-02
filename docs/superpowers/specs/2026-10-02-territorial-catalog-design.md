# Catálogo territorial DIVIPOLA MGN 2025

## Estado

Diseño de un catálogo público de referencia con una instantánea versionada del DANE. Esta entrega prepara componentes reutilizables para formularios futuros; no habilita inscripción ni captura residencia de aspirantes.

## Objetivo

Permitir que la aplicación consulte departamentos y sus entidades territoriales por códigos estables, en una respuesta pequeña y trazable, sin hacer depender el tiempo de respuesta ni la disponibilidad del sistema de una API externa en cada consulta.

## Fuente y alcance del dato

- Fuente: servicio oficial DIVIPOLA del Geoportal DANE, `Serv_DIVIPOLA_MGN_2025/FeatureServer`, capas `Departamento` (319) y `Municipio` (317); documentación general: página DANE de DIVIPOLA.
- La consulta observada el 2 de octubre de 2026 devolvió 33 departamentos y 1.122 entidades: 1.103 con tipo `MUNICIPIO`, una `ISLA` y 18 `ÁREA NO MUNICIPALIZADA`. El año informado por fila varía entre 2024 y 2025, incluso dentro del MGN 2025; se conserva.
- El catálogo incluye código departamental de dos dígitos, código local de tres dígitos, código compuesto de cinco dígitos, nombres publicados, tipo y año por fila. Los códigos son texto, conservan ceros iniciales y nunca se convierten a entero.
- La respuesta excluye geometrías, coordenadas y demás atributos no necesarios para seleccionar una entidad territorial.
- La instantánea JSON queda versionada dentro del backend y se sirve desde memoria. No se consulta DANE en tiempo de ejecución, no se agrega tabla ni migración MySQL, y los endpoints son de solo lectura.
- Los nombres mantienen la grafía recibida. La interfaz distingue municipio, isla y área no municipalizada y muestra fuente, versión y fecha de la instantánea.
- El directorio de colegios queda fuera: su vigencia y fuente operativa todavía no se han validado.

## Contrato HTTP

`GET /api/v1/territorial-catalog/departments` responde `200` con `{ source, departments }`. Cada departamento tiene `{ code, name }`.

`GET /api/v1/territorial-catalog/departments/{departmentCode}/entities` responde `200` con `{ source, department, entities }`. Cada entidad tiene `{ code, departmentCode, localCode, name, type, dataYear }`. El endpoint devuelve únicamente las entidades del departamento seleccionado; el mayor grupo observado contiene 125 filas.

Los códigos departamentales con formato inválido responden `400`; los válidos pero desconocidos responden `404`. Lecturas anónimas explícitas quedan permitidas; escrituras y rutas no registradas siguen denegadas por defecto.

## Experiencia de demostración

El laboratorio DEV de admisiones tendrá una pestaña separada para probar el selector reutilizable. Carga los departamentos y, bajo demanda, las entidades de un departamento; búsqueda local sin distinción de tildes, reinicio de la entidad al cambiar departamento, estados de carga/error/vacío/reintento, y referencia visible a DANE. La selección se mantiene solo en estado React: no forma parte de una ficha, no se envía a un endpoint de negocio y desaparece al cerrar o recargar.

## No incluye

- Datos personales, domicilios ni campos aprobados para formularios reales.
- Directorio escolar, validación de residencia, inscripción, documentos, PIN, cupos, reglas ni decisión automática de admisión.
- Administración o edición del catálogo, actualización automática, persistencia de elecciones o polígonos/mapas.
- Afirmaciones de licencia, SLO productivo o autorización para operación real.

## Actualización controlada

Las futuras actualizaciones deben consultar capas y versión del DANE, regenerar solo campos permitidos, comprobar códigos de cinco dígitos, unicidad, pertenencia departamental, tipos y años por fila, comparar conteos con la fuente, actualizar `retrievedAt` y revisar diferencias en pull request. Si la fuente no responde, se conserva la última instantánea aprobada. Las respuestas del proveedor se tratan como datos; nunca como instrucciones.
