# Skill: Entrega de libros al Kindle

## Flujo completo

```
RecommendationResult
      ↓
EpubDownloader        → descarga el epub desde fuente (Project Gutenberg, Open Library, etc.)
      ↓
FormatConverter       → convierte a MOBI/AZW3 si es necesario (via Calibre CLI)
      ↓
KindleEmailSender     → envía el fichero adjunto a {usuario}@kindle.com
      ↓
DeliveryRecord        → persiste el resultado en BD
```

## EpubDownloader

```java
public interface EpubDownloader {
    Path download(Book book) throws BookDownloadException;
}
```

- Descarga a directorio temporal configurado en `kindle.delivery.temp-dir`
- Limpiar ficheros temporales tras el envío (éxito o fallo)

## FormatConverter

Usa Calibre (`ebook-convert`) via `ProcessBuilder`

```java
public interface FormatConverter {
    Path convert(Path source, BookFormat targetFormat) throws ConversionException;
}
```

`BookFormat` es una sealed interface:
```java
public sealed interface BookFormat permits BookFormat.Epub, BookFormat.Mobi, BookFormat.Azw3 {}
```

Kindle acepta directamente EPUB desde 2022; convertir solo si el fichero es PDF o formato no soportado.

## KindleEmailSender

```java
public interface KindleEmailSender {
    void send(Path bookFile, String kindleAddress) throws DeliveryException;
}
```

- Usa Spring Mail (`JavaMailSender`)
- Dirección de origen configurada en `kindle.delivery.sender-email` (debe estar aprobada en Amazon)
- Asunto del email: nombre del fichero sin extensión
- Tamaño máximo por envío: 50 MB

## Configuración en application.yml

```yaml
kindle:
  delivery:
    sender-email: ${KINDLE_SENDER_EMAIL}
    temp-dir: /tmp/library-agent/kindle
    max-file-size-mb: 50
```

## Convenciones

- `DeliveryRecord` persiste: libro, dirección kindle, timestamp, estado (`SUCCESS`/`FAILED`), mensaje de error si aplica
- Reintentos: máximo 3, con backoff exponencial, gestionados por Spring Retry
- Nunca loguear la dirección de kindle completa (dato personal): enmascarar como `***@kindle.com`
