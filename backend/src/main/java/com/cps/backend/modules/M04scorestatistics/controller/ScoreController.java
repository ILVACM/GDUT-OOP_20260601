package com.cps.backend.modules.M04scorestatistics.controller;

import com.cps.backend.common.api.PageResult;
import com.cps.backend.common.api.Result;
import com.cps.backend.common.security.RequireRole;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M04scorestatistics.dto.*;
import com.cps.backend.modules.M04scorestatistics.service.ScoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 分数与统计 Controller。
 *
 * <p>参考 M04-Score-Statistics.md §6 API 接口定义。</p>
 * <p>注意：GET /scores/me/mistakes 必须在 GET /scores/{id} 之前声明，
 * 避免 Spring 将 "me" 解析为 id 参数。</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    // ===== 答题与判分 =====

    /**
     * 学生提交答卷。
     * POST /api/v1/exams/{examId}/submit
     * 参考 M04-Score-Statistics.md §6.1
     */
    @PostMapping("/exams/{examId}/submit")
    @RequireRole(UserType.student)
    public Result<ScoreVO> submitExam(
            @PathVariable Integer examId,
            @Valid @RequestBody ExamSubmitReq req,
            HttpServletRequest httpRequest) {
        // 覆盖 URL 路径中的 examId 确保一致
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        ScoreVO vo = scoreService.submitExam(req, userId);
        return Result.success(vo);
    }

    /**
     * 教师评卷（简答题）。
     * POST /api/v1/scores/{scoreId}/grade-essay
     * 参考 M04-Score-Statistics.md §6.1
     */
    @PostMapping("/scores/{scoreId}/grade-essay")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<ScoreVO> gradeEssay(
            @PathVariable Integer scoreId,
            @Valid @RequestBody EssayGradeReq req) {
        ScoreVO vo = scoreService.gradeEssay(scoreId, req);
        return Result.success(vo);
    }

    // ===== 分数查询 =====

    /**
     * 我的所有成绩（分页）。
     * GET /api/v1/scores/me
     * 参考 M04-Score-Statistics.md §6.2
     */
    @GetMapping("/scores/me")
    @RequireRole({UserType.student, UserType.teacher, UserType.admin})
    public Result<PageResult<ScoreListVO>> getMyScores(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        PageResult<ScoreListVO> result = scoreService.getMyScores(userId, page, size);
        return Result.success(result);
    }

    /**
     * 我的错题集（分页）。
     * GET /api/v1/scores/me/mistakes
     * 参考 M04-Score-Statistics.md §6.2
     *
     * <p>注意：此接口必须在 GET /scores/{id} 之前声明，
     * 否则 Spring 会将 "me" 当作 id 参数解析。</p>
     */
    @GetMapping("/scores/me/mistakes")
    @RequireRole(UserType.student)
    public Result<PageResult<MistakeItemVO>> getMyMistakes(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        PageResult<MistakeItemVO> result = scoreService.getMyMistakes(userId, page, size);
        return Result.success(result);
    }

    /**
     * 分数详情。
     * GET /api/v1/scores/{id}
     * 参考 M04-Score-Statistics.md §6.2
     */
    @GetMapping("/scores/{id}")
    @RequireRole({UserType.student, UserType.teacher, UserType.admin})
    public Result<ScoreVO> getScoreById(@PathVariable Integer id) {
        ScoreVO vo = scoreService.findById(id);
        return Result.success(vo);
    }

    /**
     * 某考试的所有考生分数（分页）。
     * GET /api/v1/exams/{examId}/scores
     * 参考 M04-Score-Statistics.md §6.2
     */
    @GetMapping("/exams/{examId}/scores")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<PageResult<ScoreListVO>> getExamScores(
            @PathVariable Integer examId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageResult<ScoreListVO> result = scoreService.getExamScores(examId, page, size);
        return Result.success(result);
    }

    // ===== 统计报表 =====

    /**
     * 考试统计报表。
     * GET /api/v1/statistics/exams/{examId}
     * 参考 M04-Score-Statistics.md §6.3
     */
    @GetMapping("/statistics/exams/{examId}")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<ExamStatisticsVO> getExamStatistics(@PathVariable Integer examId) {
        ExamStatisticsVO vo = scoreService.getExamStatistics(examId);
        return Result.success(vo);
    }

    /**
     * 题目统计列表（分页）。
     * GET /api/v1/statistics/questions
     * 参考 M04-Score-Statistics.md §6.3
     */
    @GetMapping("/statistics/questions")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<PageResult<QuestionStatisticsVO>> getQuestionStatistics(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "use") String sortBy) {
        PageResult<QuestionStatisticsVO> result = scoreService.getQuestionStatisticsPaginated(page, size, sortBy);
        return Result.success(result);
    }

    /**
     * 单题详细统计。
     * GET /api/v1/statistics/questions/{id}
     * 参考 M04-Score-Statistics.md §6.3
     */
    @GetMapping("/statistics/questions/{id}")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<QuestionStatisticsVO> getQuestionStatisticById(@PathVariable Integer id) {
        QuestionStatisticsVO vo = scoreService.getQuestionStatisticById(id);
        return Result.success(vo);
    }
}
