# Especificación — Plataforma base y Centro de Identidad Visual

**Estado:** aprobada por el usuario el 2026-09-29 como inicio de implementación.

## Resultado esperado

Dos repositorios privados ejecutables localmente en Windows: frontend React/Vite/SCSS y backend Java 25/Spring Boot, persistencia relacional MySQL versionada, instrucciones AGENTS-first y un Centro de Identidad Visual que solo edita personal autorizado. Compose coordina los servicios de desarrollo sin fusionar los repositorios ni sus despliegues.

## Requisitos funcionales

1. La aplicación consume configuración institucional pública versionada: nombre visible, tokens de color, recursos de marca, banners publicados y etiquetas de módulos.
2. Administradores autorizados editan paleta, logos y banners, orden/vigencia/alternativo y nombres visibles de módulos permitidos.
3. El administrador previsualiza los cambios en el navegador antes de publicarlos. Cada publicación guarda un snapshot inmutable; restaurar una versión antigua crea una revisión nueva y monotónica.
4. El catálogo solo admite módulos conocidos por código, mantiene claves técnicas estables y bloquea cambios que dejen inaccesible la administración.
5. El API registra actor institucional, acción, versión y fecha en cada publicación de configuración o activo.
6. El endpoint público devuelve solo datos visuales; no entrega usuarios, permisos, nombres originales de archivos ni detalles de auditoría.
7. Si el API público está fuera de servicio, el frontend conserva una paleta oficial local y muestra estado visible sin romper navegación ni controles de foco.
8. Imágenes aceptadas inicialmente: PNG, JPEG y WebP; validar firma real, límites configurables de bytes y dimensiones, y texto alternativo en contenido editorial.
9. Los tokens de color iniciales son `primary`, `ink`, `surface`, `text`, `accent` y `focus`. Los pares texto/fondo deben alcanzar contraste WCAG AA de 4.5:1 (3:1 para texto grande y componentes gráficos); se muestra la razón durante la edición y se bloquea la publicación si el contraste de texto normal no cumple.
10. El catálogo de claves inicial es `home`, `students`, `programs`, `curricula`, `subjects`, `academic-load` y `visual-identity`. Las claves técnicas no cambian; el administrador modifica etiquetas. La UI no publica módulos que el backend aún no marca como disponibles y el centro visual siempre permanece accesible.
11. Navegación e interfaz se adaptan a móvil, permiten teclado, muestran foco, errores y estados vacíos, y respetan la paleta dinámica con contraste legible.

## Configuración pública mínima

```json
{
  "revision": 1,
  "institutionName": "Universidad Pedagógica y Tecnológica de Colombia",
  "colors": {
    "primary": "#FFCC29",
    "ink": "#1A1A1A",
    "surface": "#FFFFFF",
    "text": "#1A1A1A",
    "accent": "#FFCC29",
    "focus": "#1A1A1A"
  },
  "assets": {"logoLight": null, "logoDark": null, "favicon": null},
  "modules": [{"key": "visual-identity", "label": "Identidad visual", "available": true, "visible": true, "order": 90}],
  "banners": []
}
```

El API no devuelve actor, ruta física, MIME no validado ni historial de auditoría en esta respuesta.

## Requisitos de calidad

- Java 25, SDKMAN, Spring Boot estable compatible con Java 25; la versión se fija junto con Maven Wrapper.
- Dos monolitos desplegables independientemente en repositorios públicos `open-university-frontend` y `open-university-backend`, ramas `develop`. El código público no habilita datos ni operación institucional.
- Compose local levanta frontend, backend y MySQL; no se usa como manifiesto productivo.
- El backend sirve bundles es-CO por defecto e inglés al negociar `Accept-Language`.
- TDD/AAA obligatorio en reglas, API, persistencia, seguridad, validación de activos y UI.
- Autorización administrativa en servidor; el proveedor de identidad y claims UPTC siguen pendientes de confirmación institucional.
- El API usa JWT de acceso en `Authorization: Bearer`; no mantiene autenticación en cookie ni sesión. CSRF solo se desactiva mientras esa condición se mantenga; si se adopta una cookie, se debe habilitar protección CSRF antes del despliegue.
- SQL de producción usa MySQL y migraciones Flyway. Las pruebas aisladas no sustituyen la verificación final contra MySQL.
- Latencia objetivo: consultas críticas definidas <50 ms promedio bajo conjunto de datos y carga reproducibles; añadir p95/p99.
- No publicar como listo para producción hasta integrar SSO institucional, almacenamiento institucional persistente, respaldo/restauración, observabilidad y cortes aprobados.

## Contratos API iniciales

| Método y ruta | Acceso | Contrato |
|---|---|---|
| `GET /api/v1/me` | Token Bearer autenticado | Subject y permisos internos conocidos del usuario actual; no devuelve claims de perfil; `Cache-Control: no-store` |
| `GET /api/v1/branding` | Público | Configuración visual vigente y revisión/ETag; solo campos necesarios para renderizar |
| `GET /api/v1/admin/branding` | `branding:read` | Configuración de administración, incluida metainformación de publicación |
| `PUT /api/v1/admin/branding` | `branding:write` | Reemplaza configuración validada con control optimista de revisión |
| `POST /api/v1/admin/branding/rollback` | `branding:write` | Copia un snapshot anterior en una revisión nueva usando `expectedRevision` |
| `POST /api/v1/admin/branding/assets` | `branding:write` | Sube imagen permitida, devuelve id y metadatos seguros |
| `GET /assets/{assetId}` | Público según publicación | Sirve activo publicado con MIME/headers seguros y caché versionada |

En la implementación inicial, `BRAND_ADMIN` e `INSTITUTIONAL_ADMIN` fueron valores técnicos provisionales de pruebas, nunca un contrato UPTC. La conversión incorporada se eliminó: hoy cualquier valor, incluidos esos ejemplos históricos, concede acceso solo si figura explícitamente en `UPTC_OIDC_ROLE_PERMISSION_MAPPING`, que permanece vacío hasta aprobación institucional.

Errores estándar: 400 por entrada inválida, 401 sin autenticación, 403 sin permiso, 404 por activo inexistente y 409 por revisión concurrente. Ningún error devuelve rutas de almacenamiento o stack traces. El endpoint público utiliza ETag derivado de la revisión y caché corta; al publicar una revisión nueva, la respuesta cambia.

## No incluido en esta fase

No activar integración con un IdP supuesto, no copiar bases productivas, no reemplazar SIRA/otros en este primer incremento, no habilitar módulos académicos todavía no implementados y no prometer carga productiva sin ensayos, reconciliación y aceptación institucional.
