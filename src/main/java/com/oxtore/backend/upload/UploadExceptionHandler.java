package com.oxtore.backend.upload;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global on purpose, but limited to upload-specific exception types:
 * an oversized multipart body is rejected by the DispatcherServlet before any controller is chosen, so the
 * package-scoped advices never see it; and product controllers in several packages validate image references.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> tooLarge(MaxUploadSizeExceededException ex) {
        return error("Image too large (max 5 MB)", HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<Object> invalidImage(InvalidImageException ex) {
        return error(ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(InvalidImageReferenceException.class)
    public ResponseEntity<Object> invalidReference(InvalidImageReferenceException ex) {
        return error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<Object> badMultipart(Exception ex) {
        return error("Expected a multipart/form-data request with a \"file\" field", HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> error(String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
