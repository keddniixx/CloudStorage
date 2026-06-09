package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.Exceptions.DataNotFoundException;
import com.visage.cloudstorage.Exceptions.NotCorrectNameFileOrPackage;
import com.visage.cloudstorage.Exceptions.ParentFileIsAlreadyExists;
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
        if (path.endsWith("/")) {
            minioService.removeFolderBatch(path);
        } else {
            minioService.removeObject(path);
        }
    }

    public FileResource createPackage(String name, Integer userId) throws Exception {
        if (name == null || name.isBlank() || !name.endsWith("/")) {
            throw new NotCorrectNameFileOrPackage("Имя папки должно заканчиваться символом /");
        }
        int separator = name.substring(0, name.length() - 1).lastIndexOf('/');
        if (separator >= 0) {
            String parent = name.substring(0, separator + 1);
            try {
                minioService.metadataObject(parent, userId);
            } catch (Exception exception) {
                throw new ParentFileIsAlreadyExists("Родительская папка не найдена");
            }
        }
        try {
            minioService.metadataObject(name, userId);
            throw new NotCorrectNameFileOrPackage("Папка уже существует");
        } catch (NotCorrectNameFileOrPackage exception) {
            throw exception;
        } catch (Exception ignored) {
            minioService.createDirectory(name);
        }
        return FileResource.builder().path(name).name(name.substring(name.lastIndexOf('/', name.length() - 2) + 1)).type("DIRECTORY").size(0L).build();
    }

    public void createTheCentralPoint(Integer userId) throws Exception {
        minioService.createDirectory("the" + userId + "/");
    }
}
