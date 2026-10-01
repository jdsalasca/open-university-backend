# Guía pública de espacios UPTC — diseño del primer corte

**Fecha:** 1 de octubre de 2026

**Estado:** alcance técnico para vista previa local; no equivale a aceptación institucional ni a un inventario exhaustivo.

## Propósito

Ayudar a estudiantes, docentes, personal y visitantes a encontrar sedes, centros regionales y algunos servicios universitarios usando referencias que la UPTC publica. La primera versión también orienta hacia canales institucionales distintos para consultar préstamo, asignación o alquiler de determinados espacios, sin recibir solicitudes ni afirmar disponibilidad.

## Evidencia y alcance de datos

- La página oficial [Localización y sedes](https://uptc.edu.co/sitio/portal/sitios/localizacion/) publica seis sedes/seccionales/regionales y once ubicaciones CREAD; indicaba actualización del 3 de julio de 2026. Los CREAD se identifican aparte de los campus de la Universidad porque varias ubicaciones corresponden a instalaciones comunitarias o asociadas.
- El [Directorio UPTC](https://www.uptc.edu.co/sitio/portal/sitios/directorio/) expone una columna «Ubicación» y señalaba actualización del 11 de agosto de 2026. La aplicación enlaza a ese directorio para consultas completas; no copia nombres, teléfonos ni correos de personas.
- Se pueden mostrar cuatro puntos de servicio publicados: ACRA en el Edificio de Admisiones de Tunja; Ingeniería de Sistemas en el Edificio Central, segundo piso; el Instituto Internacional de Idiomas en el Edificio de Bienestar de Sogamoso; y la Escuela de Posgrados de Ciencias en el Edificio de Posgrados, oficina 103. Cada punto conserva su propia página oficial como fuente.
- Los planos localizados tienen fecha antigua y no demuestran el estado actual de edificios, accesos o recorridos. No se usarán para inventar coordenadas, rutas interiores ni atributos de accesibilidad.

## Decisión de arquitectura

Se implementa una capacidad de lectura pública en el monolito Spring Boot y una página React. El backend sirve una instantánea JSON versionada en el classpath mediante una interfaz de consulta (`PublicSpaceDirectory`); la fuente se puede sustituir posteriormente por un adaptador de persistencia sin cambiar la respuesta HTTP ni la interfaz React. El primer corte no crea tablas MySQL: falta designar al responsable del inventario y aprobar un flujo administrativo de actualización.

La respuesta de `GET /api/v1/spaces` incluye `locations` y `requestPathways`. Cada ubicación conserva identidad estable, tipo, nombre, municipio/departamento cuando la fuente los especifica, dirección postal, detalle del edificio/oficina cuando aplica, referencia de búsqueda cartográfica y URL/fecha de la fuente. Cada una de las cinco rutas de uso de espacios presenta tipo de servicio, audiencia descrita por la fuente, resumen, nota de disponibilidad y una o más fuentes oficiales con fecha de consulta. La fecha de consulta es `2026-10-01`; la fecha de actualización original se conserva solo cuando la página oficial la declara. Si la fuente solo describe una ubicación interior —como el CREAD Rondón, en el segundo piso de la biblioteca municipal— `address` y `mapQuery` son nulos: se conserva el detalle pero se evita generar una búsqueda cartográfica ambigua.

El navegador carga el catálogo una vez y filtra localmente por texto y tipo. La búsqueda ignora mayúsculas y diacríticos. «Abrir mapa» construye un enlace de búsqueda de OpenStreetMap con la dirección publicada y solo navega fuera del sistema después de que la persona lo activa; si falta una dirección no hay enlace. No hay geolocalización ni mapa incrustado. Una nota pide verificar los detalles con la fuente oficial.

Se añade `spaces` al catálogo del Centro de Identidad Visual para que su nombre y visibilidad en la navegación sean configurables. Ocultar el módulo elimina su enlace lateral, pero no cambia el carácter público ni bloquea una URL directa. La API solo permite `GET`; métodos no declarados permanecen denegados.

## Interfaz y errores

La ruta `/#espacios` ofrece selector por tipo, campo de búsqueda, contador de resultados, fichas y estados de carga, error con reintento y búsqueda sin resultados. Cada ficha diferencia sedes universitarias, CREAD y servicios; presenta dirección/fuente/fecha sin afirmar que la lista sea completa. La sección «Préstamo, asignación y alquiler» separa auditorios/espacios académicos, escenarios deportivos, salas de biblioteca, aulas de informática y Break Room de personal. Cada tarjeta enlaza a sus fuentes, advierte que no hay disponibilidad en tiempo real y conserva reglas o diferencias de aforo como motivos para consultar a la unidad responsable. La pantalla es de lectura pública, adaptable y operable por teclado.

## Fuera del primer corte

Edición de inventario, importación del directorio, coordenadas propias, ubicación del usuario, navegación edificio-a-edificio, salones, horarios en vivo, búsqueda de cupos, envío de solicitudes, reserva, aprobación, tarifa y afirmaciones sobre accesibilidad física. Las rutas de orientación abren las páginas oficiales en otra pestaña; no escriben en esos sistemas ni sustituyen sus procesos. Las funciones de operación requieren inventario vigente, responsable institucional, validación de campos y procesos aprobados. No se modelan personas ni se procesan datos personales.

## Verificación

Pruebas AAA deben cubrir fuentes y tipos en la instantánea, unicidad de identificadores, lectura anónima y denegación de otros métodos, búsqueda con tildes, filtros combinados, estado vacío/error/reintento, enlaces cartográficos construidos desde direcciones, navegación configurable y acceso directo a la página. Las suites completas de ambos repositorios, build, lint, Compose y diagramas C4/proceso verifican la integración. No se realiza trabajo de optimización ni se anuncia un SLO con esta entrega.
