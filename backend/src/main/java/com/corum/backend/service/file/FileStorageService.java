package com.corum.backend.service.file;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.file.FileDownloadLog;
import com.corum.backend.domain.file.FileDownloadLogRepository;
import com.corum.backend.domain.file.UploadFile;
import com.corum.backend.domain.file.UploadFileRepository;
import com.corum.backend.domain.setting.SiteSetting;
import com.corum.backend.domain.setting.SiteSettingRepository;
import com.corum.backend.dto.file.FileResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;
    private final UploadFileRepository uploadFileRepository;
    private final FileDownloadLogRepository fileDownloadLogRepository;
    private final BoardRepository boardRepository;
    private final SiteSettingRepository siteSettingRepository;

    @org.springframework.beans.factory.annotation.Value("${storage.s3.bucket}")
    private String bucket;

    @Transactional
    public List<FileResponse> uploadFiles(String targetType, Long targetId,
                                          List<MultipartFile> files, Long uploadedBy) {
        return files.stream()
                .map(file -> uploadFile(targetType, targetId, file, uploadedBy))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<FileResponse> uploadPostFiles(Long boardId, Long postId,
                                              List<MultipartFile> files, Long uploadedBy) {
        validatePostFilePolicy(boardId, postId, files);
        return uploadFiles("POST", postId, files, uploadedBy);
    }

    @Transactional
    public FileResponse uploadFile(String targetType, Long targetId,
                                   MultipartFile file, Long uploadedBy) {
        String ext = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String storagePath = targetType.toLowerCase(Locale.ROOT) + "/" + targetId + "/" + storedName;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storagePath)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new BusinessException("파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UploadFile uploadFile = UploadFile.builder()
                .targetType(targetType)
                .targetId(targetId)
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .storagePath(storagePath)
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(uploadedBy)
                .build();

        return new FileResponse(uploadFileRepository.save(uploadFile));
    }

    @Transactional
    public byte[] downloadFile(Long fileId, Long memberId, HttpServletRequest request) {
        UploadFile uploadFile = getUploadFile(fileId);

        try {
            byte[] data = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(uploadFile.getStoragePath())
                            .build()
            ).asByteArray();

            uploadFile.increaseDownloadCount();
            fileDownloadLogRepository.save(FileDownloadLog.builder()
                    .fileId(fileId)
                    .memberId(memberId)
                    .ipAddress(getClientIp(request))
                    .build());
            return data;
        } catch (Exception e) {
            throw new BusinessException("파일 다운로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void deleteFile(Long fileId) {
        UploadFile uploadFile = getUploadFile(fileId);

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(uploadFile.getStoragePath())
                .build());

        uploadFileRepository.delete(uploadFile);
    }

    @Transactional
    public void deleteFilesByTarget(String targetType, Long targetId) {
        List<UploadFile> files = uploadFileRepository.findByTargetTypeAndTargetId(targetType, targetId);
        files.forEach(file -> {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(file.getStoragePath())
                        .build());
            } catch (Exception e) {
                log.warn("S3 file delete failed: {}", file.getStoragePath());
            }
        });
        uploadFileRepository.deleteByTargetTypeAndTargetId(targetType, targetId);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> getFiles(String targetType, Long targetId) {
        return uploadFileRepository.findByTargetTypeAndTargetId(targetType, targetId)
                .stream()
                .map(FileResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FileResponse getFirstFile(String targetType, Long targetId) {
        UploadFile file = uploadFileRepository.findFirstByTargetTypeAndTargetIdOrderByIdAsc(targetType, targetId);
        return file == null ? null : new FileResponse(file);
    }

    @Transactional(readOnly = true)
    public UploadFile getUploadFile(Long fileId) {
        return uploadFileRepository.findById(fileId)
                .orElseThrow(() -> BusinessException.notFound("파일을 찾을 수 없습니다."));
    }

    public byte[] downloadProfileImage(String storedName) {
        String storagePath = "profiles/" + storedName;
        try {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(storagePath)
                            .build()
            ).asByteArray();
        } catch (Exception e) {
            throw new BusinessException("프로필 이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
    }

    public String uploadProfileImage(Long memberId, MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        String storedName = "profile_" + memberId + "_" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String storagePath = "profiles/" + storedName;
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storagePath)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new BusinessException("프로필 이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return "/api/files/profile/" + storedName;
    }

    private void validatePostFilePolicy(Long boardId, Long postId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc().orElse(null);

        int maxCount = board.getFileMaxCount() == null ? 5 : board.getFileMaxCount();
        int currentCount = uploadFileRepository.countByTargetTypeAndTargetId("POST", postId);
        if (currentCount + files.size() > maxCount) {
            throw new BusinessException("첨부 파일은 최대 " + maxCount + "개까지 등록할 수 있습니다.");
        }

        int maxSizeMb = board.getFileMaxSizeMb() != null
                ? board.getFileMaxSizeMb()
                : setting != null && setting.getFileMaxSizeMb() != null ? setting.getFileMaxSizeMb() : 10;
        long maxBytes = maxSizeMb * 1024L * 1024L;

        Set<String> allowedExtensions = parseAllowedExtensions(
                board.getFileAllowedExtensions() != null && !board.getFileAllowedExtensions().isBlank()
                        ? board.getFileAllowedExtensions()
                        : setting == null ? null : setting.getFileAllowedExtensions()
        );

        for (MultipartFile file : files) {
            if (file.getSize() > maxBytes) {
                throw new BusinessException("파일 용량은 " + maxSizeMb + "MB 이하만 허용됩니다.");
            }
            String ext = getExtension(file.getOriginalFilename());
            if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(ext)) {
                throw new BusinessException("허용되지 않은 파일 확장자입니다: " + ext);
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
    }

    private Set<String> parseAllowedExtensions(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(ext -> ext.trim().toLowerCase(Locale.ROOT))
                .map(ext -> ext.startsWith(".") ? ext.substring(1) : ext)
                .filter(ext -> !ext.isBlank())
                .collect(Collectors.toSet());
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        return ip == null || ip.isBlank() ? request.getRemoteAddr() : ip.split(",")[0].trim();
    }
}
