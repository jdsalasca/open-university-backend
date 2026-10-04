# Riesgos de integración

- **Implementaciones locales desfasadas:** los checkouts antiguos están entre 51 y 80 commits detrás de sus `origin/develop` actuales. Sus copias pueden retirar mejoras recientes o romper contratos; comparar por archivo antes de portar.
- **Artefactos efímeros:** `.harness-moon`, `.playwright-mcp`, capturas, registros, resultados de ejecución y trazas locales no forman parte del producto y no se suben.
- **Datos institucionales no validados:** un enlace público no prueba elegibilidad, disponibilidad, reglas, cupos, autorización para automatizar ni designación de un área propietaria. Mantener el directorio como informativo.
- **Información personal:** no incorporar identidad, horarios, notas, expedientes ni datos de aspirantes o estudiantes reales en código, pruebas, capturas o logs.
- **Contenido que cambia:** conservar la fuente institucional y describir solo lo que respalda; la fecha de consulta no equivale a la actualización de la página.
- **Integración coordinada:** publicar frontend primero, actualizar el gitlink después y confirmar ambos SHAs remotos y las CI asociadas.
- **Trabajo local preservado:** existen archivos modificados y no seguidos en los checkouts antiguos. No hacer reset, limpieza masiva ni commit global mientras no se concilie cada cambio.
- **Preview local:** Compose y Compose Watch solo respaldan la revisión de desarrollo. No representan despliegue, seguridad de producción, rendimiento institucional ni aceptación UPTC.
