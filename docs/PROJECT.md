# Universiry — plataforma institucional UPTC

## Propósito

Construir una plataforma institucional única que, por etapas y dominio de negocio, unifique y reemplace los sistemas que hoy apoyan la vida universitaria de la UPTC. El objetivo incluye admisiones, expediente y ciclo del estudiante, estructura académica, currículos, mallas, asignaturas, carga académica, registro, bienestar, talento humano, finanzas, investigación, extensión y procesos administrativos. La lista definitiva, las reglas vigentes y las integraciones se confirman con los responsables institucionales durante el descubrimiento.

Un inventario público de DTIC de 2024 relaciona sistemas como SIRA, SIRD, SEDI, HUMANO, OLIB, GOOBI, SIPRO, SIIUPS y otros sobre Oracle y MySQL. Se conserva como antecedente histórico: no demuestra el estado actual ni sustituye el inventario autorizado. [Fuente UPTC](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/rectoria/planeacion/sig/.content/doc/rdc/2024/inf_revdtic_24.pdf)

## Decisiones confirmadas

- Dos repositorios privados coordinados: `Universiry-frontend` (monolito Vite/React/TypeScript) y `Universiry-backend` (monolito Java/Spring Boot). Ambos integran en `develop`.
- El checkout backend incluye el frontend como submódulo para alojar el Compose local que levanta frontend, backend y MySQL. Esta relación de checkout no comparte código fuente ni despliegues entre las aplicaciones.
- Frontend: Vite, React y TypeScript.
- Backend: Java 25 administrado por SDKMAN y Spring Boot; el build y el `.sdkmanrc` fijan versión exacta.
- Backend como monolito modular, separado por capacidades de negocio. Los microservicios quedan aplazados hasta que haya evidencia de una frontera y una ganancia concreta.
- MySQL es la base relacional objetivo. Migraciones explícitas, integridad referencial y propiedad clara de datos por dominio.
- TDD en ciclos RED-GREEN-REFACTOR; pruebas con estructura AAA, casos felices, límites, permisos y regresiones.
- Diseño por contratos e interfaces, cohesión alta, acoplamiento bajo, principios SOLID y revisión de duplicaciones.
- Centro de Identidad Visual administrable: paleta, logos, imágenes institucionales, banners y nombres visibles de módulos, con autorización, vista previa y auditoría.
- Mensajes del backend resueltos por `Accept-Language`, con español de Colombia como idioma predeterminado.
- Compose local con MySQL y recarga de frontend/backend al cambiar código.
- Actualizar diagramas C4 y procesos al modificar componentes, límites, integraciones o cortes de migración.
- Objetivo de consultas críticas MySQL: promedio <50 ms bajo carga y volumen acordados; p95/p99 acompañan el promedio.

## Usuarios y permisos preliminares

Los grupos exactos y su mapeo al proveedor institucional se confirman con UPTC. El diseño inicial distingue comunidad universitaria, operadores académicos, personal administrativo, docentes, soporte técnico, administradores de identidad visual y administradores institucionales. El backend traduce los grupos reconocidos a permisos internos de la capacidad; ocultar elementos en React no es un control de acceso. Permite solamente pares método/ruta que estén registrados y probados, y deniega el resto de `/api/v1/admin/**` para que un futuro módulo no herede permisos por compartir un prefijo.

## Alcance del primer incremento

1. Dos repositorios reproducibles, herramientas de contexto IA, Compose Watch con MySQL local, traducciones del backend, base versionada y documentación viva.
2. Centro de Identidad Visual que publica una configuración de marca validada: colores, logos, banners y nombres de módulos.
3. Estructura de navegación y contratos preparados para identidad, estudiante y catálogo académico, sin inventar datos reales ni marcar módulos futuros como funcionales.
4. Inventario de integraciones y requisitos institucionales como condición previa a la autenticación federada, migraciones de datos productivos y cortes oficiales.

## Límites de seguridad y publicación

- El desarrollo usa solamente datos sintéticos.
- Las imágenes se validan por contenido real, dimensiones y tamaño; se excluyen SVG activos.
- El almacenamiento local de activos se limita al entorno de desarrollo. El almacenamiento productivo compartido se define con DTIC antes de desplegar más de una instancia.
- La identidad institucional y la lista oficial de roles son dependencias externas por descubrir. No se publicará un centro administrativo sin un proveedor y un mapeo de permisos aprobados.
- Una migración de dominio requiere perfilado, conciliación, aceptación del dueño de datos, ensayo de reversa y aprobación de corte. No habrá doble escritura sin reconciliación.

## Identidad visual inicial

El manual de identidad gráfica UPTC 2022 especifica amarillo `#FFCC29` (RGB 255/204/41) y negro `#1A1A1A` (RGB 26/26/26). La página oficial lista archivos de logotipos UPTC 2026. Los valores iniciales se documentan con estas fuentes y permanecen configurables; una actualización institucional prevalece. [Manual gráfico](https://www.uptc.edu.co/sitio/export/sites/default/portal/sitios/universidad/rectoria/comunicaciones/.content/doc/manual/man_identgraf_2022.pdf) · [Logos oficiales 2026](https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/comunicaciones/9_identidad_grafica/)
