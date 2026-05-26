package com.visage.cloudstorage.Controllers;

import com.visage.cloudstorage.Model.FileResource;
import com.visage.cloudstorage.Services.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<FileResource> upload(@RequestPart("file") MultipartFile file, @RequestParam String path) throws Exception {
        return ResponseEntity.ok(resourceService.createFile(file, path));
    }

    @GetMapping
    public ResponseEntity<InputStreamResource> download(@RequestParam String path) throws Exception {
        return ResponseEntity.ok(resourceService.download(path));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam String path) throws Exception {
        resourceService.delete(path);
        return ResponseEntity.noContent().build();
    }
}
