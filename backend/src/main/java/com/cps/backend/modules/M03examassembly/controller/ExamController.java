package com.cps.backend.modules.M03examassembly.controller;

import com.cps.backend.common.api.PageResult;
import com.cps.backend.common.api.Result;
import com.cps.backend.common.security.RequireRole;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M03examassembly.dto.*;
import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import com.cps.backend.modules.M03examassembly.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考试与组卷 Controller。
 *
 * <p>参考 M03-Exam-Assembly.md §6 API 接口定义。</p>
 * <p>所有路径以 /api/v1 为前缀（参考 01-Global-Standards.md §2.1）。</p>
 * <p>Controller 仅负责参数校验和调用 Service，不写业务逻辑（参考 §6.2 分层职责）。</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    /**
     * 创建考试（手动组卷）。
     * POST /api/v1/exams/manual
     * 参考 M03-Exam-Assembly.md §6
     */
    @PostMapping("/exams/manual")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<ExamVO> createManualExam(@Valid @RequestBody ExamCreateManualReq req) {
        ExamVO vo = examService.createManualExam(req);
        return Result.success(vo);
    }

    /**
     * 创建考试（自动组卷）。
     * POST /api/v1/exams/auto
     * 参考 M03-Exam-Assembly.md §6
     */
    @PostMapping("/exams/auto")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<ExamVO> createAutoExam(@Valid @RequestBody ExamCreateAutoReq req) {
        ExamVO vo = examService.createAutoExam(req);
        return Result.success(vo);
    }

    /**
     * 获取可参加的考试列表（学生视角）。
     * GET /api/v1/exams/available
     * 注意：此路由必须在 GET /api/v1/exams/{id} 之前映射（具体路径优先）。
     * 参考 M03-Exam-Assembly.md §6
     */
    @GetMapping("/exams/available")
    @RequireRole(UserType.student)
    public Result<List<ExamForStudentVO>> listAvailableExams() {
        List<ExamForStudentVO> vos = examService.listAvailableExams();
        return Result.success(vos);
    }

    /**
     * 考试详情（教师/管理员视角）。
     * GET /api/v1/exams/{id}
     * 参考 M03-Exam-Assembly.md §6
     */
    @GetMapping("/exams/{id}")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<ExamVO> getExamById(@PathVariable Integer id) {
        ExamVO vo = examService.getExamById(id);
        return Result.success(vo);
    }

    /**
     * 学生视角预览（脱敏，剔除答案）。
     * GET /api/v1/exams/{id}/preview
     * 参考 M03-Exam-Assembly.md §6
     */
    @GetMapping("/exams/{id}/preview")
    @RequireRole(UserType.student)
    public Result<ExamForStudentVO> getExamForStudent(@PathVariable Integer id) {
        ExamForStudentVO vo = examService.getExamForStudent(id);
        return Result.success(vo);
    }

    /**
     * 修改考试（仅 draft 状态）。
     * PUT /api/v1/exams/{id}
     * 参考 M03-Exam-Assembly.md §6
     */
    @PutMapping("/exams/{id}")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<ExamVO> updateExam(
            @PathVariable Integer id,
            @Valid @RequestBody ExamCreateManualReq req) {
        ExamVO vo = examService.updateExam(id, req);
        return Result.success(vo);
    }

    /**
     * 发布考试（draft → publish）。
     * POST /api/v1/exams/{id}/publish
     * 参考 M03-Exam-Assembly.md §6
     */
    @PostMapping("/exams/{id}/publish")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<Void> publishExam(@PathVariable Integer id) {
        examService.publishExam(id);
        return Result.success();
    }

    /**
     * 撤回考试（publish → draft）。
     * POST /api/v1/exams/{id}/withdraw
     * 参考 M03-Exam-Assembly.md §6
     */
    @PostMapping("/exams/{id}/withdraw")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<Void> withdrawExam(@PathVariable Integer id) {
        examService.withdrawExam(id);
        return Result.success();
    }

    /**
     * 删除考试（仅 draft 状态）。
     * DELETE /api/v1/exams/{id}
     * 参考 M03-Exam-Assembly.md §6
     */
    @DeleteMapping("/exams/{id}")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<Void> deleteExam(@PathVariable Integer id) {
        examService.deleteExam(id);
        return Result.success();
    }

    /**
     * 分页查询考试列表（教师/管理员）。
     * GET /api/v1/exams
     * 参考 M03-Exam-Assembly.md §6
     */
    @GetMapping("/exams")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<PageResult<ExamVO>> listExams(
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageResult<ExamVO> result = examService.listExams(page, size, status);
        return Result.success(result);
    }
}
