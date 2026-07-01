package com.corum.backend.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 구 홈페이지(hanwoolin.com) 게시판 -> Corum 게시판 1회성 이관 배치.
 * migration 프로파일에서만 등록되며, 일반 로컬/운영 기동 시에는 실행되지 않는다.
 *
 * 실행 예: ./gradlew bootRun --args='--spring.profiles.active=local,migration'
 */
@Slf4j
@Component
@Profile("migration")
public class BoardMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate; // Corum(PostgreSQL) 기본 데이터소스
    private final JdbcTemplate legacyJdbcTemplate; // 구 홈페이지(MS SQL Server)
    private final S3Client s3Client;

    // Lombok @RequiredArgsConstructor는 필드의 @Qualifier를 생성자 파라미터로 복사해주지 않아
    // 두 JdbcTemplate 빈이 뒤섞이는 문제가 있었다. 생성자를 직접 선언해 @Qualifier를
    // 파라미터에 명시적으로 붙인다.
    public BoardMigrationRunner(
            JdbcTemplate jdbcTemplate,
            @Qualifier("legacyJdbcTemplate") JdbcTemplate legacyJdbcTemplate,
            S3Client s3Client) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyJdbcTemplate = legacyJdbcTemplate;
        this.s3Client = s3Client;
    }

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Value("${legacy.ftp.host}")
    private String ftpHost;

    @Value("${legacy.ftp.port:21}")
    private int ftpPort;

    @Value("${legacy.ftp.username}")
    private String ftpUsername;

    @Value("${legacy.ftp.password}")
    private String ftpPassword;

    @Value("${legacy.ftp.root:/www}")
    private String ftpRoot;

    @Value("${migration.dry-run:true}")
    private boolean dryRun;

    // 1차: 좌제도 지도 하위 3개 게시판. 검증 끝나면 목록만 추가하면 됨.
    private static final List<BoardMigrationSpec> SPECS = List.of(
            new BoardMigrationSpec("JwaNotice", "JwaNoticeC", 29L),
            new BoardMigrationSpec("JwaCondition", "JwaConditionc", 30L),
            new BoardMigrationSpec("JwaNanoom", "JwaNanoomC", 31L),
            // 한울인 나눔방 > 알려드려요 (게시글 2768건 규모 검증용)
            new BoardMigrationSpec("notice", "noticec", 13L)
    );

    private static final Pattern INLINE_IMG = Pattern.compile(
            "src=\"(/[^\"#?]+\\.(?:jpg|jpeg|png|gif|bmp|webp))\"", Pattern.CASE_INSENSITIVE);

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 게시판 이관 시작 (dryRun={}) ===", dryRun);
        ensureMigrationMapTable();

        if (dryRun) {
            // dry-run은 MSSQL 조회 결과만 미리보기 하므로 FTP 접속이 필요 없다.
            for (BoardMigrationSpec spec : SPECS) {
                log.info("--- [DRY-RUN] {} -> board_id={} ---", spec.legacyPostTable(), spec.targetBoardId());
                migratePosts(spec, null);
                migrateComments(spec);
            }
        } else {
            try (LegacyFtpClient ftp = new LegacyFtpClient(ftpHost, ftpPort, ftpUsername, ftpPassword)) {
                for (BoardMigrationSpec spec : SPECS) {
                    log.info("--- {} -> board_id={} ---", spec.legacyPostTable(), spec.targetBoardId());
                    migratePosts(spec, ftp);
                    migrateComments(spec);
                }
            }
        }
        log.info("=== 게시판 이관 종료 ===");
    }

    private void ensureMigrationMapTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS migration_id_map (
                    legacy_table VARCHAR(100) NOT NULL,
                    legacy_oid   BIGINT       NOT NULL,
                    new_post_id  BIGINT       NOT NULL,
                    PRIMARY KEY (legacy_table, legacy_oid)
                )
                """);
    }

    private Long findMigratedPostId(String legacyTable, long legacyOid) {
        return jdbcTemplate.query(
                "SELECT new_post_id FROM migration_id_map WHERE legacy_table = ? AND legacy_oid = ?",
                ps -> {
                    ps.setString(1, legacyTable);
                    ps.setLong(2, legacyOid);
                },
                rs -> rs.next() ? rs.getLong(1) : null);
    }

    private void migratePosts(BoardMigrationSpec spec, LegacyFtpClient ftp) {
        String sql = "SELECT OID, Title, RecUName, FName, FName_, Content, RecDate, ReadCount, RecIP, FPath " +
                "FROM " + spec.legacyPostTable() + " ORDER BY OID";

        legacyJdbcTemplate.query(sql, (ResultSet rs) -> {
            long oid = rs.getLong("OID");

            if (findMigratedPostId(spec.legacyPostTable(), oid) != null) {
                log.debug("[{}#{}] 이미 이관됨, 스킵", spec.legacyPostTable(), oid);
                return;
            }

            String title = rs.getString("Title");
            String writerName = rs.getString("RecUName");
            String rawContent = rs.getString("Content");
            Timestamp recDate = rs.getTimestamp("RecDate");
            int readCount = rs.getInt("ReadCount");
            String recIp = rs.getString("RecIP");
            String fname = rs.getString("FName");
            String fnameOrig = rs.getString("FName_");
            String fpath = rs.getString("FPath");

            LocalDateTime createdAt = recDate != null ? recDate.toLocalDateTime() : LocalDateTime.now();
            String content = rewriteInlineImages(rawContent, ftp);

            if (dryRun) {
                log.info("[DRY-RUN] post 이관 예정: {}#{} \"{}\" (작성일={}, 조회수={})",
                        spec.legacyPostTable(), oid, title, createdAt, readCount);
                return;
            }

            Long newPostId = jdbcTemplate.queryForObject(
                    "INSERT INTO posts (board_id, title, content, writer_name, view_count, client_ip, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class,
                    spec.targetBoardId(), title, content, writerName, readCount, recIp, createdAt, createdAt);

            jdbcTemplate.update(
                    "INSERT INTO migration_id_map (legacy_table, legacy_oid, new_post_id) VALUES (?, ?, ?)",
                    spec.legacyPostTable(), oid, newPostId);

            migrateAttachments(fname, fnameOrig, fpath, newPostId, createdAt, ftp);
            log.info("[{}#{}] -> post_id={} 이관 완료", spec.legacyPostTable(), oid, newPostId);
        });
    }

    private void migrateComments(BoardMigrationSpec spec) {
        String sql = "SELECT OID, POID, RecUName, Content, RecDate, RecIP FROM " +
                spec.legacyCommentTable() + " ORDER BY OID";

        legacyJdbcTemplate.query(sql, (ResultSet rs) -> {
            long oid = rs.getLong("OID");
            long poid = rs.getLong("POID");
            String writerName = rs.getString("RecUName");
            String content = rs.getString("Content");
            Timestamp recDate = rs.getTimestamp("RecDate");
            String recIp = rs.getString("RecIP");
            LocalDateTime createdAt = recDate != null ? recDate.toLocalDateTime() : LocalDateTime.now();

            Long newPostId = findMigratedPostId(spec.legacyPostTable(), poid);
            if (newPostId == null) {
                log.warn("[{}#{}] 대상 게시글(POID={})이 아직 이관되지 않아 댓글 스킵", spec.legacyCommentTable(), oid, poid);
                return;
            }

            // migration_id_map은 (legacy_table, legacy_oid) 단위로 이미 이관됐는지 기억한다.
            // 댓글 테이블명은 게시글 테이블명과 겹치지 않으므로 같은 테이블을 재사용해도 안전하다.
            // 재실행 시 게시글은 건너뛰지만 댓글은 이 체크가 없으면 매번 중복 삽입된다.
            if (findMigratedPostId(spec.legacyCommentTable(), oid) != null) {
                log.debug("[{}#{}] 이미 이관됨, 스킵", spec.legacyCommentTable(), oid);
                return;
            }

            if (dryRun) {
                log.info("[DRY-RUN] comment 이관 예정: {}#{} -> post_id={}", spec.legacyCommentTable(), oid, newPostId);
                return;
            }

            Long newCommentId = jdbcTemplate.queryForObject(
                    "INSERT INTO comments (post_id, writer_name, content, depth, sort_order, is_deleted, client_ip, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 0, 0, false, ?, ?, ?) RETURNING id",
                    Long.class,
                    newPostId, writerName, content, recIp, createdAt, createdAt);

            jdbcTemplate.update(
                    "INSERT INTO migration_id_map (legacy_table, legacy_oid, new_post_id) VALUES (?, ?, ?)",
                    spec.legacyCommentTable(), oid, newCommentId);
        });
    }

    /** FName/FName_(콤마 구분, 인덱스로 1:1 대응)에 담긴 첨부파일을 FTP에서 받아 MinIO로 옮긴다. */
    private void migrateAttachments(String fnameCsv, String fnameOrigCsv, String fpath,
                                     Long newPostId, LocalDateTime createdAt, LegacyFtpClient ftp) {
        if (fnameCsv == null || fnameCsv.isBlank() || fpath == null || fpath.isBlank()) {
            return;
        }
        String[] storedNames = fnameCsv.split(",");
        String[] origNames = fnameOrigCsv != null ? fnameOrigCsv.split(",") : new String[0];

        for (int i = 0; i < storedNames.length; i++) {
            String stored = storedNames[i].trim();
            if (stored.isEmpty()) continue;
            String original = i < origNames.length && !origNames[i].isBlank() ? origNames[i].trim() : stored;

            String remotePath = ftpRoot + "/HBoard/" + fpath + "/" + stored;
            byte[] bytes;
            try {
                bytes = ftp.download(remotePath);
            } catch (Exception e) {
                log.warn("첨부파일 다운로드 실패: {} ({})", remotePath, e.getMessage());
                continue;
            }

            String ext = extensionOf(stored);
            String newStoredName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
            String storagePath = "post/" + newPostId + "/" + newStoredName;
            String mimeType = guessMimeType(ext);

            uploadToS3(storagePath, bytes, mimeType);

            jdbcTemplate.update(
                    "INSERT INTO files (target_type, target_id, original_name, stored_name, storage_path, mime_type, file_size, download_count, created_at) " +
                            "VALUES ('POST', ?, ?, ?, ?, ?, ?, 0, ?)",
                    newPostId, original, newStoredName, storagePath, mimeType, (long) bytes.length, createdAt);
        }
    }

    /** 본문 HTML 속 절대경로 이미지(src="/UploadImages/...")를 찾아 FTP에서 받아 MinIO에 올리고 URL을 치환한다. */
    private String rewriteInlineImages(String content, LegacyFtpClient ftp) {
        if (content == null || content.isBlank()) return content;

        Matcher m = INLINE_IMG.matcher(content);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (m.find()) {
            String legacyPath = m.group(1);
            String newUrl = migrateInlineImage(legacyPath, ftp);
            result.append(content, last, m.start(1)).append(newUrl != null ? newUrl : legacyPath);
            last = m.end(1);
        }
        result.append(content.substring(last));
        return result.toString();
    }

    private String migrateInlineImage(String legacyPath, LegacyFtpClient ftp) {
        if (dryRun) {
            log.info("[DRY-RUN] 인라인 이미지 이관 예정: {}", legacyPath);
            return null;
        }

        // 본문 HTML에는 브라우저가 URL 인코딩한 경로(src="/UploadImages/.../%ec%98%b9.jpg")가 그대로
        // 박혀 있다. FTP 서버엔 percent-encoding이 아니라 실제 한글 파일명으로 존재하므로 디코딩 필요.
        String decodedPath;
        try {
            decodedPath = java.net.URLDecoder.decode(legacyPath, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            decodedPath = legacyPath;
        }
        String remotePath = ftpRoot + decodedPath;
        byte[] bytes;
        try {
            bytes = ftp.download(remotePath);
        } catch (Exception e) {
            log.warn("인라인 이미지 다운로드 실패: {} ({})", remotePath, e.getMessage());
            return null;
        }

        String ext = extensionOf(legacyPath);
        String newStoredName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        String storagePath = "inline/" + newStoredName;
        String mimeType = guessMimeType(ext);

        uploadToS3(storagePath, bytes, mimeType);
        return "/api/files/inline/" + newStoredName;
    }

    private void uploadToS3(String storagePath, byte[] bytes, String mimeType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(storagePath)
                        .contentType(mimeType)
                        .contentLength((long) bytes.length)
                        .build(),
                RequestBody.fromBytes(bytes));
    }

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 && idx < filename.length() - 1 ? filename.substring(idx + 1).toLowerCase() : "";
    }

    private String guessMimeType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "hwp" -> "application/x-hwp";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            default -> "application/octet-stream";
        };
    }
}
