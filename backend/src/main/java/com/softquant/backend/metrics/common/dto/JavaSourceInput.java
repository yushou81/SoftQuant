package com.softquant.backend.metrics.common.dto;

import jakarta.validation.constraints.NotBlank;

public class JavaSourceInput {

    @NotBlank(message = "fileName must not be blank")
    private String fileName;

    @NotBlank(message = "content must not be blank")
    private String content;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
