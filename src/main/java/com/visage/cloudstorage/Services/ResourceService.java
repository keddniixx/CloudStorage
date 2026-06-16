package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.Exceptions.DataNotFoundException;
import com.visage.cloudstorage.Exceptions.NotCorrectNameFileOrPackage;
import com.visage.cloudstorage.Exceptions.ParentFileIsAlreadyExists;
import com.visage.cloudstorage.Model.FileResource;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final MinioService minioService;

    public FileResource createFile(MultipartFile file, String path) throws Exception {
        minioService.upload(file.getInputStream(), path, file);
        return FileResource.builder().path(path).name(file.getOriginalFilename()).size(file.getSize()).type(file.getContentType()).build();
    }

    public InputStreamResource download(String path) throws Exception {
        return downloadResource(path, 0);
    }

    public InputStreamResource downloadResource(String path, Integer userId) throws Exception {
        if (!path.endsWith("/")) return new InputStreamResource(minioService.getObject(path));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (Result<Item> result : minioService.getObjects(path, true)) {
                Item item = result.get();
                String objectPath = item.objectName();
                if (objectPath.equals(path) || objectPath.endsWith("/")) continue;
                String entryName = objectPath.substring(path.length());
                if (entryName.isBlank()) continue;
                zip.putNextEntry(new ZipEntry(entryName));
                try (InputStream input = minioService.getObject(objectPath)) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        return new InputStreamResource(new ByteArrayInputStream(buffer.toByteArray()));
    }

    public void delete(String path) throws Exception {
        if (path.endsWith("/")) minioService.removeFolderBatch(path); else minioService.removeObject(path);
    }

    public FileResource createPackage(String name, Integer userId) throws Exception {
        if (name == null || name.isBlank() || !name.endsWith("/")) throw new NotCorrectNameFileOrPackage("Имя папки должно заканчиваться символом /");
        int separator = name.substring(0, name.length() - 1).lastIndexOf('/');
        if (separator >= 0) {
            try { minioService.metadataObject(name.substring(0, separator + 1), userId); }
            catch (Exception exception) { throw new ParentFileIsAlreadyExists("Родительская папка не найдена"); }
        }
        try { minioService.metadataObject(name, userId); throw new NotCorrectNameFileOrPackage("Папка уже существует"); }
        catch (NotCorrectNameFileOrPackage exception) { throw exception; }
        catch (Exception exception) { minioService.createDirectory(name); }
        return FileResource.builder().path(name).name(name.substring(name.lastIndexOf('/', name.length() - 2) + 1)).type("DIRECTORY").size(0L).build();
    }

    public void createTheCentralPoint(Integer userId) throws Exception {
        minioService.createDirectory("the" + userId + "/");
    }

    public FileResource moveResoutce(String from, String to, Integer userId) throws Exception {
        boolean directory = from.endsWith("/");
        String destination = directory && !to.endsWith("/") ? to + "/" : to;
        if (directory) {
            List<String> sources = new ArrayList<>();
            for (Result<Item> result : minioService.getObjects(from, true)) {
                String source = result.get().objectName();
                sources.add(source);
            }
            for (String source : sources) {
                minioService.copy(source, destination + source.substring(from.length()));
            }
            for (String source : sources) {
                minioService.removeObject(source);
            }
        } else {
            minioService.copy(from, destination);
            minioService.removeObject(from);
        }
        FileResource moved = minioService.metadataObject(destination, userId);
        String resultPath = destination.endsWith("/") ? destination.substring(0, destination.length() - 1) : destination;
        int separator = resultPath.lastIndexOf(47);
        return FileResource.builder()
                .path(resultPath.substring(0, separator + 1))
                .name(resultPath.substring(separator + 1) + (directory ? "/" : ""))
                .size(moved.getSize())
                .type(moved.getType())
                .build();
    }
    public List<FileResource> searchResource(String query, Integer userId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalizedQuery = query.toLowerCase(java.util.Locale.ROOT);
        String base = "the" + userId + "/";
        List<FileResource> matches = new ArrayList<>();
        for (Result<Item> result : minioService.getObjects(base, true)) {
            try {
                Item item = result.get();
                String objectPath = item.objectName();
                if (objectPath.endsWith("/")) {
                    continue;
                }
                int separator = objectPath.lastIndexOf(47);
                String name = objectPath.substring(separator + 1);
                if (name.toLowerCase(java.util.Locale.ROOT).contains(normalizedQuery)) {
                    matches.add(FileResource.builder()
                            .path(objectPath.substring(0, separator + 1))
                            .name(name)
                            .size(item.size())
                            .type("FILE")
                            .build());
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        return matches;
    }
}
