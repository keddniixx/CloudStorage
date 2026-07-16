package com.visage.cloudstorage.Model;

import lombok.Builder;

@Builder
public record ErrorResponse(int status,String message) {
}
