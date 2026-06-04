package com.cps.backend.modules.M03examassembly.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M02questionbank.dto.QuestionCreateReq;
import com.cps.backend.modules.M02questionbank.dto.QuestionVO;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import com.cps.backend.modules.M02questionbank.service.QuestionService;
import com.cps.backend.modules.M03examassembly.dto.*;
import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// 参考 M03-Exam-Assembly.md §7 业务规则
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExamServiceTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @PersistenceContext
    private EntityManager entityManager;

    private String judgeAnswerJson() {
        return "{\"version\":1,\"type\":\"Judge\",\"correct\":true}";
    }

    private String singleChoiceAnswerJson() {
        return "{\"version\":1,\"type\":\"SingleChoice\",\"correctOption\":\"B\",\"options\":[\"A\",\"B\",\"C\",\"D\"]}";
    }

    // 创建测试用的题目
    private List<Integer> createTestQuestions(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> {
                QuestionVO q = questionService.create(new QuestionCreateReq(
                    QuestionType.Judge, "Test question " + i, 0, judgeAnswerJson()));
                return q.id();
            })
            .toList();
    }

    private String futureStarttime() {
        return LocalDateTime.now().plusDays(1).toString();
    }

    private String futureEndtime() {
        return LocalDateTime.now().plusDays(1).plusHours(2).toString();
    }

    @Test
    @DisplayName("手动组卷成功")
    void createManualSuccess() {
        List<Integer> qIds = createTestQuestions(3);
        List<ExamQuestionItemReq> items = qIds.stream()
            .map(id -> new ExamQuestionItemReq(id, 10))
            .toList();
        ExamCreateManualReq req = new ExamCreateManualReq(
            "手动组卷测试", futureStarttime(), futureEndtime(), items);
        ExamVO vo = examService.createManual(req);
        assertNotNull(vo.id());
        assertEquals("手动组卷测试", vo.exam());
        assertEquals(ExamStatus.draft, vo.status());
        assertEquals(3, vo.totalQuestions());
        assertEquals(30, vo.totalScore());
    }

    @Test
    @DisplayName("手动组卷 - 题目不存在")
    void createManualQuestionNotFound() {
        List<ExamQuestionItemReq> items = List.of(
            new ExamQuestionItemReq(99999, 10)
        );
        ExamCreateManualReq req = new ExamCreateManualReq(
            "失败组卷", futureStarttime(), futureEndtime(), items);
        assertThrows(BusinessException.class, () -> examService.createManual(req));
    }

    @Test
    @DisplayName("手动组卷 - 时间校验失败")
    void createManualInvalidTime() {
        List<Integer> qIds = createTestQuestions(1);
        List<ExamQuestionItemReq> items = List.of(new ExamQuestionItemReq(qIds.getFirst(), 10));
        // endtime before starttime
        ExamCreateManualReq req = new ExamCreateManualReq(
            "时间错误", futureEndtime(), futureStarttime(), items);
        assertThrows(BusinessException.class, () -> examService.createManual(req));
    }

    @Test
    @DisplayName("自动组卷成功")
    void createAutoSuccess() {
        createTestQuestions(5);
        AutoRule rule = new AutoRule(3, 30, null, false);
        ExamCreateAutoReq req = new ExamCreateAutoReq(
            "自动组卷测试", futureStarttime(), futureEndtime(), rule);
        ExamVO vo = examService.createAuto(req);
        assertNotNull(vo.id());
        assertEquals(ExamStatus.draft, vo.status());
        assertEquals(3, vo.totalQuestions());
        assertEquals(30, vo.totalScore());
    }

    @Test
    @DisplayName("自动组卷 - 候选题不足")
    void createAutoNotEnoughQuestions() {
        createTestQuestions(2);
        AutoRule rule = new AutoRule(5, 50, null, false);
        ExamCreateAutoReq req = new ExamCreateAutoReq(
            "题不足", futureStarttime(), futureEndtime(), rule);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> examService.createAuto(req));
        assertEquals(4302, ex.getCode());
    }

    @Test
    @DisplayName("自动组卷 - typeFilter过滤")
    void createAutoWithTypeFilter() {
        // 创建 Judge 题和 SingleChoice 题
        questionService.create(new QuestionCreateReq(
            QuestionType.Judge, "Judge Q", 0, judgeAnswerJson()));
        questionService.create(new QuestionCreateReq(
            QuestionType.SingleChoice, "SC Q", 0, singleChoiceAnswerJson()));

        AutoRule rule = new AutoRule(1, 10, List.of(QuestionType.Judge), false);
        ExamCreateAutoReq req = new ExamCreateAutoReq(
            "题型过滤", futureStarttime(), futureEndtime(), rule);
        ExamVO vo = examService.createAuto(req);
        assertNotNull(vo.id());
    }

    @Test
    @DisplayName("发布考试 - draft → publish")
    void publishSuccess() {
        List<Integer> qIds = createTestQuestions(1);
        ExamCreateManualReq req = new ExamCreateManualReq(
            "发布测试", futureStarttime(), futureEndtime(),
            List.of(new ExamQuestionItemReq(qIds.getFirst(), 10)));
        ExamVO created = examService.createManual(req);
        ExamVO published = examService.publish(created.id());
        assertEquals(ExamStatus.publish, published.status());
    }

    @Test
    @DisplayName("发布考试 - 非draft状态拒绝")
    void publishNonDraft() {
        List<Integer> qIds = createTestQuestions(1);
        ExamCreateManualReq req = new ExamCreateManualReq(
            "发布测试2", futureStarttime(), futureEndtime(),
            List.of(new ExamQuestionItemReq(qIds.getFirst(), 10)));
        ExamVO created = examService.createManual(req);
        examService.publish(created.id());
        // 已经 publish，再次发布应失败
        assertThrows(BusinessException.class, () -> examService.publish(created.id()));
    }

    @Test
    @DisplayName("撤回考试 - publish → draft")
    void withdrawSuccess() {
        List<Integer> qIds = createTestQuestions(1);
        ExamCreateManualReq req = new ExamCreateManualReq(
            "撤回测试", futureStarttime(), futureEndtime(),
            List.of(new ExamQuestionItemReq(qIds.getFirst(), 10)));
        ExamVO created = examService.createManual(req);
        examService.publish(created.id());
        ExamVO withdrawn = examService.withdraw(created.id());
        assertEquals(ExamStatus.draft, withdrawn.status());
    }

    @Test
    @DisplayName("删除考试 - 仅draft可删")
    void deleteDraftSuccess() {
        List<Integer> qIds = createTestQuestions(1);
        ExamCreateManualReq req = new ExamCreateManualReq(
            "删除测试", futureStarttime(), futureEndtime(),
            List.of(new ExamQuestionItemReq(qIds.getFirst(), 10)));
        ExamVO created = examService.createManual(req);
        examService.delete(created.id());
        // 删除后查询应抛异常
        assertThrows(BusinessException.class, () -> examService.findById(created.id()));
    }

    @Test
    @DisplayName("删除考试 - 非draft拒绝删除")
    void deleteNonDraft() {
        List<Integer> qIds = createTestQuestions(1);
        ExamCreateManualReq req = new ExamCreateManualReq(
            "删除测试2", futureStarttime(), futureEndtime(),
            List.of(new ExamQuestionItemReq(qIds.getFirst(), 10)));
        ExamVO created = examService.createManual(req);
        examService.publish(created.id());
        assertThrows(BusinessException.class, () -> examService.delete(created.id()));
    }

    @Test
    @DisplayName("状态实时判定 - draft不变")
    void resolveDraftStatus() {
        List<Integer> qIds = createTestQuestions(1);
        ExamCreateManualReq req = new ExamCreateManualReq(
            "状态测试", futureStarttime(), futureEndtime(),
            List.of(new ExamQuestionItemReq(qIds.getFirst(), 10)));
        ExamVO created = examService.createManual(req);
        // draft 状态查询时仍为 draft
        ExamVO found = examService.findById(created.id());
        assertEquals(ExamStatus.draft, found.status());
    }

    @Test
    @DisplayName("组卷后题目use递增")
    void questionUseIncrementedAfterAssembly() {
        QuestionVO q = questionService.create(new QuestionCreateReq(
            QuestionType.Judge, "Use increment test", 0, judgeAnswerJson()));
        assertEquals(0, q.use());

        ExamCreateManualReq req = new ExamCreateManualReq(
            "Use测试", futureStarttime(), futureEndtime(),
            List.of(new ExamQuestionItemReq(q.id(), 10)));
        examService.createManual(req);

        // @Modifying 批量更新后需刷新持久化上下文
        entityManager.flush();
        entityManager.clear();

        QuestionVO afterQ = questionService.findById(q.id());
        assertEquals(1, afterQ.use());
    }
}
