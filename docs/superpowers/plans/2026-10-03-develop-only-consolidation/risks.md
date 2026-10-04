# Riesgos de integración

- **Contenido cambiante:** las páginas UPTC pueden modificar texto, servicios o fechas. Conservar URL oficial y fecha de actualización visible; no deducir disponibilidad.
- **Reglas institucionales:** no convertir un enlace público en elegibilidad, cupo, beneficio vigente, selección, matrícula o dato personal.
- **Fuentes locales obsoletas:** el checkout fuente está muy detrás de `develop`; aplicar cambios puntuales solo después de comparar su comportamiento con el código remoto actual.
- **Artefactos efímeros:** Harness local, capturas de Playwright, resultados de ejecución y trazas no son archivos de producto y no se suben.
- **Integración entre repositorios:** publicar frontend primero y actualizar el gitlink backend después; verificar el SHA remoto antes del segundo push.
- **Interrupción o rechazo remoto:** no usar force push ni crear una rama como atajo. Releer el remoto, reconciliar el avance y conservar los checkouts locales.
