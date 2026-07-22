package com.visage.cloudstorage.unit.Services;

import com.visage.cloudstorage.Model.FileResource;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Services.MinioService;
import com.visage.cloudstorage.Services.ResourceService;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class ResourceServiceTest {

    @Mock
    private MinioService minioService;

    @InjectMocks
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
    }

    private Result<Item> createMockResult(String objectName, Long size) throws Exception {
        Item mockItem = mock(Item.class);
        when(mockItem.objectName()).thenReturn(objectName);
        when(mockItem.size()).thenReturn(size);

        Result<Item> mockResult = mock(Result.class);
        when(mockResult.get()).thenReturn(mockItem);
        return mockResult;
    }

    @Test
    void shouldBeResourcesWhenUserIdExistsAndPathNotExists() throws Exception {
        String path = "";
        Integer userId = 1;
        String base = "the" + userId + "/" + path;

        Iterable<Result<Item>> objects = Arrays.asList(createMockResult("the1/papka/", 0L), createMockResult("the1/file.txt", 100L));
        when(minioService.getObjects(eq(base), eq(false))).thenReturn(objects);

        List<FileResource> listValue = resourceService.resources(userId, path);

        assertNotNull(listValue, "Не нулл");
        assertEquals(2, listValue.size());

        assertNotNull(listValue.get(0), "Папка не нулл");
        assertEquals("the1/", listValue.get(0).getPath(), " Проверка пути");
        assertEquals("papka/", listValue.get(0).getName(), " Проверка имени");
        assertEquals("DIRECTORY", listValue.get(0).getType(), " Проверка типа");
        assertEquals(null, listValue.get(0).getSize(), "Проверка размера");

        assertNotNull(listValue.get(1), "Файл не нулл");
        assertEquals("the1/", listValue.get(1).getPath(), " Проверка пути");
        assertEquals("file.txt", listValue.get(1).getName(), " Проверка имени");
        assertEquals("FILE", listValue.get(1).getType(), " Проверка типа");
        assertEquals(100L, listValue.get(1).getSize(), "Проверка размера");
    }

    @Test
    void shouldBeResourcesWhenUserIdExistsAndPathExists() throws Exception {
        String path = "papka/";
        Integer userId = 1;
        String base = "the" + userId + "/" + path;

        Iterable<Result<Item>> objects = Arrays.asList(createMockResult("the1/papka/papka/", 0L), createMockResult("the1/papka/file.txt", 100L));
        when(minioService.getObjects(eq(base), eq(false))).thenReturn(objects);

        List<FileResource> listValue = resourceService.resources(userId, path);

        assertNotNull(listValue, "Не нулл");
        assertEquals(2, listValue.size());

        assertNotNull(listValue.get(0), "Папка не нулл");
        assertEquals("the1/papka/", listValue.get(0).getPath(), " Проверка пути");
        assertEquals("papka/", listValue.get(0).getName(), " Проверка имени");
        assertEquals("DIRECTORY", listValue.get(0).getType(), " Проверка типа");
        assertEquals(null, listValue.get(0).getSize(), "Проверка размера");

        assertNotNull(listValue.get(1), "Файл не нулл");
        assertEquals("the1/papka/", listValue.get(1).getPath(), " Проверка пути");
        assertEquals("file.txt", listValue.get(1).getName(), " Проверка имени");
        assertEquals("FILE", listValue.get(1).getType(), " Проверка типа");
        assertEquals(100L, listValue.get(1).getSize(), "Проверка размера");
    }

    @Test
    void shouldBeCreatePackage() throws Exception {
        int userId = 1;
        String name = "somename/";
        when(minioService.metadataObject("the1/", userId)).thenReturn(FileResource.builder().name("the1/").build());
        List<FileResource> list = resourceService.createPackage(name, userId);
        assertNotNull(list, "Не нулл");
        assertNotNull(list.get(0), "Не нулл");
        assertEquals("DIRECTORY", list.get(0).getType(), "Проверка типа");
        assertEquals(name, list.get(0).getName(), "Проверка имени");
        assertEquals("the1/", list.get(0).getPath(), "Проверка пути");
    }

    @Test
    void shouldBeCreateFile() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        List<MultipartFile> files = new ArrayList<>();
        files.add(multipartFile);
        User user = User.builder().id(1).username("testuser").build();
        String path = "file.txt";

        FileResource file = resourceService.createFile(files, user, path);

        assertNotNull(file, "Не нулл");
        assertEquals("FILE", file.getType(), "Проверка типа");
    }

    private InputStream createInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldBeDownloadResourceWithOneFile() throws Exception {
        int userId = 1;
        String path = "the1/file.txt";
        String content = "Some content";
        InputStream inputStream = createInputStream(content);
        when(minioService.getObject(path)).thenReturn(inputStream);

        InputStreamResource inputStreamResource = resourceService.downloadResource(path, userId);

        assertNotNull(inputStreamResource, "Не нулл");

        String resultContent = new String(inputStreamResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(content, resultContent, "Проверка контента");
        verify(minioService, times(1)).getObject(eq(path));
        verify(minioService, never()).getObjects(anyString(), anyBoolean());
    }

    private Result<Item> createMockResultForDownload(String objectName, Long size) throws Exception {
        Item mockItem = mock(Item.class);
        when(mockItem.objectName()).thenReturn(objectName);

        Result<Item> mockResult = mock(Result.class);
        when(mockResult.get()).thenReturn(mockItem);
        return mockResult;
    }

    @Test
    void shouldBeDownloadResourceWithOnePackage() throws Exception {
        int userId = 1;
        String path = "the1/files/";

        Iterable<Result<Item>> objects = Arrays.asList(createMockResultForDownload("the1/files/", 0L),
                createMockResultForDownload("the1/files/empty-folder/", 0L),
                createMockResultForDownload("the1/files/file.txt", 100L),
                createMockResultForDownload("the1/files/file2.txt", 100L));

        when(minioService.getObjects(eq(path), eq(true))).thenReturn(objects);

        String content1 = "Some content";
        String content2 = "Some content2";

        when(minioService.getObject(eq(path + "file.txt"))).thenReturn(createInputStream(content1));
        when(minioService.getObject(eq(path + "file2.txt"))).thenReturn(createInputStream(content2));

        InputStreamResource inputStreamResource = resourceService.downloadResource(path, userId);

        Map<String, String> zipContents = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStreamResource.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                String entryContents = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                zipContents.put(entryName, entryContents);
            }
            zipInputStream.closeEntry();
        }
        assertEquals(2, zipContents.size(), "2 Файла");
        assertEquals(content1, zipContents.get("file.txt"), "Первый контент");
        assertEquals(content2, zipContents.get("file2.txt"), "Второй контент");

        verify(minioService, never()).getObject(eq(path));
        verify(minioService, times(1)).getObjects(eq(path), eq(true));
    }

    @Test
    void shouldReturnTheActualFileLocationAfterMove() throws Exception {
        int userId = 1;
        String pathFrom = "the1/onepackage/file.txt";
        String pathTo = "the1/twopackage/file.txt";
        when(minioService.metadataObject(pathFrom, userId)).thenReturn(FileResource.builder().name("file.txt").build());
        when(minioService.metadataObject(pathTo, userId)).thenReturn(FileResource.builder().size(10L).type("text/plain").build());
        FileResource fileResource = resourceService.moveResoutce(pathFrom, pathTo, 1);
        assertEquals("the1/twopackage/", fileResource.getPath());
        assertEquals("file.txt", fileResource.getName());
        assertEquals(10L, fileResource.getSize());
        verify(minioService).copy(pathFrom, pathTo);
        verify(minioService).removeObject(pathFrom);
    }

    @Test
    void shouldReturnTheActualDirectoryLocationAfterMove() throws Exception {
        int userId = 1;
        String pathFrom = "the1/onepackage/";
        String pathTo = "the1/twopackage/";
        when(minioService.metadataObject("the1/onepackage/", userId)).thenReturn(FileResource.builder().name("onepackage/").build());
        when(minioService.metadataObject(pathTo, userId)).thenReturn(FileResource.builder().size(0L).type("application/octet-stream").build());
        Iterable<Result<Item>> objects = List.of(createMockResultForDownload(pathFrom, 0L));
        when(minioService.getObjects(pathFrom, true)).thenReturn(objects);
        FileResource fileResource = resourceService.moveResoutce(pathFrom, pathTo, 1);
        assertEquals("the1/", fileResource.getPath());
        assertEquals("twopackage/", fileResource.getName());
        verify(minioService).copy(pathFrom, pathTo);
        verify(minioService).removeObject(pathFrom);
    }

    @Test
    void shouldBeSearchResource() throws Exception {
        String query = "file";
        Integer userId = 1;
        String path = "the" + userId + "/";
        Iterable<Result<Item>> objects = Arrays.asList(createMockResult("the1/papka/file.txt", 100L), createMockResult("the1/papka/file2.txt", 100L));
        when(minioService.getObjects(eq(path), eq(true))).thenReturn(objects);

        List<FileResource> listValue = resourceService.searchResource(query, userId);
        assertNotNull(listValue, "Не нулл");

        assertNotNull(listValue.get(0), "Файл не нулл");
        assertEquals("the1/papka/", listValue.get(0).getPath(), " Проверка пути");
        assertEquals("file.txt", listValue.get(0).getName(), " Проверка имени");
        assertEquals("FILE", listValue.get(0).getType(), " Проверка типа");
        assertEquals(100L, listValue.get(0).getSize(), "Проверка размера");

    }

}
