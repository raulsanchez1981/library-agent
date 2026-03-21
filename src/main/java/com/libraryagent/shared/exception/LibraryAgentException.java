package com.libraryagent.shared.exception;

import org.springframework.http.HttpStatus;

public class LibraryAgentException extends RuntimeException {

    private final HttpStatus status;

    public LibraryAgentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public LibraryAgentException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // Constructores de conveniencia para los casos más comunes
    public static LibraryAgentException notFound(String message) {
        return new LibraryAgentException(message, HttpStatus.NOT_FOUND);
    }

    public static LibraryAgentException badRequest(String message) {
        return new LibraryAgentException(message, HttpStatus.BAD_REQUEST);
    }

    public static LibraryAgentException internal(String message, Throwable cause) {
        return new LibraryAgentException(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
