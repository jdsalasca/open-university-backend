# Compose con MySQL, MongoDB y perfil SQLite — 5 de octubre de 2026

Primer corte del encargo de migrar a un único repositorio `open-university-os`. Este corte
resuelve la parte de infraestructura de datos y levanta el stack completo; el monorepo como tal
viene en el corte siguiente, porque cambiar la estructura del repositorio mientras el Compose no
está verificado haría más difícil distinguir un fallo de entorno de uno de código.

## Servicios

| Servicio | Imagen | Puerto local | Estado verificado |
| --- | --- | --- | --- |
| `mysql` | `mysql:8.4` | 3307 | healthy, 44 tablas, Flyway V24–V27 aplicadas |
| `mongodb` | `mongo:8` | 27017 | healthy, `ping` → 1 |
| `backend` | build `./backend` | 8080 | `/actuator/health` → liveness/readiness/status `UP` |
| `frontend` | build `./frontend` | 5173 | HTTP 200, renderiza y consume la API |

El backend espera a que MySQL **y** MongoDB estén `healthy` antes de arrancar.

## MongoDB: aprovisionado, todavía sin usar

MongoDB se levanta y es accesible, pero **ningún puerto Spring lo consume y no existe ningún
repositorio de Mongo**. Se deliberó así: añadir un motor de documentos era una decisión de
arquitectura, y aprovisionarlo sin un dominio asignado evitaría que se convierta en una segunda
fuente de verdad. El backend recibe `MONGODB_URI` para que el primer dominio que lo necesite no
tenga que tocar el Compose.

## Perfil SQLite

SQLite no es un servidor: es un archivo. Se añade como perfil de Spring para ejecutar el backend
aislado, sin contenedor de base de datos:

- `backend/src/test/resources/application-sqlite.properties` — URL `jdbc:sqlite:file:./target/...`
  con `mode=memory&cache=shared`, Flyway desactivado porque las migraciones son específicas de
  MySQL.
- `sqlite-jdbc` en `pom.xml` con `scope=test`.
- `SqliteProfileContextTest` prueba que el contexto de Spring arranca y que `select 1` responde.

Verificación real:

```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
The following 1 profile is active: "sqlite"
HikariPool-1 - Added connection org.sqlite.jdbc4.JDBC4Connection
```

MySQL 8.4 sigue siendo el motor objetivo: SQLite solo sostiene ejecuciones aisladas.

## Preservación de datos al cambiar de nombre

El proyecto Compose pasó de `universiry` a `open-university-os`, lo que cambia el prefijo de los
volúmenes. Antes de tocar nada se copiaron los volúmenes existentes:

```
docker volume create open-university-os_mysql-data
docker run --rm -v universiry_mysql-data:/from -v open-university-os_mysql-data:/to \
  alpine sh -c "cp -a /from/. /to/"
```

El historial de Flyway sobrevivió: 44 tablas y las versiones 24 a 27 siguen aplicadas.

**El nombre de la base y del usuario MySQL no cambió** (`universiry_dev`). Renombrarlos habría
huérfado los permisos que ya vivían en el volumen y el backend habría fallado con
`Access denied for user`. Está anotado en el propio `compose.yaml`: el renombrado del proyecto no
debe arrastrar las credenciales.

## Contratos MySQL ejecutados por fin

Con Docker disponible, los contratos MySQL que antes solo corrían en CI se ejecutan en local:

```
UNIVERSIRY_MYSQL_TEST_URL=jdbc:mysql://127.0.0.1:3316/contract ...
mvn -Duniversiry.mysql-contract.enabled=true test
```

Resultado: **424 pruebas, 1 fallo, 0 errores, 3 saltadas**. Los contratos apuntan a un MySQL
desechable aparte (`open-university-os-mysql-contract`), nunca al de desarrollo.

El único fallo es `AcademicStructureMySqlPerformanceContractTest`: observa 60,6 ms frente al
objetivo de <50 ms. Es una medición de esta máquina —78 procesos de Node y 55 de Chrome
coexistiendo con Docker—, no una regresión. El propio AGENTS.md advierte que un runner no
hospedado no certifica el objetivo de latencia; la CI remota es la autoridad y ahí el test pasa.

## Cómo levantarlo

```bash
docker compose up -d --build
docker compose ps          # los cuatro servicios, con salud
```

Los volúmenes con datos se renombraron una vez; a partir de ahí `up -d` reutiliza
`open-university-os_mysql-data` y no hay que repetir la copia.

## Lo que este corte no hace

No crea el repositorio único `open-university-os`; los dos remotos siguen siendo
`open-university-backend` (con el frontend como submódulo) y `Universiry-frontend`. Tampoco
renombra el artefacto de Maven ni el paquete Java, que siguen en `universiry`. Nada de esto afecta
al runtime: el nombre del proyecto, los volúmenes y las variables de entorno ya están preparados.