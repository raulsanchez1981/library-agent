# Reglas: Eficiencia de agentes y consumo de cuota

## Cuándo NO invocar test-runner

El agente `test-runner` ejecuta `mvn test` completo (~136 tests, ~20s compilación). Es costoso.
Invocarlo solo cuando:
- Se modifica lógica de negocio real (servicios, repositorios, controladores con lógica nueva)
- Se añade o modifica un test que puede fallar por razones no obvias

**NO invocar test-runner** cuando el cambio es trivialmente correcto:
- Añadir `@MockitoBean` a un test (si la causa del fallo ya estaba identificada)
- Añadir un import
- Cambiar un mensaje de error en un string literal
- Renombrar una variable sin cambiar lógica

En esos casos, usar `Grep` para verificar consistencia y confiar en el análisis estático.

## Antes de escribir cualquier scraper HTML

**OBLIGATORIO**: Hacer `WebFetch` sobre la URL objetivo ANTES de escribir el código.
Verificar en el HTML devuelto:
1. Que los selectores CSS existen en el HTML estático (no sólo en JS client-side)
2. Que los bloques JSON-LD están presentes y contienen los campos esperados
3. Anotar los selectores exactos que funcionan

Si el HTML devuelto por WebFetch no contiene los selectores → el framework usa rendering
client-side (React, Svelte, Vue). En ese caso usar JSON-LD o meta tags, nunca CSS selectors.

## Uso de agentes especializados

- `ingestion-agent`, `recommendation-agent`: solo cuando se crea o modifica lógica de negocio del paquete. No para fixes triviales de imports o mocks en tests.
- `Explore`: solo cuando búsqueda simple con Grep/Glob no es suficiente tras 2-3 intentos.
- **No encadenar agentes** para tareas que se pueden resolver con Read + Edit directos.

## Regla de parada dura (CRÍTICO)

Si tras **3 tool calls** consecutivos no tienes un path claro a la solución:
- **PARA. No sigas investigando.**
- Resume lo que sabes y lo que no, y pregunta al usuario.

Investigar más allá de 3 ficheros sin resultado concreto es siempre un error.
El contexto consumido en investigación inútil degrada el razonamiento del resto de la sesión.

## Errores secundarios: ignorar o preguntar, nunca investigar

Si durante un fix aparece un error o comportamiento inesperado **no relacionado directamente**
con el cambio pedido:
- No lo investigues.
- Menciona que existe y pregunta si el usuario quiere que lo abordes.

Ejemplo de lo que NO hacer: el usuario pide "añade refresco de sesión" → aparece un error
CSRF en los logs → no leer 20 ficheros de node_modules para entender el error CSRF.

## Subdirectorios con su propio CLAUDE.md o AGENTS.md

**Leer siempre** el AGENTS.md o CLAUDE.md del subdirectorio antes de tocar cualquier fichero
dentro de él. Las convenciones pueden diferir radicalmente del proyecto principal.

## Regla general

Si la tarea cabe en: leer 1-2 ficheros → editar → verificar con Grep → hacerlo sin agentes.
Los agentes añaden valor en tareas complejas multi-fichero o que requieren razonamiento
amplio. No para fixes de 3 líneas.
