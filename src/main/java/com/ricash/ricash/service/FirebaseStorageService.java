package com.ricash.ricash.service;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    private final Storage storage;
    private final String bucketName;

    public FirebaseStorageService() throws IOException {
        // Utilisez le nom de bucket correct
        this.bucketName = "ricash-83a56.firebasestorage.app"; // ou "ricash-83a56.appspot.com"

        StorageOptions options = StorageOptions.newBuilder()
                .setProjectId("ricash-83a56")
                .build();

        this.storage = options.getService();
    }

    public String uploadFile(MultipartFile file, String destinationPath) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }

        // Validation du type
        if (!isValidImageType(file)) {
            throw new IllegalArgumentException("Type d'image non supporté");
        }

        // Nom de fichier unique
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + fileExtension;
        String fullPath = destinationPath + "/" + fileName;

        // Upload
        BlobId blobId = BlobId.of(bucketName, fullPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            Blob blob = storage.create(blobInfo, inputStream);
            return blob.getMediaLink(); // Meilleure méthode pour l'URL
        }
    }

    private boolean isValidImageType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null &&
                (contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/jpg"));
    }

    private String getFileExtension(String filename) {
        return filename != null && filename.contains(".") ?
                filename.substring(filename.lastIndexOf(".")) : ".jpg";
    }
}