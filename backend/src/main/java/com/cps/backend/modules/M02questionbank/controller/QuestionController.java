package com.cps.backend.modules.M02questionbank.controller;

import com.cps.backend.common.api.PageResult;
import com.cps.backend.common.api.Result;
import com.cps.backend.common.security.RequireRole;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M02questionbank.dto.*;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import com.cps.backend.modules.M02questionbank.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题库管理 Controller。
 *
 * <p>参考 M02-Question-Bank.md §7 API 接口定义。</p>
 * <p>所有接口均需要 teacher 或 admin 角色（参考 01-Global-Standards.md §3.3 业务码约定）。</p>
 *
 * <p><b>路由顺序说明</b>：
 * <code>/questions/random</code> 必须在 <code>/questions/{id}</code> 之前定义，
 * 因为 Spring 按声明顺序匹配，若先声明 <code>/{id}</code> 会将 "random" 误解析为 ID。</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 随机获取单道题目（用于自动组卷逐题筛选）。
     * GET /api/v1/questions/random?type=SingleChoice&excludedIds=1,2,3
     * 参考 M03-Exam-Assembly.md — 自动组卷单题获取模式
     *
     * <p>此方法必须在 listQuestions (GET /questions) 之前声明，
     * 以确保 Spring 优先匹配更具体的 /questions/random 路径。</p>
     */
    @GetMapping("/questions/random")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<QuestionPreviewVO> getRandomQuestion(
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) List<Integer> excludedIds) {
        QuestionPreviewVO vo = questionService.getRandomQuestion(type, excludedIds);
        return Result.success(vo);
    }

    /**
     * 创建题目。
     * POST /api/v1/questions
     * 参考 M02-Question-Bank.md §7
     */
    @PostMapping("/questions")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<QuestionVO> createQuestion(@Valid @RequestBody QuestionCreateReq req) {
        QuestionVO vo = questionService.create(req);
        return Result.success(vo);
    }

    /**
     * 批量导入题目。
     * POST /api/v1/questions/batch
     * 参考 M02-Question-Bank.md §7, §8 业务规则4
     */
    @PostMapping("/questions/batch")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<BatchImportResult> batchImportQuestions(@Valid @RequestBody List<QuestionCreateReq> reqs) {
        BatchImportResult result = questionService.batchCreate(reqs);
        return Result.success(result);
    }

    /**
     * 查询题目详情。
     * GET /api/v1/questions/{id}
     * 参考 M02-Question-Bank.md §7
     */
    @GetMapping("/questions/{id}")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<QuestionVO> getQuestionById(@PathVariable Integer id) {
        QuestionVO vo = questionService.findById(id);
        return Result.success(vo);
    }

    /**
     * 分页查询题目列表。
     * GET /api/v1/questions
     * 参考 M02-Question-Bank.md §7
     */
    @GetMapping("/questions")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<PageResult<QuestionVO>> listQuestions(
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        QuestionQueryReq req = new QuestionQueryReq(type, keyword, page, size);
        Page<QuestionVO> pageResult = questionService.search(req);
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 更新题目。
     * PUT /api/v1/questions/{id}
     * 参考 M02-Question-Bank.md §7
     */
    @PutMapping("/questions/{id}")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<QuestionVO> updateQuestion(
            @PathVariable Integer id,
            @Valid @RequestBody QuestionUpdateReq req) {
        QuestionVO vo = questionService.update(id, req);
        return Result.success(vo);
    }

    /**
     * 删除题目。
     * DELETE /api/v1/questions/{id}
     * 参考 M02-Question-Bank.md §7, §8 业务规则3
     */
    @DeleteMapping("/questions/{id}")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<Void> deleteQuestion(@PathVariable Integer id) {
        questionService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除题目。
     * DELETE /api/v1/questions/batch
     */
    @DeleteMapping("/questions/batch")
    @RequireRole({UserType.teacher, UserType.admin})
    public Result<Void> batchDeleteQuestions(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            questionService.delete(id);
        }
        return Result.success();
    }
}
