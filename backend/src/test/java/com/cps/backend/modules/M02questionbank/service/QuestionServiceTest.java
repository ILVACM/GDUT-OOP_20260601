package com.cps.backend.modules.M02questionbank.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M02questionbank.dto.*;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import com.cps.backend.modules.M02questionbank.repository.QuestionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// 参考 M02-Question-Bank.md §8 业务规则
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionServiceTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private EntityManager entityManager;

    private String singleChoiceAnswerJson() {
        return "{\"version\":1,\"type\":\"SingleChoice\",\"correctOption\":\"B\",\"options\":[\"A\",\"B\",\"C\",\"D\"]}";
    }

    private String multipleChoiceAnswerJson() {
        return "{\"version\":1,\"type\":\"MultipleChoice\",\"correctOptions\":[\"A\",\"C\"],\"options\":[\"A\",\"B\",\"C\",\"D\"]}";
    }

    private String judgeAnswerJson() {
        return "{\"version\":1,\"type\":\"Judge\",\"correct\":true}";
    }

    private String fillAnswerJson() {
        return "{\"version\":1,\"type\":\"Fill\",\"blanks\":[\"went\",\"goes\"]}";
    }

    private String essayAnswerJson() {
        return "{\"version\":1,\"type\":\"Essay\",\"reference\":\"参考答案\",\"keywords\":[\"关键词\"],\"scoreRule\":\"按要点给分\"}";
    }

    @Test
    @DisplayName("创建单选题成功")
    void createSingleChoiceSuccess() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.SingleChoice, "What is 2+2?", 0, singleChoiceAnswerJson());
        QuestionVO vo = questionService.create(req);
        assertNotNull(vo.id());
        assertEquals(QuestionType.SingleChoice, vo.type());
        assertEquals(0, vo.use());
        assertEquals(0, vo.correct());
        assertNull(vo.accuracy());
    }

    @Test
    @DisplayName("创建判断题成功")
    void createJudgeSuccess() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.Judge, "The sky is blue", 0, judgeAnswerJson());
        QuestionVO vo = questionService.create(req);
        assertNotNull(vo.id());
        assertEquals(QuestionType.Judge, vo.type());
    }

    @Test
    @DisplayName("创建填空题成功")
    void createFillSuccess() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.Fill, "She ___ to school yesterday", 0, fillAnswerJson());
        QuestionVO vo = questionService.create(req);
        assertNotNull(vo.id());
    }

    @Test
    @DisplayName("创建简答题成功")
    void createEssaySuccess() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.Essay, "Explain OOP", 0, essayAnswerJson());
        QuestionVO vo = questionService.create(req);
        assertNotNull(vo.id());
    }

    @Test
    @DisplayName("创建失败 - 答案JSON格式错误")
    void createInvalidAnswerJson() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.SingleChoice, "Test", 0, "not a valid json");
        assertThrows(BusinessException.class, () -> questionService.create(req));
    }

    @Test
    @DisplayName("创建失败 - 单选题缺少correctOption")
    void createSingleChoiceMissingOption() {
        String badJson = "{\"version\":1,\"type\":\"SingleChoice\",\"options\":[\"A\",\"B\"]}";
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.SingleChoice, "Test", 0, badJson);
        assertThrows(BusinessException.class, () -> questionService.create(req));
    }

    @Test
    @DisplayName("批量导入成功")
    void batchCreateSuccess() {
        List<QuestionCreateReq> reqs = List.of(
            new QuestionCreateReq(QuestionType.Judge, "Q1", 0, judgeAnswerJson()),
            new QuestionCreateReq(QuestionType.Judge, "Q2", 0, judgeAnswerJson())
        );
        List<QuestionVO> vos = questionService.batchCreate(reqs);
        assertEquals(2, vos.size());
    }

    @Test
    @DisplayName("批量导入失败 - 超过100题")
    void batchCreateOverLimit() {
        List<QuestionCreateReq> reqs = java.util.stream.IntStream.range(0, 101)
            .mapToObj(i -> new QuestionCreateReq(QuestionType.Judge, "Q" + i, 0, judgeAnswerJson()))
            .toList();
        BusinessException ex = assertThrows(BusinessException.class,
            () -> questionService.batchCreate(reqs));
        assertEquals(4200, ex.getCode());
    }

    @Test
    @DisplayName("按ID查询题目")
    void findByIdSuccess() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.Judge, "Find test", 0, judgeAnswerJson());
        QuestionVO created = questionService.create(req);
        QuestionVO found = questionService.findById(created.id());
        assertEquals(created.id(), found.id());
    }

    @Test
    @DisplayName("按ID查询 - 题目不存在")
    void findByIdNotFound() {
        assertThrows(BusinessException.class, () -> questionService.findById(99999));
    }

    @Test
    @DisplayName("分页查询")
    void searchWithPaging() {
        questionService.create(new QuestionCreateReq(
            QuestionType.Judge, "Search test 1", 0, judgeAnswerJson()));
        questionService.create(new QuestionCreateReq(
            QuestionType.Judge, "Search test 2", 0, judgeAnswerJson()));
        QuestionQueryReq query = new QuestionQueryReq(null, null, 0, 10);
        Page<QuestionVO> page = questionService.search(query);
        assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    @DisplayName("更新题目")
    void updateSuccess() {
        QuestionCreateReq createReq = new QuestionCreateReq(
            QuestionType.Judge, "Old context", 0, judgeAnswerJson());
        QuestionVO created = questionService.create(createReq);
        QuestionUpdateReq updateReq = new QuestionUpdateReq("New context", 1, judgeAnswerJson());
        QuestionVO updated = questionService.update(created.id(), updateReq);
        assertEquals("New context", updated.context());
        assertEquals(1, updated.img());
    }

    @Test
    @DisplayName("删除题目")
    void deleteSuccess() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.Judge, "Delete test", 0, judgeAnswerJson());
        QuestionVO created = questionService.create(req);
        questionService.delete(created.id());
        assertThrows(BusinessException.class, () -> questionService.findById(created.id()));
    }

    @Test
    @DisplayName("题内统计自维护 - incrementUse")
    void incrementUseSuccess() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.Judge, "Use test", 0, judgeAnswerJson());
        QuestionVO created = questionService.create(req);
        questionService.incrementUse(created.id());
        // @Modifying 批量更新后需刷新持久化上下文，否则 findById 返回缓存旧实体
        entityManager.flush();
        entityManager.clear();
        QuestionVO after = questionService.findById(created.id());
        assertEquals(1, after.use());
    }

    @Test
    @DisplayName("题内统计自维护 - incrementCorrect")
    void incrementCorrectSuccess() {
        QuestionCreateReq req = new QuestionCreateReq(
            QuestionType.Judge, "Correct test", 0, judgeAnswerJson());
        QuestionVO created = questionService.create(req);
        questionService.incrementUse(created.id());
        questionService.incrementCorrect(created.id());
        // @Modifying 批量更新后需刷新持久化上下文，否则 findById 返回缓存旧实体
        entityManager.flush();
        entityManager.clear();
        QuestionVO after = questionService.findById(created.id());
        assertEquals(1, after.use());
        assertEquals(1, after.correct());
        assertEquals(1.0, after.accuracy());
    }
}
