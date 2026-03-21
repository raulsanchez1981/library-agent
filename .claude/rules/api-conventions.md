# Reglas: Convenciones de API REST

## Endpoints

- Rutas en inglés, en minúsculas, con guiones: `/api/v1/book-mentions`, `/api/v1/user-profile`
- Sustantivos en plural para colecciones: `/books`, `/recommendations`
- Versión en la ruta: `/api/v1/`

## Controladores

```java
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<BookDto> create(@Valid @RequestBody CreateBookRequest request) {
        BookDto created = service.create(request);
        URI location = URI.create("/api/v1/books/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}
```

## Respuestas consistentes

- Usar siempre `ResponseEntity<T>` — nunca devolver el objeto directamente
- Códigos HTTP semánticos: `200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`, `404 Not Found`
- Errores con estructura uniforme:
  ```json
  {
    "status": 400,
    "error": "Validation failed",
    "message": "El campo 'title' es obligatorio",
    "timestamp": "2026-03-21T10:00:00Z"
  }
  ```

## Validación

- `@Valid` en todos los `@RequestBody`
- Usar Bean Validation (Jakarta) en los records de request:
  ```java
  public record CreateBookRequest(
      @NotBlank String title,
      @NotBlank String author
  ) {}
  ```
- Errores de validación capturados globalmente por `@RestControllerAdvice`

## DTOs

- Records inmutables para request y response
- Nunca exponer entidades JPA directamente en la API
