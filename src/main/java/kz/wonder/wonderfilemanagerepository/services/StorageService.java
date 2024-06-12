package kz.wonder.wonderfilemanagerepository.services;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Service
@Slf4j
public class StorageService {


    private final Storage storage;
    private final String bucketName;

    public StorageService(final Storage storage,
                          @Value("${application.config.bucket-name}") final String bucketName) {

        this.storage = storage;
        this.bucketName = bucketName;
    }


    public String uploadFile(InputStream inputStream, String filename, String directory) {
        try {
            byte[] fileBytes = inputStream.readAllBytes();
            long fileSize = fileBytes.length;
            String contentType = Files.probeContentType(new File(filename).toPath());
            String storagePath = String.format("%s/%s", directory, filename);


            Map<String, String> metadata = new HashMap<>();
            metadata.put("filename", filename);
            metadata.put("content-type", contentType);
            metadata.put("content-length", String.valueOf(fileSize));

            BlobInfo blobInfo = BlobInfo
                    .newBuilder(bucketName, storagePath)
                    .setContentType(contentType)
                    .setMetadata(metadata)
                    .build();

            final Blob blob = storage.create(blobInfo, fileBytes);

            if (blob != null && !blob.getContentType().isBlank()) {
                log.info("File [{}] uploaded successfully.", filename);
                return filename;
            }

        } catch (IOException ioe) {
            log.error("Error Occurred: [{}]", ioe.getMessage(), ioe);
        }

        return filename.concat(" failed to upload");
    }


    public String deleteFile(String filename, String directory) {
        try {
            String storagePath = format("%s/%s", directory, filename);

            var blobId = BlobInfo
                    .newBuilder(bucketName, storagePath)
                    .build().getBlobId();

            boolean deleted = storage.delete(blobId);
            log.info("File [{}] deleted [{}]", filename, deleted ? "successfully" : "failed");

            return format("File [%s] deleted [%s]", filename, deleted ? "successfully" : "failed");

        } catch (Exception ex) {
            log.error("Error Occurred: [{}]", ex.getMessage(), ex);
        }

        return "Unable to delete file: " + filename;
    }

    public String deleteFolder(String folderName) {
        try {
            storage.list(bucketName, Storage.BlobListOption.prefix(folderName))
                    .iterateAll()
                    .forEach(blob -> {
                        // Delete each object in the folder
                        storage.delete(BlobId.of(bucketName, blob.getName()));
                    });

            // Delete the folder (by deleting an object that represents the folder)
            storage.delete(BlobId.of(bucketName, folderName));

            log.info("Folder [{}] deleted successfully", folderName);

            return format("Folder [%s] deleted successfully", folderName);

        } catch (Exception ex) {
            log.error("Error Occurred: [{}]", ex.getMessage(), ex);
        }

        return "Unable to delete folder: " + folderName;
    }

    public Blob getFile(String filename, String directory) {
        try {
            String storagePath = format("%s/%s", directory, filename);

            var blobId = BlobInfo
                    .newBuilder(bucketName, storagePath)
                    .build().getBlobId();

            var blob = storage.get(blobId);
            log.info("File [{}] fetched.", filename);

            return blob;

        } catch (Exception ex) {
            log.error("Error Occurred: [{}]", ex.getMessage(), ex);
        }

        return null;
    }
}
