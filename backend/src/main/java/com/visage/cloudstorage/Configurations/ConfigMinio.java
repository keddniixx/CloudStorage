package com.visage.cloudstorage.Configurations;


import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@Profile("!test")
public class ConfigMinio {

    @Value("${spring.minio.url}")
    private String url;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(url)
                .credentials("minio", "miniominio")
                .build();
    }

}
