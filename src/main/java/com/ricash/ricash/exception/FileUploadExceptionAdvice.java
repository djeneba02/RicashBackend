package com.ricash.ricash.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

@ControllerAdvice
public class FileUploadExceptionAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException() {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Fichier trop volumineux", "message", "La taille maximale est de 10MB")
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipartException() {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Erreur de fichier", "message", "Problème avec le téléchargement des fichiers")
        );
    }
}
