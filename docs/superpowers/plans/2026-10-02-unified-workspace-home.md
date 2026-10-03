# Portada del portal por capacidades — Plan de implementación

> **Para agentes ejecutores:** seguir `superpowers:executing-plans` y `superpowers:test-driven-development` tarea por tarea. Implementación nativa en la rama `develop`, según autorización persistente del usuario; no iniciar subagentes.

**Objetivo:** convertir Resumen en la página inicial que expone rutas públicas y accesos administrativos autorizados. Los laboratorios sintéticos no tienen entradas en el producto.

**Arquitectura:** agregar un componente de portada dentro del monolito React y conectarlo al shell existente. La portada usa configuración institucional y permisos de `/api/v1/me`; no crea identidad, permisos ni servicios nuevos.

**Stack:** React, TypeScript, SCSS, Vitest, Testing Library.

**Especificación:** [diseño de portada](../specs/2026-10-02-unified-workspace-home-design.md).

## Restricciones globales

- Mantener `develop` como rama de integración y usar los repositorios frontend/backend existentes.
- No deducir roles o permisos desde `subject`, nombre, sesión local ni estado visual.
- Conservar las rutas actuales, en especial `#inicio` para el Centro de Identidad Visual.
- No agregar captura ni persistencia de datos personales, ni convertir demos en operación institucional.
- Mantener navegación mobile-first, accesible, clara/oscura y con SCSS compartido.
- Probar con AAA observando RED antes de modificar comportamiento de producción.

## Enfoque de revisión

- Sin fragmento inicial y con `#resumen`: la portada se monta y el enlace activo identifica la ruta.
- `#inicio`: conserva el Centro de Identidad Visual, sin colisión con la nueva portada.
- Sin permiso administrativo: no aparecen enlaces a consolas protegidas.
- Permisos parciales: aparece únicamente la consola cuya familia de permisos está presente.
- Banner antes de inicio, en el instante de inicio, en el instante de fin y fuera de vigencia.
- Sin fragmento de laboratorio: la ruta desconocida vuelve a la portada, sin montar contenido sintético.
- Vite production: los módulos demo desconectados no se incluyen en el bundle.

---

### Tarea 1: Definir el contrato observable de la portada

**Archivos:**
- Crear: `frontend/src/features/workspace/WorkspaceHomePage.test.tsx`
- Crear: `frontend/src/features/workspace/WorkspaceHomePage.tsx`

**Interfaces:**
- Consume marca pública (`PublicBranding`), permisos efectivos (`ApplicationPermission[]`) y, solo para rotular la revisión local, el flag de sesión `local-preview`.
- Expone portada semántica con enlaces por hash; ningún permiso se deriva del contenido de identidad.

- [x] Escribir pruebas AAA para rutas públicas, accesos por permisos, ausencia sin permisos, banners activos/no vigentes y ausencia de enlaces a laboratorios.
- [x] Ejecutar Vitest sobre esa prueba y confirmar RED por módulo/componente ausente.
- [x] Implementar la portada y selección de banner activo con inicio inclusivo y fin exclusivo.
- [x] Repetir la prueba dirigida y comprobar GREEN; verificar teclado, región/encabezados y etiquetas accesibles.

### Tarea 2: Integrar Resumen al shell

**Archivos:**
- Modificar: `frontend/src/App.tsx`
- Modificar: `frontend/src/App.test.tsx`
- Modificar: `frontend/src/App.scss`

**Interfaces:**
- `ApplicationView` agrega `home`; `readApplicationView()` resuelve `#resumen` y lo usa cuando no hay fragmento.
- `#inicio` continúa resolviendo identidad visual.
- `ApplicationShell` pasa `branding.branding`, permisos efectivos e `isLocalPreviewSession` a `WorkspaceHomePage` para identificar visualmente la sesión temporal local.

- [x] Añadir pruebas de navegación para la portada inicial, conservar `#inicio` y restaurar `#resumen` después del retorno OIDC.
- [x] Observar RED: `#inicio` se resolvía como la portada y `#resumen` no estaba permitido como hash de retorno OIDC.
- [x] Habilitar el enlace Resumen, enlazar el lockup institucional y enrutar la portada inicial; mantener `#inicio`.
- [x] Añadir estilos mobile-first y scroll horizontal accesible a la barra móvil.
- [x] Ejecutar pruebas `App.test.tsx` y `WorkspaceHomePage.test.tsx`.

### Tarea 3: Registrar límites y recorridos

**Archivos:**
- Modificar: `docs/architecture/c4.md`
- Modificar: `docs/architecture/process-flows.md`
- Modificar: `docs/ROADMAP.md`
- Modificar: `frontend/AGENTS.md`

- [x] Actualizar C4 para la portada, branding, `/api/v1/me` y enlaces de módulos.
- [x] Añadir el flujo de navegación y permisos sin otorgamiento en el frontend.
- [x] Registrar la portada como entrega y documentar que los laboratorios no tienen acceso desde el producto.
- [x] Actualizar la guía frontend con `#resumen`, rutas y conducta por permisos.

### Tarea 4: Validar e integrar

- [x] Ejecutar `npm test` (51 archivos Vitest, 402 pruebas; 16 pruebas de scripts), `npm run build` y `npm run lint`.
- [x] Verificar el servidor local y la ruta `/#resumen` sin detener Compose.
- [x] Revisar manifest de producción, tema oscuro/claro, focus, enlaces y overflow móvil.
- [x] Confirmar que no se modificaron contratos backend ni se añadieron dependencias.
- [ ] Commit y push frontend a `develop`; integrar documentación y puntero del submódulo en backend `develop` si corresponde.

## Evidencia de integración

- La suite completa, build, lint, inspección de manifest, revisión en navegador y commits se registran al cerrar esta entrega.
- Home tests confirm no synthetic lab links in navigation or product pages; the deployment preview notice remains visibly labeled as a local developer session when applicable.
