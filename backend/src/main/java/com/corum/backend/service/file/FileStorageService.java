package com.corum.backend.service.file;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.file.FileDownloadLog;
import com.corum.backend.domain.file.FileDownloadLogRepository;
import com.corum.backend.domain.file.UploadFile;
import com.corum.backend.domain.file.UploadFileRepository;
import com.corum.backend.domain.message.Message;
import com.corum.backend.domain.message.MessageRecipientRepository;
import com.corum.backend.domain.message.MessageRepository;
import com.corum.backend.domain.post.Post;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.domain.setting.SiteSetting;
import com.corum.backend.domain.setting.SiteSettingRepository;
import com.corum.backend.dto.file.FileResponse;
import com.corum.backend.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
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
    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final BoardService boardService;
    private final MessageRepository messageRepository;
    private final MessageRecipientRepository messageRecipientRepository;
    private final SiteSettingRepository siteSettingRepository;

    @Value("${storage.s3.bucket}")
    private String bucket;

    private static final int THUMB_WIDTH = 600;
    private static final int SMALL_THUMB_WIDTH = 480;
    private static final double SMALL_THUMB_QUALITY = 0.75;
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> VIDEO_MIME_TYPES = Set.of(
            "video/mp4", "video/webm", "video/ogg"
    );
    // 실행/스크립트 계열 확장자는 게시판별 허용 목록 설정과 무관하게 항상 차단한다
    private static final Set<String> ALWAYS_BLOCKED_EXTENSIONS = Set.of(
            "jsp", "jspx", "php", "php3", "php4", "php5", "phtml", "asp", "aspx",
            "exe", "sh", "bat", "cmd", "com", "cgi", "pl", "py", "rb",
            "html", "htm", "svg", "js", "mjs", "jar", "war", "msi", "dll"
    );
    private static final int DEFAULT_MAX_SIZE_MB = 20;

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
        validateUpload(targetType, targetId, ext, file.getSize());
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

    // ===== 업로드 검증 (확장자 화이트리스트 + 용량) =====
    private void validateUpload(String targetType, Long targetId, String ext, long fileSize) {
        if (ext.isEmpty() || ALWAYS_BLOCKED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException("허용되지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST);
        }

        SiteSetting siteSetting = siteSettingRepository.findTopByOrderByIdAsc().orElse(null);
        String allowedCsv = siteSetting != null ? siteSetting.getFileAllowedExtensions() : null;
        Integer maxSizeMb = siteSetting != null ? siteSetting.getFileMaxSizeMb() : null;

        if ("POST".equals(targetType)) {
            Post post = postRepository.findById(targetId).orElse(null);
            if (post != null) {
                Board board = boardRepository.findById(post.getBoardId()).orElse(null);
                if (board != null) {
                    if (board.getFileAllowedExtensions() != null && !board.getFileAllowedExtensions().isBlank()) {
                        allowedCsv = board.getFileAllowedExtensions();
                    }
                    if (board.getFileMaxSizeMb() != null) {
                        maxSizeMb = board.getFileMaxSizeMb();
                    }
                }
            }
        }

        if (allowedCsv != null && !allowedCsv.isBlank()) {
            Set<String> allowed = Arrays.stream(allowedCsv.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            if (!allowed.isEmpty() && !allowed.contains(ext.toLowerCase())) {
                throw new BusinessException("허용되지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST);
            }
        }

        long limitMb = maxSizeMb != null ? maxSizeMb : DEFAULT_MAX_SIZE_MB;
        if (fileSize > limitMb * 1024L * 1024L) {
            throw new BusinessException("파일 크기가 허용된 용량(" + limitMb + "MB)을 초과했습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    // ===== 파일 접근 권한 확인 (게시글 첨부 → 게시판 권한, 쪽지 첨부 → 발신/수신자만) =====
    @Transactional(readOnly = true)
    public boolean canAccessFile(UploadFile file, Long memberId, String permType) {
        String targetType = file.getTargetType();
        Long targetId = file.getTargetId();
        if ("POST".equals(targetType)) {
            Post post = postRepository.findById(targetId).orElse(null);
            if (post == null) return false;
            return boardService.hasPermission(post.getBoardId(), memberId, permType);
        }
        if ("MESSAGE".equals(targetType)) {
            if (memberId == null) return false;
            Message message = messageRepository.findById(targetId).orElse(null);
            if (message == null) return false;
            if (memberId.equals(message.getSenderId())) return true;
            return messageRecipientRepository.existsByMessageIdAndRecipientId(targetId, memberId);
        }
        // 알 수 없는 target_type은 기본 거부
        return false;
    }

    private String generateAndUploadThumbnail(byte[] imageBytes, String originalPath, String mimeType) {
        return generateAndUploadToPath(imageBytes, "thumb/" + originalPath, mimeType, THUMB_WIDTH, 0.65);
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

    // ===== 인라인 동영상 (에디터 본문 삽입용) =====
    public String uploadInlineVideo(MultipartFile file, Long uploadedBy) {
        String mimeType = file.getContentType();
        if (mimeType == null || !VIDEO_MIME_TYPES.contains(mimeType.toLowerCase())) {
            throw new BusinessException("mp4, webm, ogg 형식의 동영상만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
        String ext = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String storagePath = "inline/video/" + storedName;
        try {
            byte[] bytes = file.getBytes();
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storagePath)
                    .contentType(mimeType)
                    .contentLength((long) bytes.length)
                    .build(),
                RequestBody.fromBytes(bytes)
            );
        } catch (IOException e) {
            throw new BusinessException("동영상 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return "/api/files/inline-video/" + storedName;
    }

    // 동영상은 브라우저의 탐색(seek)을 위해 HTTP Range 요청을 지원해야 한다.
    public ResponseEntity<byte[]> streamInlineVideo(String storedName, String rangeHeader) {
        String storagePath = "inline/video/" + storedName;
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(storagePath).build());
        } catch (Exception e) {
            throw new BusinessException("동영상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        long contentLength = head.contentLength();
        MediaType mediaType;
        try {
            mediaType = head.contentType() != null ? MediaType.parseMediaType(head.contentType()) : MediaType.valueOf("video/mp4");
        } catch (Exception e) {
            mediaType = MediaType.valueOf("video/mp4");
        }

        long start = 0;
        long end = contentLength - 1;
        boolean partial = false;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] parts = rangeHeader.substring(6).split("-", 2);
            try {
                if (!parts[0].isBlank()) start = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isBlank()) end = Long.parseLong(parts[1]);
                end = Math.min(end, contentLength - 1);
                partial = true;
            } catch (NumberFormatException ignored) {
                start = 0;
                end = contentLength - 1;
            }
        }

        byte[] data = s3Client.getObjectAsBytes(
            GetObjectRequest.builder()
                .bucket(bucket)
                .key(storagePath)
                .range("bytes=" + start + "-" + end)
                .build()
        ).asByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.add("X-Content-Type-Options", "nosniff");
        headers.setContentLength(data.length);

        if (partial) {
            headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength);
            return new ResponseEntity<>(data, headers, HttpStatus.PARTIAL_CONTENT);
        }
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
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

    // ===== 썸네일 일괄 재생성 =====
    @Transactional
    public ThumbnailRegenerateResult regenerateMissingThumbnails() {
        List<UploadFile> targets = uploadFileRepository.findImageFilesWithoutThumbnail(
                List.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp")
        );
        return doRegenerate(targets);
    }

    @Transactional
    public ThumbnailRegenerateResult regenerateAllThumbnails() {
        List<UploadFile> targets = uploadFileRepository.findAll().stream()
                .filter(f -> f.getMimeType() != null && IMAGE_MIME_TYPES.contains(f.getMimeType().toLowerCase()))
                .collect(Collectors.toList());
        return doRegenerate(targets);
    }

    private ThumbnailRegenerateResult doRegenerate(List<UploadFile> targets) {
        int success = 0, fail = 0;
        List<String> failedPaths = new ArrayList<>();

        for (UploadFile f : targets) {
            try {
                byte[] bytes = s3Client.getObjectAsBytes(
                        GetObjectRequest.builder().bucket(bucket).key(f.getStoragePath()).build()
                ).asByteArray();
                String thumbPath = generateAndUploadThumbnail(bytes, f.getStoragePath(), f.getMimeType());
                if (thumbPath != null) {
                    f.updateThumbnailPath(thumbPath);
                    success++;
                } else {
                    fail++;
                    failedPaths.add(f.getStoragePath());
                }
            } catch (Exception e) {
                log.warn("썸네일 재생성 실패 [{}]: {}", f.getStoragePath(), e.getMessage());
                fail++;
                failedPaths.add(f.getStoragePath());
            }
        }

        return new ThumbnailRegenerateResult(targets.size(), success, fail, failedPaths);
    }

    public record ThumbnailRegenerateResult(int total, int success, int fail, List<String> failedPaths) {}

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
