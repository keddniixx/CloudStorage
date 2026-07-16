package com.visage.cloudstorage.Events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadEvent {
    private String username;
    private String filename;
    private long size;
}