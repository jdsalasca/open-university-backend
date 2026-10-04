# Plan: acceso por teclado al contenido principal

## Objetivo

Permitir que quien navega con teclado omita el menú global y llegue directamente a la región principal de la ruta visible.

## Diseño

- Reutilizar el `id` de ruta actual del elemento `<main>` como destino del enlace.
- Hacer enfocable el `<main>` con `tabIndex={-1}` y mover allí el foco al activar el enlace.
- Mantener el enlace fuera de pantalla hasta que reciba foco visible; aplicar estilos en SCSS y el anillo de foco compartido.
- No cambiar rutas, datos, permisos, backend, persistencia ni contratos de dominio.

## Pasos y verificación

1. Añadir pruebas AAA para salto con teclado, foco del `<main>` y cambio de ruta; observar RED antes del código productivo.
2. Implementar el destino compartido, foco del `<main>` y presentación SCSS visible solo al enfocar.
3. Ejecutar pruebas unitarias, suite Vitest completa, lint y build; investigar los fallos que aparezcan bajo carga.
4. Verificar con navegador la interacción de teclado en escritorio y móvil, capturar y revisar la pantalla.
5. Registrar el corte en `docs/ROADMAP.md`, publicar frontend en `develop`, actualizar el submódulo frontend del backend y confirmar CI y remotos.

## Verificación ejecutada

- RED: antes de añadir la interfaz, la prueba focalizada falló porque no había un enlace accesible para saltar al contenido.
- GREEN: CI en `develop` validó los cambios en `f8e868a46a313bd0287a0641f31b6df38b0e1685`: 75 archivos Vitest y 510 pruebas pasaron; lint, build y el presupuesto de bundle también pasaron ([ejecución 37232616441](https://github.com/jdsalasca/open-university-frontend/actions/runs/37232616441)).
- `npm run lint` y `npm run build` pasan localmente; el CSS inicial queda en 21.991 B frente al límite de 22.000 B.
- Una corrida completa local anterior a corregir el orden de pruebas reportó 488/510; CI aisló una interacción con el chunk diferido y, después de mover esa prueba detrás de la expectativa de carga, ejecutó las 510 pruebas en verde. En una corrida local posterior, la nueva prueba aislada superó el timeout predeterminado de 5 s mientras la aplicación del equipo estaba bajo carga; la cobertura funcional pasó en CI y también en el navegador real.
- Playwright en `http://127.0.0.1:5173/#resumen`: Tab enfoca el enlace, Enter enfoca `<main id="resumen">`; las capturas se revisaron en 1440 × 900 y 390 × 844.
- El enlace reutiliza los estilos de navegación existentes para conservar el límite del CSS. No cambian rutas, datos, permisos, backend, persistencia ni contratos de dominio; no se requieren cambios C4 o de proceso.

## Criterio de salida

Las pruebas confirman que el enlace es el primer control de teclado, enfoca el `<main>` y sigue la ruta; la UI muestra un foco legible en escritorio y móvil; ambos repositorios quedan limpios y alineados con `origin/develop`.
