package com.cps.backend.modules.M02questionbank.repository;

import com.cps.backend.modules.M02questionbank.entity.Question;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link QuestionRepository} 单元测试。
 *
 * <p>使用真实 SQLite（{@code test} profile，{@code ddl-auto=none}，DDL 由 schema/*.sql 加载）。</p>
 *
 * <p>参考 02-Data-Dictionary.md §4.2、§4.2.3、§7.2。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Question newQuestion(QuestionType type, String context, String answer, int use, int correct) {
        Question q = new Question();
        q.setType(type);
        q.setContext(context);
        q.setImg(0);
        q.setAnswer(answer);
        q.setUse(use);
        q.setCorrect(correct);
        return q;
    }

    @Test
    void saveAndFindByType() {
        questionRepository.save(newQuestion(QuestionType.SingleChoice, "1+1=?", "{\"version\":1,\"correctOption\":\"B\",\"options\":[\"A\",\"B\",\"C\",\"D\"]}", 0, 0));
        questionRepository.save(newQuestion(QuestionType.Judge, "Sky is blue", "{\"version\":1,\"correct\":true}", 0, 0));
        questionRepository.save(newQuestion(QuestionType.SingleChoice, "Capital of China?", "{\"version\":1,\"correctOption\":\"A\",\"options\":[\"A\",\"B\",\"C\",\"D\"]}", 0, 0));
        entityManager.flush();
        entityManager.clear();

        List<Question> single = questionRepository.findByType(QuestionType.SingleChoice);
        assertEquals(2, single.size());

        List<Question> judge = questionRepository.findByType(QuestionType.Judge);
        assertEquals(1, judge.size());
        assertEquals(QuestionType.Judge, judge.get(0).getType());
    }

    @Test
    void searchByKeyword() {
        questionRepository.save(newQuestion(QuestionType.Fill, "He ___ to school every day.", "{\"version\":1,\"blanks\":[\"goes\"]}", 0, 0));
        questionRepository.save(newQuestion(QuestionType.Fill, "She ___ in the library.", "{\"version\":1,\"blanks\":[\"reads\"]}", 0, 0));
        questionRepository.save(newQuestion(QuestionType.Essay, "Discuss the impact of AI on education.", "{\"version\":1,\"reference\":\"...\",\"keywords\":[\"AI\",\"education\"],\"scoreRule\":\"by key points\"}", 0, 0));
        entityManager.flush();
        entityManager.clear();

        // 关键字检索（context LIKE）
        Page<Question> schoolPage = questionRepository.searchByKeyword(
                QuestionType.Fill, "school", PageRequest.of(0, 10));
        assertEquals(1, schoolPage.getTotalElements());
        assertEquals(QuestionType.Fill, schoolPage.getContent().get(0).getType());

        // type 为 null → 不限制题型
        Page<Question> allMatches = questionRepository.searchByKeyword(
                null, "AI", PageRequest.of(0, 10));
        assertEquals(1, allMatches.getTotalElements());

        // keyword 为空 → 命中所有
        Page<Question> allQuestions = questionRepository.searchByKeyword(
                null, "", PageRequest.of(0, 10));
        assertEquals(3, allQuestions.getTotalElements());
    }

    @Test
    void incrementUseAndCorrect() {
        Question saved = questionRepository.save(newQuestion(QuestionType.Judge, "Q1", "{\"version\":1,\"correct\":true}", 0, 0));
        Integer id = saved.getId();
        entityManager.flush();
        entityManager.clear();

        // 组卷被抽中：use += 1
        questionRepository.incrementUse(id);
        questionRepository.incrementUse(id);
        // @Modifying 不刷新 1st level cache，必须 flush + clear 才能看到更新后的值
        entityManager.flush();
        entityManager.clear();

        Question afterUse = questionRepository.findById(id).orElseThrow();
        assertEquals(2, afterUse.getUse());
        assertEquals(0, afterUse.getCorrect());

        // 判分正确：correct += 1
        questionRepository.incrementCorrect(id);
        entityManager.flush();
        entityManager.clear();

        Question afterCorrect = questionRepository.findById(id).orElseThrow();
        assertEquals(2, afterCorrect.getUse());
        assertEquals(1, afterCorrect.getCorrect());
    }

    @Test
    void jsonFieldRoundTrip() {
        String json = "{\"version\":1,\"correctOption\":\"C\",\"options\":[\"A\",\"B\",\"C\",\"D\"]}";
        Question saved = questionRepository.save(newQuestion(QuestionType.SingleChoice, "JSON test", json, 0, 0));
        entityManager.flush();
        entityManager.clear();

        Question loaded = questionRepository.findById(saved.getId()).orElseThrow();
        // JSON 字段原样存取（无自动转换）
        assertEquals(json, loaded.getAnswer());
        assertNotNull(loaded.getAnswer());
        assertTrue(loaded.getAnswer().contains("correctOption"));
    }
}
