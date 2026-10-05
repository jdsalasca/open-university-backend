# ADR-0005: Repositorio único `open-university-os`

- **Estado:** Aceptado por el patrocinador
- **Fecha:** 2026-10-05
- **Alcance:** nombre del proyecto, estructura del repositorio y motores de datos de desarrollo local

## Contexto

El proyecto venía distribuido en dos repositorios públicos coordinados: `open-university-backend`
contenía el monolito Java/Spring Boot, el Compose y la documentación de integración, y mantenía el
frontend Vite/React/TypeScript como submódulo en `frontend/`. Coordinar un cambio que atraviesa
ambos monolitos exigía dos push, dos CI y la actualización manual del puntero del submódulo.

En paralelo, `compose.yaml` no declaraba el nombre del proyecto, de modo que sus volúmenes se
llamaban `universiry_*` y renombrar cualquier servicio habría huérfano los datos de MySQL.

## Decisión

- El proyecto se llama **`open-university-os`** y es un monorepo: `backend/` con el monolito
  Java/Spring Boot y `frontend/` con el monolito Vite/React/TypeScript, con un único `develop` y un
  único CI.
- `compose.yaml` declara `name: open-university-os`. Como el nombre del proyecto es el prefijo de
  sus volúmenes, se copiaron los volúmenes existentes antes de levantar el stack nuevo.
- MongoDB 8 queda aprovisionado en el Compose para documentos que no encajen en el modelo relacional,
  pero ningún puerto Spring lo consume todavía. No se crea una segunda fuente de verdad sin un
  dominio asignado y su dueño.
- El perfil Spring `sqlite` permite ejecutar el backend sin contenedor para pruebas. MySQL 8.4 sigue
  siendo el motor relacional objetivo y Flyway versiona el esquema.
- Hasta que el contenido real del frontend esté en el repositorio único, el submódulo se conserva en
  `frontend/` y no se elimina su puntero.

## Consecuencias

- Un cambio que cruza frontend y backend entra en un solo commit y un solo CI.
- Los dos remotos actuales se conservan intactos como origen y respaldo durante la transición.
- La base y el usuario MySQL develop mantienen sus nombres históricos: renombrarlos invalidaría los
  permisos ya concedidos dentro del volumen copiado.
- MongoDB disponible no equivale a funcionalidad NoSQL; cada dominio que lo consuma debe registrar su
  propietario y su fuente de verdad.
