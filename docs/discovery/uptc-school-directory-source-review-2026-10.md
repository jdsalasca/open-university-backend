# Revisión de fuentes para directorio de establecimientos educativos

**Corte de consulta:** 2026-10-02
**Propósito:** identificar fuentes públicas para establecimientos educativos de origen de aspirantes a pregrado presencial UPTC.
**Estado:** revisión pública exploratoria. No designa dueño de datos UPTC, no valida campos del formulario y no autoriza una integración ni el uso de datos reales.

## Hallazgos

| Fuente | Evidencia observada | Uso y límite |
|---|---|---|
| [Buscando Colegio / DUE del MEN](https://sineb.mineducacion.gov.co/bcol/app?service=page/BuscandoColegio) | El portal público carga un formulario de consulta avanzada con departamento, municipio, establecimiento, código DUE, sede, código DUE de sede, sector y otros filtros. | Es un canal oficial para consulta manual. En esta revisión no se identificó un contrato de API pública documentado para integrarlo como servicio. No automatizar consultas ni depender de la estructura del portal. |
| [Guía MEN para reporte a DUE, SIMAT, EVI y EDUC (abril de 2018)](https://www.mineducacion.gov.co/1759/articles-369048_recurso_1.pdf) | Describe DUE como directorio oficial de educación preescolar, básica y media, con administración MEN y actualización por secretarías certificadas. También indica que el público puede consultar establecimientos y sedes, y describe descarga de resultados a Excel desde la interfaz. | Aporta el modelo de autoridad y consulta, pero es una guía de 2018; no prueba que la exportación descrita siga operando igual ni define una interfaz de integración actual. No usarla para asumir campos obligatorios de admisión universitaria. |
| [Bases consolidadas SINEB](https://portalsineb.mineducacion.gov.co/portal/secciones/Informacion-Estadistica/Bases-consolidadas/) | La página consultada enumera como última base nominal de establecimientos educativos y sedes el año 2022. La publicación presenta por separado bases consolidadas y estadísticas. Durante la revisión, el enlace de descarga de establecimientos 2022 no pudo recuperarse desde el navegador de investigación. | No hay una extracción nominal más reciente validada aquí. La imposibilidad de recuperar ese enlace debe comprobarse por un canal MEN antes de intentar cualquier ingestión; no equivale a afirmar que el MEN no conserve datos más recientes en DUE. |
| [Conjunto “Directorio único de establecimientos educativos (DUE)” en Datos Abiertos Colombia](https://www.datos.gov.co/dataset/Directorio-nico-de-establecimientos-educativos-DUE/28t6-6wvz) | La ficha pública muestra archivos anuales hasta DUE 2022 y una fecha de actualización del registro de catálogo del 31 de octubre de 2025. | La fecha de actualización de la ficha no convierte en 2025 los registros del archivo 2022. No cargar ese snapshot como directorio actual de aspirantes. |
| [Boletín SINEB “Establecimientos y Sedes 2015–2024”, Boletín Estadístico 2025](https://portalsineb.mineducacion.gov.co/1782/articles-412174_Boletin_Sedes_2015_2024.pdf) | MEN explica que estas estadísticas se recopilan mediante DUE y publica cifras agregadas hasta 2024; por ejemplo, el total nacional reportado es 17.997 establecimientos para 2024. | Sirve para contexto y para confirmar que el DUE sigue siendo una fuente estadística relevante; no contiene un maestro nominal actual para selector o registro de aspirante. |

## Decisión para esta plataforma

- Mantener `TerritorialCatalog` y el snapshot DIVIPOLA separados del directorio escolar. DIVIPOLA resuelve división político-administrativa; no identifica colegios ni sedes educativas.
- No importar la base nominal 2022 ni los conjuntos no oficiales localizados durante la búsqueda como si fueran datos vigentes.
- No extraer ni raspar el portal Buscando Colegio. Si UPTC decide usar el directorio, solicitar a MEN/DTIC un contrato autorizado o una extracción oficial con fecha de corte, esquema, cobertura, licencia, frecuencia de actualización y responsable.
- Antes de persistir institución de procedencia de una persona, confirmar con ACRA y la autoridad institucional de datos si ese dato se recoge, su propósito, conjunto mínimo de campos, manejo de instituciones no encontradas y si el código DUE es el identificador aceptado.
- Si se aprueba la fuente, introducir un puerto `EducationalEstablishmentDirectory` y su adaptador/snapshot con controles de fecha y procedencia. No consultar MEN sincrónicamente al enviar una inscripción y no duplicar campos de dirección/contacto si el proceso no los necesita.

## Siguiente evidencia necesaria

1. Responsable UPTC de admisiones valida el dato de institución educativa y sus reglas por convocatoria.
2. ACRA, DTIC y el responsable de privacidad confirman campos, finalidad, fuente maestra, acceso y retención.
3. MEN/DTIC confirma vía de entrega autorizada, versión, uso permitido, identificador, cobertura, calendario de actualización y contacto operativo.
4. Se valida una muestra contra el portal oficial y el archivo fuente antes de habilitar una consulta integrada.

Esta revisión no cambia C4, modelo de datos ni proceso de admisión: todavía no existe una integración escolar autorizada. La UI y API del catálogo territorial permanecen fuera del formulario de inscripción.
