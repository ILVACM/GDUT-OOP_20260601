package com.cps.backend.modules.M04scorestatistics.controller;

import com.cps.backend.common.api.Result;
import com.cps.backend.common.security.RequireRole;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M04scorestatistics.dto.AnswerItem;
import com.cps.backend.modules.M04scorestatistics.service.DraftCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
public class DraftController {

    private final DraftCacheService draftCacheService;

    public DraftController(DraftCacheService draftCacheService) {
        this.draftCacheService = draftCacheService;
    }

    /**
     * Save draft answers (auto-save during exam).
     * PUT /api/v1/exams/{examId}/draft
     */
    @PutMapping("/{examId}/draft")
    @RequireRole(UserType.student)
    public Result<Void> saveDraft(
            @PathVariable Integer examId,
            @Valid @RequestBody DraftSaveReq req,
            HttpServletRequest httpRequest
    ) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        draftCacheService.saveDraft(examId, userId, req.answers());
        return Result.success();
    }

    /**
     * Load draft answers.
     * GET /api/v1/exams/{examId}/draft
     */
    @GetMapping("/{examId}/draft")
    @RequireRole(UserType.student)
    public Result<List<AnswerItem>> loadDraft(
            @PathVariable Integer examId,
            HttpServletRequest httpRequest
    ) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        List<AnswerItem> answers = draftCacheService.loadDraft(examId, userId);
        return Result.success(answers != null ? answers : List.of());
    }

    public record DraftSaveReq(@NotEmpty List<AnswerItem> answers) {}
}
