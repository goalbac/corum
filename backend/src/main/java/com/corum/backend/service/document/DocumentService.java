package com.corum.backend.service.document;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.document.DocumentFolder;
import com.corum.backend.domain.document.DocumentFolderRepository;
import com.corum.backend.domain.document.DocumentVersion;
import com.corum.backend.domain.document.DocumentVersionRepository;
import com.corum.backend.domain.file.UploadFile;
import com.corum.backend.domain.file.UploadFileRepository;
import com.corum.backend.dto.document.DocumentFolderRequest;
import com.corum.backend.dto.document.DocumentFolderResponse;
import com.corum.backend.dto.document.DocumentVersionRequest;
import com.corum.backend.dto.document.DocumentVersionResponse;
import com.corum.backend.dto.file.FileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentFolderRepository documentFolderRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final UploadFileRepository uploadFileRepository;

    @Transactional(readOnly = true)
    public List<DocumentFolderResponse> getFolders(Long boardId) {
        return documentFolderRepository.findByBoardIdOrderBySortOrderAscIdAsc(boardId).stream()
                .map(DocumentFolderResponse::new)
                .toList();
    }

    @Transactional
    public DocumentFolderResponse createFolder(Long boardId, DocumentFolderRequest request) {
        validateParent(boardId, request.getParentId());
        DocumentFolder folder = DocumentFolder.builder()
                .boardId(boardId)
                .parentId(request.getParentId())
                .name(request.getName())
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .build();
        return new DocumentFolderResponse(documentFolderRepository.save(folder));
    }

    @Transactional
    public DocumentFolderResponse updateFolder(Long boardId, Long folderId, DocumentFolderRequest request) {
        DocumentFolder folder = documentFolderRepository.findById(folderId)
                .orElseThrow(() -> BusinessException.notFound("문서 폴더를 찾을 수 없습니다."));
        if (!folder.getBoardId().equals(boardId)) {
            throw BusinessException.notFound("문서 폴더를 찾을 수 없습니다.");
        }
        validateParent(boardId, request.getParentId());
        folder.update(request.getParentId(), request.getName(), request.getSortOrder());
        return new DocumentFolderResponse(folder);
    }

    @Transactional
    public void deleteFolder(Long boardId, Long folderId) {
        DocumentFolder folder = documentFolderRepository.findById(folderId)
                .orElseThrow(() -> BusinessException.notFound("문서 폴더를 찾을 수 없습니다."));
        if (!folder.getBoardId().equals(boardId)) {
            throw BusinessException.notFound("문서 폴더를 찾을 수 없습니다.");
        }
        documentFolderRepository.delete(folder);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> getVersions(Long postId) {
        return documentVersionRepository.findByPostIdOrderByVersionNumberDesc(postId).stream()
                .map(v -> new DocumentVersionResponse(v, findFile(v.getFileId())))
                .toList();
    }

    @Transactional
    public DocumentVersionResponse createVersion(Long postId, DocumentVersionRequest request, Long memberId) {
        FileResponse file = findPostFile(postId, request.getFileId());
        int nextVersion = documentVersionRepository.findMaxVersionNumber(postId) + 1;
        DocumentVersion version = DocumentVersion.builder()
                .postId(postId)
                .versionNumber(nextVersion)
                .fileId(request.getFileId())
                .changeNote(request.getChangeNote())
                .createdBy(memberId)
                .build();
        return new DocumentVersionResponse(documentVersionRepository.save(version), file);
    }

    private void validateParent(Long boardId, Long parentId) {
        if (parentId != null && !documentFolderRepository.existsByIdAndBoardId(parentId, boardId)) {
            throw BusinessException.notFound("상위 문서 폴더를 찾을 수 없습니다.");
        }
    }

    private FileResponse findFile(Long fileId) {
        return uploadFileRepository.findById(fileId)
                .map(FileResponse::new)
                .orElseThrow(() -> BusinessException.notFound("파일을 찾을 수 없습니다."));
    }

    private FileResponse findPostFile(Long postId, Long fileId) {
        UploadFile file = uploadFileRepository.findById(fileId)
                .orElseThrow(() -> BusinessException.notFound("파일을 찾을 수 없습니다."));
        if (!"POST".equals(file.getTargetType()) || !postId.equals(file.getTargetId())) {
            throw BusinessException.forbidden("해당 게시글의 첨부 파일만 버전으로 등록할 수 있습니다.");
        }
        return new FileResponse(file);
    }
}
