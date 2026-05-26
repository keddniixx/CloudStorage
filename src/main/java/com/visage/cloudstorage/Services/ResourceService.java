package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.Model.FileResource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final MinioService minioService;

    public FileResource createFile(MultipartFile file, String path) throws Exception {
        minioService.upload(file.getInputStream(), path, file);
        return FileResource.builder().path(path).name(file.getOriginalFilename()).size(file.getSize()).type(file.getContentType()).build();
    }

    public InputStreamResource download(String path) throws Exception {
        return new InputStreamResource(minioService.getObject(path));
    }

    public void delete(String path) throws Exception {
        minioService.removeObject(path);
    }
}
