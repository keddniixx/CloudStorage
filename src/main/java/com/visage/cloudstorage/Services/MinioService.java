package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.Model.FileResource;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;
    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void createBucketForUser() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
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
}
