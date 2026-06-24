package com.visage.cloudstorage.Controllers;

import com.visage.cloudstorage.Model.ErrorResponse;
import com.visage.cloudstorage.Model.FileResource;
import com.visage.cloudstorage.Model.User;
import com.visage.cloudstorage.Services.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping("/directory")
    public ResponseEntity<?> lookAllResource(@RequestParam("path") String path, @AuthenticationPrincipal User user) {
        validatePath(path);
        return ResponseEntity.ok().body(resourceService.resources(user.getId(), path));
    }


    @PostMapping("/directory")
    public ResponseEntity<?> createPackage(@RequestParam("path") String path, @AuthenticationPrincipal User user) {
        if (!path.isEmpty()) {
            if (!path.endsWith("/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ErrorResponse.builder().message("Невалидный или отсутствующий путь").status(400).build());
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createPackage(path, user.getId()));

    }

    @PostMapping("/resource")
    public ResponseEntity<FileResource> uploadFile(@RequestPart("object") List<MultipartFile> files,
                                                   @AuthenticationPrincipal User user,
                                                   @RequestParam String path) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.createFile(files, user, path));
    }

    @GetMapping("/resource/download")
    public ResponseEntity<?> downloadFile(@RequestParam("path") String path,
                                          @AuthenticationPrincipal User user) {
        validatePath(path);
        InputStreamResource object = null;
        object = resourceService.downloadResource(path, user.getId());
        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(path).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(object);
    }

    @GetMapping("/resource/move")
    public ResponseEntity<?> move(@RequestParam("from") String from,
                                  @RequestParam("to") String to,
                                  @AuthenticationPrincipal User user) {
        validatePath(to);
        validatePath(from);
        return ResponseEntity.ok().body(resourceService.moveResoutce(from, to, user.getId()));
    }

    @DeleteMapping("/resource")
    public ResponseEntity<?> delete(@RequestParam("path") String path,
                                    @AuthenticationPrincipal User user) {
        validatePath(path);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/resource/search")
    public ResponseEntity<?> search(@RequestParam("query") String query,
                                    @AuthenticationPrincipal User user) {
        validatePath(query);
        return ResponseEntity.ok().body(resourceService.searchResource(query, user.getId()));
    }

    @GetMapping("/resource")
    public ResponseEntity<?> lookThisResource(@RequestParam("path") String path, @AuthenticationPrincipal User user) {
        validatePath(path);
        return ResponseEntity.ok().body(resourceService.resource(user.getId(), path));
    }

    private ResponseEntity<?> validatePath(String path) {
        if (path.equals("") || path.equals("") || path.contains("../") || path.contains("..\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .message("Невалидный или отсутствующий путь")
                            .status(400)
                            .build());
        }
        return null;
    }
}