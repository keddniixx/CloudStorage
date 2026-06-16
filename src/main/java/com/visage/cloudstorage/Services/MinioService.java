package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.Model.FileResource;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.StatObjectArgs;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;
    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void createBucketForUser() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
    }

    public void upload(InputStream input, String completePath, MultipartFile file) throws Exception {
        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(completePath + file.getOriginalFilename()).stream(input, file.getSize(), -1).contentType(file.getContentType()).build());
    }

    public InputStream getObject(String path) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(path).build());
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
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(path).build());
    }

    public void createDirectory(String path) throws Exception {
        String directoryPath = path.endsWith("/") ? path : path + "/";
        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(directoryPath).stream(new ByteArrayInputStream(new byte[0]), 0, -1).build());
    }

    public void removeFolderBatch(String folderPrefix) throws Exception {
        String prefix = folderPrefix.endsWith("/") ? folderPrefix : folderPrefix + "/";
        List<DeleteObject> objectsToDelete = new ArrayList<>();
        for (var result : minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(true).build())) objectsToDelete.add(new DeleteObject(result.get().objectName()));
        if (!objectsToDelete.isEmpty()) for (var result : minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(bucketName).objects(objectsToDelete).build())) result.get();
    }

    public Iterable<io.minio.Result<Item>> getObjects(String prefix, boolean recursive) {
        return minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(recursive).build());
    }

    public void copy(String source, String destination) throws Exception {
        minioClient.copyObject(CopyObjectArgs.builder().bucket(bucketName).object(destination).source(CopySource.builder().bucket(bucketName).object(source).build()).build());
    }
}
