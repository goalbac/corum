package com.corum.backend.service.file;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.file.FileDownloadLog;
import com.corum.backend.domain.file.FileDownloadLogRepository;
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

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;
    private final UploadFileRepository uploadFileRepository;
    private final FileDownloadLogRepository downloadLogRepository;

    @Value("${storage.s3.bucket}")
    private String bucket;

    private static final int THUMB_WIDTH = 600;
    private static final int SMALL_THUMB_WIDTH = 300;
    private static final double SMALL_THUMB_QUALITY = 0.72;
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

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

        byte[] originalBytes;
        try {
            originalBytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(storagePath)
                .contentType(file.getContentType())
                .contentLength((long) originalBytes.length)
                .build(),
            RequestBody.fromBytes(originalBytes)
        );

        String thumbnailPath = null;
        String mimeType = file.getContentType();
        if (mimeType != null && IMAGE_MIME_TYPES.contains(mimeType.toLowerCase())) {
            thumbnailPath = generateAndUploadThumbnail(originalBytes, storagePath, mimeType);
        }

        UploadFile uploadFile = UploadFile.builder()
                .targetType(targetType)
                .targetId(targetId)
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .storagePath(storagePath)
                .thumbnailPath(thumbnailPath)
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .uploadedBy(uploadedBy)
                .build();

        return new FileResponse(uploadFileRepository.save(uploadFile));
    }

    private String generateAndUploadThumbnail(byte[] imageBytes, String originalPath, String mimeType) {
        return generateAndUploadToPath(imageBytes, "thumb/" + originalPath, mimeType, THUMB_WIDTH, 0.82);
    }

    private String generateAndUploadToPath(byte[] imageBytes, String thumbPath, String mimeType, int width, double quality) {
        try {
            String outputFormat = mimeType.equalsIgnoreCase("image/png") ? "png" : "jpeg";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .width(width)
                    .outputFormat(outputFormat)
                    .outputQuality(quality)
                    .toOutputStream(out);
            byte[] thumbBytes = out.toByteArray();
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(thumbPath)
                    .contentType("image/" + outputFormat)
                    .contentLength((long) thumbBytes.length)
                    .build(),
                RequestBody.fromBytes(thumbBytes)
            );
            return thumbPath;
        } catch (Exception e) {
            log.warn("썸네일 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    // ===== 썸네일 조회 (없으면 원본 반환) =====
    @Transactional(readOnly = true)
    public byte[] readThumbnailBytes(Long fileId) {
        UploadFile uploadFile = uploadFileRepository.findById(fileId)
                .orElseThrow(() -> BusinessException.notFound("파일을 찾을 수 없습니다."));
        String path = uploadFile.getThumbnailPath() != null
                ? uploadFile.getThumbnailPath()
                : uploadFile.getStoragePath();
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(path).build()
            ).asByteArray();
        } catch (Exception e) {
            throw new BusinessException("파일을 불러올 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** 대시보드용 소형 썸네일 (300px) — 기존 썸네일을 추가 리사이징 */
    @Transactional(readOnly = true)
    public byte[] readSmallThumbnailBytes(Long fileId) {
        byte[] source = readThumbnailBytes(fileId);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(source))
                    .width(SMALL_THUMB_WIDTH)
                    .outputFormat("jpeg")
                    .outputQuality(SMALL_THUMB_QUALITY)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (Exception e) {
            return source;
        }
    }

    // ===== 파일 조회 (카운트 증가 없음, 이미지 인라인 표시용) =====
    @Transactional(readOnly = true)
    public byte[] readFileBytes(Long fileId) {
        UploadFile uploadFile = uploadFileRepository.findById(fileId)
                .orElseThrow(() -> BusinessException.notFound("파일을 찾을 수 없습니다."));
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(uploadFile.getStoragePath())
                    .build()
            ).asByteArray();
        } catch (Exception e) {
            throw new BusinessException("파일을 불러올 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

    @Transactional
    public void logDownload(Long fileId, Long memberId, String ipAddress) {
        downloadLogRepository.save(FileDownloadLog.builder()
                .fileId(fileId)
                .memberId(memberId)
                .ipAddress(ipAddress)
                .build());
    }

    // ===== 사이트 에셋 (로고, 파비콘) =====
    public String uploadSiteAsset(String type, MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        String storedName = type + "_" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String storagePath = "site/" + storedName;
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
        return "/api/files/site/" + storedName;
    }

    public byte[] downloadSiteAsset(String storedName) {
        String storagePath = "site/" + storedName;
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storagePath)
                    .build()
            ).asByteArray();
        } catch (Exception e) {
            throw new BusinessException("파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
    }

    // ===== 인라인 이미지 (에디터 본문 삽입용) =====
    public String uploadInlineImage(MultipartFile file, Long uploadedBy) {
        String ext = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String storagePath = "inline/" + storedName;
        try {
            byte[] bytes = file.getBytes();
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storagePath)
                    .contentType(file.getContentType())
                    .contentLength((long) bytes.length)
                    .build(),
                RequestBody.fromBytes(bytes)
            );
            // 대시보드용 소형 썸네일도 생성
            String mimeType = file.getContentType();
            if (mimeType != null && IMAGE_MIME_TYPES.contains(mimeType.toLowerCase())) {
                generateAndUploadToPath(bytes, "inline/thumb/" + storedName, mimeType, SMALL_THUMB_WIDTH, SMALL_THUMB_QUALITY);
            }
        } catch (IOException e) {
            throw new BusinessException("이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return "/api/files/inline/" + storedName;
    }

    public byte[] downloadInlineImage(String storedName) {
        String storagePath = "inline/" + storedName;
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(storagePath).build()
            ).asByteArray();
        } catch (Exception e) {
            throw new BusinessException("이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
    }

    /** 인라인 이미지 소형 썸네일 (대시보드용) */
    public byte[] downloadInlineThumbnail(String storedName) {
        String thumbPath = "inline/thumb/" + storedName;
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(thumbPath).build()
            ).asByteArray();
        } catch (Exception e) {
            // 썸네일 없으면 원본 반환 (기존 업로드된 파일)
            return downloadInlineImage(storedName);
        }
    }

    // ===== 프로필 이미지 서빙 =====
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

    // ===== 팝업 이미지 업로드 (S3 직접 저장, DB 미기록) =====
    public String uploadPopupImage(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        String storedName = "popup_" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String storagePath = "popup/" + storedName;
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
            throw new BusinessException("팝업 이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return "/api/files/popup/" + storedName;
    }

    public byte[] downloadPopupImage(String storedName) {
        String storagePath = "popup/" + storedName;
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(storagePath).build()
            ).asByteArray();
        } catch (Exception e) {
            throw new BusinessException("팝업 이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
    }

    // ===== 프로필 이미지 업로드 (S3 직접 저장, DB 미기록) =====
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

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
