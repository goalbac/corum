package com.corum.backend.controller.calendar;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.calendar.IcsImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Year;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/calendars")
@RequiredArgsConstructor
public class IcsImportController {

    private final IcsImportService icsImportService;

    /**
     * ICS 가져오기: URL 또는 파일 업로드
     * POST /api/admin/calendars/{calendarId}/import-ics
     * - url (form param, optional)
     * - file (multipart, optional)
     * - yearFrom (default: 현재 연도)
     * - yearTo   (default: 현재 연도 + 1)
     */
    @PostMapping(value = "/{calendarId}/import-ics", consumes = "multipart/form-data")
    public ApiResponse<Map<String, Integer>> importIcs(
            @PathVariable Long calendarId,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws Exception {

        int currentYear = Year.now().getValue();
        int from = yearFrom != null ? yearFrom : currentYear;
        int to   = yearTo   != null ? yearTo   : currentYear + 1;
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;

        Map<String, Integer> result;
        if (file != null && !file.isEmpty()) {
            result = icsImportService.importFromStream(calendarId, file.getInputStream(), from, to, memberId);
        } else if (url != null && !url.isBlank()) {
            result = icsImportService.importFromUrl(calendarId, url.trim(), from, to, memberId);
        } else {
            return ApiResponse.ok(Map.of("imported", 0, "skipped", 0));
        }
        return ApiResponse.ok(result);
    }
}
