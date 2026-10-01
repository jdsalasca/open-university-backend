# Guía pública de espacios UPTC — diseño del primer corte

**Fecha:** 1 de octubre de 2026

**Estado:** alcance técnico para vista previa local; no equivale a aceptación institucional ni a un inventario exhaustivo.

## Propósito

Ayudar a estudiantes y visitantes a encontrar sedes, centros regionales y algunos servicios universitarios usando referencias que la UPTC publica. La primera versión debe permitir buscar y filtrar lugares, mostrar su dirección o referencia física con procedencia y abrir una búsqueda voluntaria en un mapa externo.

## Evidencia y alcance de datos

- La página oficial [Localización y sedes](https://uptc.edu.co/sitio/portal/sitios/localizacion/) publica seis sedes/seccionales/regionales y once ubicaciones CREAD; indicaba actualización del 3 de julio de 2026. Los CREAD se identifican aparte de los campus de la Universidad porque varias ubicaciones corresponden a instalaciones comunitarias o asociadas.
- El [Directorio UPTC](https://www.uptc.edu.co/sitio/portal/sitios/directorio/) expone una columna «Ubicación» y señalaba actualización del 11 de agosto de 2026. La aplicación enlaza a ese directorio para consultas completas; no copia nombres, teléfonos ni correos de personas.
- Se pueden mostrar cuatro puntos de servicio publicados: ACRA en el Edificio de Admisiones de Tunja; Ingeniería de Sistemas en el Edificio Central, segundo piso; el Instituto Internacional de Idiomas en el Edificio de Bienestar de Sogamoso; y la Escuela de Posgrados de Ciencias en el Edificio de Posgrados, oficina 103. Cada punto conserva su propia página oficial como fuente.
- Los planos localizados tienen fecha antigua y no demuestran el estado actual de edificios, accesos o recorridos. No se usarán para inventar coordenadas, rutas interiores ni atributos de accesibilidad.

## Decisión de arquitectura

Se implementa una capacidad de lectura pública en el monolito Spring Boot y una página React. El backend sirve una instantánea JSON versionada en el classpath mediante una interfaz de consulta (`PublicSpaceDirectory`); la fuente se puede sustituir posteriormente por un adaptador de persistencia sin cambiar la respuesta HTTP ni la interfaz React. El primer corte no crea tablas MySQL: falta designar al responsable del inventario y aprobar un flujo administrativo de actualización.

La respuesta de `GET /api/v1/spaces` incluye una lista de lugares y, por registro, identidad estable, tipo, nombre, municipio/departamento cuando la fuente los especifica, dirección postal, detalle del edificio/oficina cuando aplica, referencia de búsqueda cartográfica y URL/fecha de la fuente. La fecha de consulta es `2026-10-01`; la fecha de actualización original se conserva solo cuando la página oficial la declara. Si la fuente solo describe una ubicación interior —como el CREAD Rondón, en el segundo piso de la biblioteca municipal— `address` y `mapQuery` son nulos: se conserva el detalle pero se evita generar una búsqueda cartográfica ambigua.

El navegador carga el catálogo una vez y filtra localmente por texto y tipo. La búsqueda ignora mayúsculas y diacríticos. «Abrir mapa» construye un enlace de búsqueda de OpenStreetMap con la dirección publicada y solo navega fuera del sistema después de que la persona lo activa; si falta una dirección no hay enlace. No hay geolocalización ni mapa incrustado. Una nota pide verificar los detalles con la fuente oficial.

Se añade `spaces` al catálogo del Centro de Identidad Visual para que su nombre y visibilidad en la navegación sean configurables. Ocultar el módulo elimina su enlace lateral, pero no cambia el carácter público ni bloquea una URL directa. La API solo permite `GET`; métodos no declarados permanecen denegados.

## Interfaz y errores

La ruta `/#espacios` ofrece selector por tipo, campo de búsqueda, contador de resultados, fichas y estados de carga, error con reintento y búsqueda sin resultados. Cada ficha diferencia sedes universitarias, CREAD y servicios; presenta dirección/fuente/fecha sin afirmar que la lista sea completa. La pantalla es de lectura pública, adaptable y operable por teclado.

## Fuera del primer corte

Edición de inventario, importación del directorio, coordenadas propias, ubicación del usuario, navegación edificio-a-edificio, salones, horarios en vivo y afirmaciones sobre accesibilidad física. Estas funciones requieren inventario cartográfico vigente, responsable institucional, validación de campos y proceso de actualización. No se modelan personas ni se procesan datos personales.

## Verificación

Pruebas AAA deben cubrir fuentes y tipos en la instantánea, unicidad de identificadores, lectura anónima y denegación de otros métodos, búsqueda con tildes, filtros combinados, estado vacío/error/reintento, enlaces cartográficos construidos desde direcciones, navegación configurable y acceso directo a la página. Las suites completas de ambos repositorios, build, lint, Compose y diagramas C4/proceso verifican la integración. No se realiza trabajo de optimización ni se anuncia un SLO con esta entrega.
