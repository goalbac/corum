package com.corum.backend.controller.message;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.message.ChatMessageResponse;
import com.corum.backend.dto.message.ConversationSummary;
import com.corum.backend.dto.message.MessageResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // 쪽지 발송 (multipart: recipientIds, content, files)
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> send(
            @RequestParam("recipientIds") List<Long> recipientIds,
            @RequestParam(value = "content", defaultValue = "") String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        messageService.sendWithFiles(recipientIds, content, files, userDetails.getMemberId());
        return ApiResponse.ok("쪽지가 발송되었습니다.");
    }

    // 받은 쪽지함
    @GetMapping("/inbox")
    public ApiResponse<Page<MessageResponse>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(messageService.getInbox(
                userDetails.getMemberId(), PageRequest.of(page, size)));
    }

    // 보낸 쪽지함
    @GetMapping("/sent")
    public ApiResponse<Page<MessageResponse>> getSent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(messageService.getSent(
                userDetails.getMemberId(), PageRequest.of(page, size)));
    }

    // 쪽지 상세 (읽음 처리)
    @GetMapping("/{recipientId}")
    public ApiResponse<MessageResponse> getDetail(
            @PathVariable Long recipientId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(messageService.getDetail(
                recipientId, userDetails.getMemberId()));
    }

    // 받은 쪽지 삭제
    @DeleteMapping("/{recipientId}")
    public ApiResponse<Void> deleteFromInbox(
            @PathVariable Long recipientId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        messageService.deleteFromInbox(recipientId, userDetails.getMemberId());
        return ApiResponse.ok("삭제되었습니다.");
    }

    // 안 읽은 쪽지 수
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = messageService.getUnreadCount(userDetails.getMemberId());
        return ApiResponse.ok(Map.of("count", count));
    }

    // ===== 채팅형 대화 API =====

    // 대화 상대 목록
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationSummary>> getConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(messageService.getConversations(userDetails.getMemberId()));
    }

    // 특정 상대와의 대화 내역
    @GetMapping("/conversations/{partnerId}")
    public ApiResponse<List<ChatMessageResponse>> getConversation(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(messageService.getConversation(
                userDetails.getMemberId(), partnerId));
    }

    // 특정 상대에게서 받은 쪽지 일괄 읽음 처리
    @PutMapping("/conversations/{partnerId}/read")
    public ApiResponse<Void> markConversationRead(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        messageService.markConversationRead(userDetails.getMemberId(), partnerId);
        return ApiResponse.ok("읽음 처리되었습니다.");
    }

    // 대화 전체 삭제
    @DeleteMapping("/conversations/{partnerId}")
    public ApiResponse<Void> deleteConversation(
            @PathVariable Long partnerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        messageService.deleteConversation(userDetails.getMemberId(), partnerId);
        return ApiResponse.ok("대화가 삭제되었습니다.");
    }
}
