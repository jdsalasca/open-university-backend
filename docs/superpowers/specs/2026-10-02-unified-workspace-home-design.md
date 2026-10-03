# Portada del portal por capacidades — diseño

## Problema y objetivo

La navegación muestra un botón «Resumen» deshabilitado y obliga a buscar cada experiencia en la barra lateral. La plataforma ya obtiene permisos efectivos desde `/api/v1/me`, tiene módulos públicos configurables y ofrece una sesión local acotada para revisar funciones administrativas. La portada debe servir como entrada clara para comunidad y personal, exponer rutas por capacidades reales y aplicar en la portada el banner administrable del Centro de Identidad Visual.

## Enfoque

Agregar `/#resumen` al monolito React existente y usarlo como página inicial cuando la URL no contiene un fragmento conocido. Mantener `/#inicio` para el Centro de Identidad Visual. Un componente `WorkspaceHomePage` recibe la marca pública, los permisos efectivos y los indicadores de sesión/desarrollo desde `ApplicationShell`; no consulta permisos propios ni interpreta roles.

La portada tendrá:

- Un encabezado institucional y el banner `home-hero` activo en el instante actual, con fechas de inicio inclusivas y fin exclusivo. Si no hay banner activo, conserva un hero tipográfico sin imagen.
- Accesos públicos configurables a convocatorias y guía de espacios, más el directorio UPTC de pregrado atribuido y separado del maestro curricular interno.
- Una sección administrativa que agrega enlaces existentes solo cuando `/api/v1/me` confirma al menos permiso de lectura de la capacidad correspondiente. Identidad visual, catálogo, estructura/periodos/oferta, calendario de admisiones y accesos conservan sus rutas y autorización actuales.
- Ningún acceso de portada a formularios, bandejas o escenarios académicos sintéticos. Los módulos demo aislados que se conserven para pruebas no tienen ruta de producto ni se cargan desde la portada.
- Mensajes que no revelan `subject`, correo, nombre ni otra información personal. La interfaz describe accesos por permisos efectivos y deja al backend la autorización final.

La página será adaptable, navegable por teclado y compatible con tema claro/oscuro. La navegación móvil podrá desplazarse horizontalmente para no comprimir las rutas actuales.

## Alternativas consideradas

1. Habilitar únicamente el botón Resumen y reutilizar un módulo existente: menos trabajo, pero no resuelve la entrada por capacidades.
2. Agregar un micrositio independiente: duplica shell, sesión y estilo, y separa el inicio del monolito aprobado.
3. Añadir la portada al shell existente (seleccionada): reutiliza branding, sesión y rutas; no agrega servicios, tablas, roles, permisos ni dependencias.

## Límites

- No agregar autenticación, asignación de roles, datos de perfil, captura de aspirantes, estado persistente ni API.
- No convertir las vistas previas o laboratorios en procesos institucionales.
- No modificar reglas, permisos, calendarios ni fuentes de los módulos existentes.
- El Centro de Identidad Visual sigue accesible en `/#inicio`; el portal usa `/#resumen`.

## Criterios de aceptación

1. Un acceso nuevo sin fragmento aterriza en `/#resumen`; el enlace «Resumen» y el lockup institucional abren la portada, y `/#inicio` conserva el Centro de Identidad Visual.
2. La portada muestra solo los banners `home-hero` dentro de su vigencia y usa texto alternativo y asset institucional existentes.
3. Los enlaces administrativos aparecen únicamente para permisos confirmados por `/api/v1/me`, incluso si el rol visible no está disponible; no se deducen permisos por etiqueta o sesión local.
4. Las tarjetas públicas respetan visibilidad institucional y presentan el directorio de programas como consulta de una instantánea oficial con su procedencia.
5. La portada y la navegación no ofrecen rutas de laboratorios sintéticos ni siquiera en Vite DEV.
6. No se muestran datos personales; el build de producción no incluye módulos demo desconectados.
7. `npm test`, `npm run build` y `npm run lint` pasan; la navegación y los componentes nuevos tienen pruebas AAA de permisos, banner, rutas y DEV/prod.
8. C4, flujo de navegación, roadmap e instrucciones del frontend describen la nueva portada y sus límites.
