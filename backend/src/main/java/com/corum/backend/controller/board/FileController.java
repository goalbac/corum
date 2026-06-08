package com.corum.backend.controller.board;

import com.corum.backend.domain.file.UploadFile;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.file.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/api/files/{fileId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {
        UploadFile uploadFile = fileStorageService.getUploadFile(fileId);
        byte[] data = fileStorageService.downloadFile(fileId);

        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        fileStorageService.logDownload(fileId, memberId, ip);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(uploadFile.getOriginalName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(data.length);

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/api/files/profile/{storedName}")
    public ResponseEntity<byte[]> profileImage(@PathVariable String storedName) {
        byte[] data = fileStorageService.downloadProfileImage(storedName);

        String ext = storedName.contains(".") ? storedName.substring(storedName.lastIndexOf('.') + 1).toLowerCase() : "jpeg";
        MediaType mediaType = switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.IMAGE_JPEG;
        };

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .contentLength(data.length)
                .body(data);
    }
}
