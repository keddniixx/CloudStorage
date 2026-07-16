package com.visage.cloudstorage.Model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResource {
    private String path;
    private String name;
    private Long size;
    private String type;
}
