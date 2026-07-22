package com.visage.cloudstorage.unit.Services;

import com.visage.cloudstorage.Model.FileResource;
import com.visage.cloudstorage.Services.MinioService;
import io.minio.MinioClient;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinioServiceTest {
    @Test
    void readsObjectMetadata() throws Exception {
        MinioClient client = mock(MinioClient.class);
        StatObjectResponse response = mock(StatObjectResponse.class);
        when(client.statObject(any())).thenReturn(response);
        when(response.size()).thenReturn(24L);
        when(response.contentType()).thenReturn("text/plain");
        MinioService service = new MinioService(client);
        ReflectionTestUtils.setField(service, "bucketName", "files");
        FileResource resource = service.metadataObject("the8/report.txt", 8);
        assertEquals("the8/report.txt", resource.getPath());
        assertEquals("report.txt", resource.getName());
        assertEquals(24L, resource.getSize());
        assertEquals("text/plain", resource.getType());
    }
}
