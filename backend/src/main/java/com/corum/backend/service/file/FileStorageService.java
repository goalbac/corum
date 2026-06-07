package com.corum.backend.service.file;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.file.UploadFile;
import com.corum.backend.domain.file.UploadFileRepository;
import com.corum.backend.dto.file.FileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;
    private final UploadFileRepository uploadFileRepository;

    @Value("${storage.s3.bucket}")
    private String bucket;

    // ===== 파일 업로드 =====
    @Transactional
    public List<FileResponse> uploadFiles(String targetType, Long targetId,
                                          List<MultipartFile> files, Long uploadedBy) {
        return files.stream()
                .map(file -> uploadFile(targetType, targetId, file, uploadedBy))
                .collect(Collectors.toList());
    }

    @Transactional
    public FileResponse uploadFile(String targetType, Long targetId,
                                   MultipartFile file, Long uploadedBy) {
        String ext = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String storagePath = targetType.toLowerCase() + "/" + targetId + "/" + storedName;

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

    // ===== 파일 다운로드 =====
    @Transactional
    public byte[] downloadFile(Long fileId) {
        UploadFile uploadFile = uploadFileRepository.findById(fileId)
                .orElseThrow(() -> BusinessException.notFound("파일을 찾을 수 없습니다."));

        try {
            byte[] data = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(uploadFile.getStoragePath())
                    .build()
            ).asByteArray();

            uploadFile.increaseDownloadCount();
            return data;
        } catch (Exception e) {
            throw new BusinessException("파일 다운로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===== 파일 삭제 =====
    @Transactional
    public void deleteFile(Long fileId) {
        UploadFile uploadFile = uploadFileRepository.findById(fileId)
                .orElseThrow(() -> BusinessException.notFound("파일을 찾을 수 없습니다."));

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(uploadFile.getStoragePath())
                .build());

        uploadFileRepository.delete(uploadFile);
    }

    @Transactional
    public void deleteFilesByTarget(String targetType, Long targetId) {
        List<UploadFile> files = uploadFileRepository
                .findByTargetTypeAndTargetId(targetType, targetId);
        files.forEach(f -> {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(f.getStoragePath())
                        .build());
            } catch (Exception e) {
                log.warn("S3 file delete failed: {}", f.getStoragePath());
            }
        });
        uploadFileRepository.deleteByTargetTypeAndTargetId(targetType, targetId);
    }

    // ===== 파일 목록 조회 =====
    @Transactional(readOnly = true)
    public List<FileResponse> getFiles(String targetType, Long targetId) {
        return uploadFileRepository.findByTargetTypeAndTargetId(targetType, targetId)
                .stream()
                .map(FileResponse::new)
                .collect(Collectors.toList());
    }

    public UploadFile getUploadFile(Long fileId) {
        return uploadFileRepository.findById(fileId)
                .orElseThrow(() -> BusinessException.notFound("파일을 찾을 수 없습니다."));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
