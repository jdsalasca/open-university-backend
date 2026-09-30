# Plataforma institucional UPTC

Repositorio de trabajo para la sustitución progresiva de los sistemas institucionales, con dos monolitos desplegables de forma separada:

- `frontend/`: Vite + React + TypeScript.
- `backend/`: Java 25 + Spring Boot + MySQL.

El primer incremento construye el Centro de Identidad Visual administrable y los cimientos para continuar con identidad estudiantil, programas, mallas, currículos, asignaturas y carga académica. El inventario DTIC de 2024 en `docs/PROJECT.md` es antecedente, no especificación vigente.

## Java del proyecto

La versión se fija en `.sdkmanrc` como `25.0.4-tem`. En Git Bash:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env install
sdk current java
```

En PowerShell usa el selector del proceso antes de ejecutar Maven:

```powershell
. .\tools\use-sdkman-java.ps1
java -version
```

Este helper selecciona el enlace `current` de SDKMAN para esa sesión; no cambia el PATH global de Windows.

## Desarrollo local

```powershell
cd frontend
npm ci
npm run dev
```

```powershell
. .\tools\use-sdkman-java.ps1
.\backend\mvnw.cmd -f backend\pom.xml test
```

La API lee `.env` en la raíz (copia `.env.example`) o las variables de entorno del proceso y usa MySQL. Completa las variables con una base de desarrollo; nunca uses datos productivos en el equipo local. El proveedor de identidad UPTC y sus claims requieren confirmación institucional antes de cualquier despliegue.

## Documentación de proyecto

- `AGENTS.md`: reglas de contexto e ingeniería.
- `docs/PROJECT.md`: alcance y decisiones.
- `docs/ROADMAP.md`: cronograma por etapas.
- `docs/architecture/`: diagramas C4, procesos y modelo de datos.
- `docs/specs/`: especificaciones funcionales aprobadas.
- `docs/superpowers/plans/`: planes TDD ejecutables.
