# Instantánea del directorio público UPTC de pregrado — 2 de octubre de 2026

## Fuente y alcance

Fuente primaria: [Programas de pregrado UPTC](https://www.uptc.edu.co/sitio/portal/sitios/programas_ofer/pregrado.html). La página indica que la información fue actualizada el **15 de septiembre de 2026**. La instantánea local se capturó el **2 de octubre de 2026** desde el archivo público enlazado por esa página.

Se conservaron los registros clasificados en la fuente como nivel **Pregrado** y con el indicador de registro vigente afirmativo. El resultado contiene **79 programas en 11 facultades**. Dentro de esos registros, **72** llevan el indicador textual afirmativo que se presenta como «Programa ofertado». Este último valor se copia como una marca de la publicación; no permite concluir que haya convocatoria abierta, fechas vigentes, cupos disponibles o admisión.

Cada tarjeta enlaza a la ficha oficial correspondiente. El snapshot contiene identificador de referencia, nombre, facultad/código, nivel, modalidad, lugar, resumen de lugares cuando se publica, el indicador literal y la URL de detalle. No incluye información personal.

## Ubicación y uso en la aplicación

El archivo versionado está en `frontend/src/features/academics/publicCatalog/uptcUndergraduateCatalog.snapshot.json`. Vite lo emite como un asset JSON con nombre hash y React lo solicita desde el mismo origen; el navegador filtra los registros localmente y puede reutilizar la copia cacheada. No se hace una llamada al sitio UPTC durante la navegación, ni se agregó API backend, tabla MySQL, seed o escritura administrativa.

El directorio se presenta al comienzo de `/#programas`; la sección separada «Versiones publicadas en esta plataforma» consulta el maestro curricular interno. La instantánea pública no crea ni actualiza facultades, programas, afiliaciones, currículos, periodos u oferta de la plataforma.

## Actualización y control

Al refrescar la instantánea:

1. Revisar la página oficial y el archivo público al que remite.
2. Registrar las fechas de actualización que declara la fuente y la fecha de captura local.
3. Repetir el filtro documentado y comprobar conteos, identificadores únicos, facultades y URLs oficiales.
4. Revisar las diferencias por programa y no traducir la marca «Programa ofertado» a convocatoria o disponibilidad.
5. Mantener el archivo separado de migraciones y seeds del maestro académico.

La consulta del asset estático puede fallar temporalmente. Por eso el cliente presenta carga, error, reintento y un enlace de respaldo a la publicación oficial; una instantánea local no equivale a sincronización en tiempo real.
