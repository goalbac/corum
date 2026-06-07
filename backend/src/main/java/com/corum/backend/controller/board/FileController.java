package com.corum.backend.controller.board;

import com.corum.backend.domain.file.UploadFile;
import com.corum.backend.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/api/files/{fileId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long fileId) {
        UploadFile uploadFile = fileStorageService.getUploadFile(fileId);
        byte[] data = fileStorageService.downloadFile(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(uploadFile.getOriginalName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(data.length);

        return ResponseEntity.ok().headers(headers).body(data);
    }
}
