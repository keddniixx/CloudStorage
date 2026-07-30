package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.Model.FileResource;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MinioService {

    private final MinioClient minioClient;

    private String bucketName = "name";

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Profile("!test")
    @PostConstruct
    public void createBucketForUser() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    public InputStream getObject(String path) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(path)
                .build());
    }

    public Iterable<Result<Item>> getObjects(String path, boolean recursive) {
        return minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(path)
                .recursive(recursive)
                .build());
    }

    public FileResource metadataObject(String path, Integer userId) throws Exception {
        var stat = minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(path).build());
        String normalizedPath = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
        String name = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
        if (path.endsWith("/")) {
            name += "/";
        }
        return FileResource.builder()
                .path(path)
                .name(name)
                .size(stat.size())
                .type(stat.contentType())
                .build();
    }

    public void removeObject(String path) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(path)
                .build());
    }

    public void removeFolderBatch(String folderPrefix) throws Exception {
        if (!folderPrefix.endsWith("/")) {
            folderPrefix += "/";
        }

        List<DeleteObject> toDelete = new ArrayList<>();
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(folderPrefix)
                        .recursive(true)
                        .build()
        );
        for (Result<Item> result : objects) {
            toDelete.add(new DeleteObject(result.get().objectName()));
        }

        Iterable<Result<DeleteError>> errors = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(toDelete)
                        .build()
        );

        for (Result<DeleteError> err : errors) {
            System.err.println("Unable to delete object " +
                    err.get().objectName() + ": " + err.get().message());
        }
    }

    public void upload(InputStream input, String completePath, MultipartFile file) throws Exception {
        String objectName = completePath + file.getOriginalFilename();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(input, file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
    }

    public void copy(String oldCompletePath, String newCompletePath) throws Exception {
        minioClient.copyObject(CopyObjectArgs.builder()
                .bucket(bucketName)
                .object(newCompletePath)
                .source(CopySource.builder()
                        .bucket(bucketName)
                        .object(oldCompletePath)
                        .build())
                .build());
    }

    public void createDirectory(String path) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(path)
                .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                .build());
    }

}