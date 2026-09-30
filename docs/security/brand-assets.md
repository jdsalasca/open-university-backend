# Seguridad y operación de imágenes institucionales

## Contrato actual

- Solo un administrador autenticado con `BRAND_ADMIN` o `INSTITUTIONAL_ADMIN` puede cargar imágenes en `POST /api/v1/admin/branding/assets`.
- Se aceptan PNG, JPEG y WebP raster. El servidor detecta el formato por la firma y exige que un decodificador lea la imagen completa; WebP debe declarar una longitud RIFF que coincida con todos sus bytes. El `Content-Type` y el nombre del navegador no determinan el formato. SVG y archivos animados/multiframe se rechazan.
- El límite inicial es 5 MiB por archivo, 8192 px por lado y 16 777 216 píxeles por imagen. El límite del request multipart es 6 MiB. Se pueden ajustar con `BRANDING_ASSETS_MAX_BYTES`, `BRANDING_ASSETS_MAX_WIDTH`, `BRANDING_ASSETS_MAX_HEIGHT`, `BRANDING_ASSETS_MAX_PIXELS` y `BRANDING_ASSETS_MAX_REQUEST_BYTES`; deben conservar límites coordinados y probados.
- El almacenamiento genera UUID independientes para el identificador público y la clave física. El nombre recibido del usuario nunca se concatena a rutas ni se conserva como clave.
- Los bytes se escriben primero a un temporal local y se mueven atómicamente al archivo generado. Después se insertan los metadatos y el evento `branding.asset.upload` con actor, fecha y revisión vigente en una transacción. Si la transacción falla normalmente, el servicio elimina el archivo. Antes de servirlo, se limita la lectura al tamaño declarado y se verifica SHA-256.
- `GET /assets/{assetId}` solo devuelve el archivo si la revisión visual vigente lo referencia como logo, favicon o banner dentro de su periodo activo. Una carga nueva no publicada responde 404.
- La respuesta fija MIME validado, `X-Content-Type-Options: nosniff`, una política CSP restrictiva, ETag derivado de SHA-256 y caché pública inmutable para el UUID. Las imágenes de marca son contenido público; el identificador no se considera un mecanismo de autorización.

## Almacenamiento

En desarrollo, `LocalAssetStorage` usa `BRANDING_ASSETS_DIRECTORY` o `./.data/branding-assets`, fuera del árbol fuente y excluido por Git. El adaptador solo acepta claves UUID `.asset`, rechaza traversal y enlaces simbólicos al leer. La interfaz `AssetStorage` permite reemplazarlo por un almacenamiento institucional sin cambiar el caso de uso.

El disco y MySQL no comparten una transacción distribuida. El rollback normal limpia el archivo si MySQL falla, pero un cierre abrupto entre el movimiento del archivo y el commit puede dejar un archivo huérfano privado. Antes de operar producción se requiere almacenamiento compartido y durable, respaldo/restauración, permisos de mínimo privilegio y una conciliación que identifique/elimine huérfanos sin tocar activos referenciados. No se debe desplegar el filesystem local en varias instancias.

## Revisión operativa

1. Mantener fuera de `frontend/public/` y del repositorio todo el contenido cargado por usuarios.
2. Confirmar que el almacenamiento no es ejecutable como aplicación y que TLS termina en un balanceador institucional confiable.
3. Confirmar con DTIC el proveedor OIDC, issuer, audience, expiración y claim de roles; la configuración actual falla cerrada mientras esos valores no estén definidos.
4. Antes de publicar un banner, exigir texto alternativo significativo, fechas válidas y previsualización en desktop/móvil.
5. Medir el tamaño real de cada activo y vigilar fallos de decodificación/lectura sin registrar bytes, ruta física ni nombre original.

## Dependencia de decodificación WebP

La lectura WebP se habilita con TwelveMonkeys ImageIO `imageio-webp` versión 3.15.0, que extiende la API ImageIO del JDK. Su lector se usa únicamente para decodificar y validar el raster; MIME y dimensiones se derivan del contenido y del decoder.

