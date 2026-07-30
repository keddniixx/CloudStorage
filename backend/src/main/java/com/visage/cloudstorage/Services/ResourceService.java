package com.visage.cloudstorage.Services;

import com.visage.cloudstorage.Exceptions.DataNotFoundException;
import com.visage.cloudstorage.Exceptions.NotCorrectNameFileOrPackage;
import com.visage.cloudstorage.Exceptions.ParentFileIsAlreadyExists;
import com.visage.cloudstorage.Model.FileResource;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Events.FileUploadEvent;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Transactional
@Service
public class ResourceService {

    private final MinioService minioService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FileResource resource(Integer userId, String path)  {
        String acceptPath = "the" + userId + "/" + path;
        try {
            return minioService.metadataObject(acceptPath, userId);
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
    }

    public List<FileResource> resources(Integer userId, String path) {
        String base = "the" + userId + "/" + path;
        String prefix;
        if (base.endsWith("/")) {
            prefix = base;
        } else {
            prefix = base + "/";
        }

        Iterable<Result<Item>> objects = minioService.getObjects(prefix, false);
        List<FileResource> fileResourceList = new ArrayList<>();
        for (Result<Item> object : objects) {
            FileResource fileResource;
            try {
                Item item = null;
                try {
                    item = object.get();
                } catch (RuntimeException e) {
                    throw new DataNotFoundException("Ресурс не найден");
                }
                String relative = item.objectName().substring(prefix.length());
                if (relative.isEmpty()) {
                    continue;
                }
                int idx = relative.indexOf("/");
                String name;
                if (idx == -1) {
                    name = relative;
                } else {
                    name = relative.substring(0, idx + 1);
                }
                if (item.size() == 0) {
                    fileResource = FileResource
                            .builder()
                            .path(prefix)
                            .name(name)
                            .type("DIRECTORY")
                            .build();
                } else {
                    fileResource = FileResource
                            .builder()
                            .path(prefix)
                            .name(name)
                            .size(item.size())
                            .type("FILE")
                            .build();
                }
                fileResourceList.add(fileResource);
            } catch (Exception e) {
                try {
                    throw new Exception();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return fileResourceList;
    }

    public List<FileResource> createPackage(String name, Integer userId) {
        String path = "the" + userId + "/" + name;
        FileResource fileResource = null;
        try {
            fileResource = minioService.metadataObject(path, userId);
        } catch (Exception e) {
        }
        if (fileResource != null) {
            throw new NotCorrectNameFileOrPackage("Такой файл уже существует");
        }
        String substring = name.substring(0, name.length() - 1);
        StringBuilder stringBuilder = new StringBuilder();
        String[] split = substring.split("");
        for (int i = substring.length() - 1; i >= 0; i--) {
            if (split[i].equals("/")) {
                break;
            }
            stringBuilder.insert(0, split[i]);
        }
        stringBuilder.append("/");
        String parentPath = path.substring(0, path.length() - stringBuilder.length());
        try {
            fileResource = minioService.metadataObject(parentPath, userId);
        } catch (Exception e) {
        }
        if (fileResource == null) {
            throw new ParentFileIsAlreadyExists("Родительской папки не существует");
        }
        List<FileResource> list = new ArrayList<>();
        try {
            minioService.createDirectory(path);
        } catch (Exception e) {
            throw new DataNotFoundException("Ресурс не найден");
        }
        FileResource directory = FileResource.builder()
                .type("DIRECTORY")
                .name(name)
                .path("the" + userId + "/")
                .build();
        list.add(directory);
        return list;
    }

    public FileResource createFile(List<MultipartFile> files, User user, String path) {
        String base = "the" + user.getId() + "/" + path;
        FileResource fileResourceNotExists = null;
        try {
            fileResourceNotExists = minioService.metadataObject(base + files.get(0).getOriginalFilename(), user.getId());
        } catch (Exception e) {
        }
        if (fileResourceNotExists != null) {
            if (fileResourceNotExists.getName() != null) {
                throw new NotCorrectNameFileOrPackage("Такое имя файла уже существует");
            }
        }
        boolean flag = true;
        FileResource fileResource = null;
        StringBuilder trueName = new StringBuilder();
        for (MultipartFile file : files) {
            fileResource = FileResource.builder()
                    .name(file.getOriginalFilename())
                    .path(base)
                    .size(file.getSize())
                    .type("FILE")
                    .build();
            if (files.size() > 1) { // баг если папка в папке будет то она заменится и ошибка не отработает
                String name = fileResource.getName();
                String[] split = name.split("");
                int count = 0;
                if (flag) {
                    for (int i = split.length - 1; i >= 0; i--) {
                        if (count == 1) {
                            if (split[i].equals("/")) {
                                flag = false;
                                break;
                            }
                            trueName.insert(0, split[i]);
                        }
                        if (split[i].equals("/")) {
                            count++;
                        }
                        flag = false;
                    }
                }

                try {
                    fileResourceNotExists = minioService.metadataObject(base + trueName + "/", user.getId());
                } catch (Exception e) {
                }
                if (fileResourceNotExists != null) {
                    if (fileResourceNotExists.getName() != null) {
                        throw new NotCorrectNameFileOrPackage("Такое имя файла уже существует");
                    }
                }
            }
            try {
                minioService.upload(file.getInputStream(), base, file);
                // Notification Logic
                if (file.getSize() > 500 * 1024) {
                    kafkaTemplate.send("file-upload-topic", new FileUploadEvent(user.getUsername(), file.getOriginalFilename(), file.getSize()));
                }
            } catch (Exception e) {
                throw new RuntimeException();
            }

        }
        if (files.size() == 1) {
            return fileResource;
        }
        try {
            createPackage(trueName + "/", user.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return FileResource.builder()
                .name(trueName + "/")
                .path(base)
                .type("DIRECTORY")
                .build();
    }

    public InputStreamResource downloadResource(String path, Integer userId) {
        int count = 0;
        String[] split = path.split("");
        for (int i = 0; i < path.length(); i++) {
            if (split[i].endsWith("/")) {
                break;
            }
            count++;
        }
        if ((Integer.parseInt(path.substring(3, count)) != userId)) {
            throw new BadCredentialsException("");
        }
        if (!path.endsWith("/")) {
            InputStream item = null;
            try {
                item = minioService.getObject(path);
            } catch (Exception e) {
                throw new DataNotFoundException("Ресурс не найден");
            }
            return new InputStreamResource(item);
        }
        String prefix = path;
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        Iterable<Result<Item>> objects = minioService.getObjects(prefix, true);
        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(zipBuffer)) {
            for (Result<Item> object : objects) {
                Item item = object.get();
                String relative = item.objectName();
                if (!relative.startsWith(prefix) || relative.endsWith("/")) {
                    continue;
                }
                String name = relative.substring(prefix.length());
                if (name.isBlank()) {
                    continue;
                }
                zipOutputStream.putNextEntry(new ZipEntry(name));
                try (InputStream input = minioService.getObject(relative)) {
                    input.transferTo(zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        ByteArrayInputStream zipContent = new ByteArrayInputStream(zipBuffer.toByteArray());
        return new InputStreamResource(zipContent);
    }

    public void createTheCentralPoint(Integer userId) {
        String path = "the" + userId + "/";
        try {
            minioService.createDirectory(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FileResource moveResoutce(String from, String to, Integer userId) {
        String userPrefix = "the" + userId + "/";
        if (from == null || to == null || !from.startsWith(userPrefix) || !to.startsWith(userPrefix) || from.equals(to)) {
            throw new DataNotFoundException("Путь ресурса неверен");
        }
        try {
            minioService.metadataObject(from, userId);
        } catch (Exception exception) {
            throw new DataNotFoundException("Ресурс не найден");
        }
        if (from.endsWith("/") && to.startsWith(from)) {
            throw new NotCorrectNameFileOrPackage("Нельзя переместить каталог внутрь себя");
        }
        String destination = from.endsWith("/") && !to.endsWith("/") ? to + "/" : to;
        if (from.endsWith("/") && !minioService.getObjects(from, true).iterator().hasNext()) {
            throw new DataNotFoundException("Ресурс не найден");
        }
        if (from.endsWith("/")) {
            List<String> sources = new ArrayList<>();
            for (Result<Item> result : minioService.getObjects(from, true)) {
                try {
                    sources.add(result.get().objectName());
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
            for (String source : sources) {
                try {
                    minioService.copy(source, destination + source.substring(from.length()));
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
            for (String source : sources) {
                try {
                    minioService.removeObject(source);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        } else {
            try {
                minioService.copy(from, destination);
                minioService.removeObject(from);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        try {
            FileResource moved = minioService.metadataObject(destination, userId);
            String resultPath = destination.endsWith("/") ? destination.substring(0, destination.length() - 1) : destination;
            int separator = resultPath.lastIndexOf(47);
            return FileResource.builder()
                    .path(resultPath.substring(0, separator + 1))
                    .name(resultPath.substring(separator + 1) + (from.endsWith("/") ? "/" : ""))
                    .size(moved.getSize())
                    .type(moved.getType())
                    .build();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
    public boolean deleteResource(String path, Integer id) {
        int count = 0;
        String[] split = path.split("");
        for (int i = 0; i < path.length(); i++) {
            if (split[i].endsWith("/")) {
                break;
            }
            count++;
        }
        if ((Integer.parseInt(path.substring(3, count)) != id)) {
            throw new BadCredentialsException("");
        }
        FileResource fileResource = null;
        try {
            fileResource = minioService.metadataObject(path, id);
        } catch (Exception e) {
        }
        if (fileResource == null) {
            throw new DataNotFoundException("Ресурс не найден");
        }
        if (path.endsWith("/")) {
            try {
                minioService.removeFolderBatch(path);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        } else {
            try {
                minioService.removeObject(path);
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
        return true;
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