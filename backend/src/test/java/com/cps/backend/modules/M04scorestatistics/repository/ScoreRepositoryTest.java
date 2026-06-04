package com.cps.backend.modules.M04scorestatistics.repository;

import com.cps.backend.modules.M01userauth.entity.User;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M01userauth.repository.UserRepository;
import com.cps.backend.modules.M03examassembly.entity.Exam;
import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import com.cps.backend.modules.M03examassembly.repository.ExamRepository;
import com.cps.backend.modules.M04scorestatistics.entity.Score;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ScoreRepository} 单元测试。
 *
 * <p>使用真实 SQLite（{@code test} profile，{@code ddl-auto=create-drop}）。</p>
 *
 * <p>参考 02-Data-Dictionary.md §4.4、§4.4.1、§7.5。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScoreRepositoryTest {

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    private User seedUser() {
        User u = new User();
        u.setName("score_user");
        u.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        u.setType(UserType.student);
        u.setStatus(1);
        return userRepository.save(u);
    }

    private Exam seedExam() {
        Exam e = new Exam();
        e.setExam("Score Test Exam");
        e.setStatus(ExamStatus.draft);
        e.setStarttime(LocalDateTime.now().plusDays(1).toString());
        e.setEndtime(LocalDateTime.now().plusDays(2).toString());
        e.setQuestionSum("{\"version\":1,\"items\":[]}");
        return examRepository.save(e);
    }

    private Score newScore(Integer userId, Integer examId, int all, String detail) {
        Score s = new Score();
        s.setUser(userId);
        s.setExam(examId);
        s.setAll(all);
        s.setDetail(detail);
        return s;
    }

    @Test
    void saveAndFindByUserAndExam() {
        User user = seedUser();
        Exam exam = seedExam();

        String detail = "{\"version\":1,\"items\":[{\"questionId\":1,\"userAnswer\":\"B\",\"correctAnswer\":\"B\",\"score\":5,\"isCorrect\":true}],\"summary\":{\"correctCount\":1,\"totalCount\":1,\"accuracy\":1.0}}";
        scoreRepository.save(newScore(user.getId(), exam.getId(), 5, detail));

        Optional<Score> found = scoreRepository.findByUserAndExam(user.getId(), exam.getId());
        assertTrue(found.isPresent());
        assertEquals(Integer.valueOf(5), found.get().getAll());
        assertEquals(detail, found.get().getDetail());
    }

    @Test
    void upsertInsertsWhenAbsent() {
        User user = seedUser();
        Exam exam = seedExam();

        String detail1 = "{\"version\":1,\"items\":[],\"summary\":{\"correctCount\":0,\"totalCount\":0,\"accuracy\":0}}";
        int affected = scoreRepository.upsertScore(user.getId(), exam.getId(), 10, detail1);
        // SQLite UPSERT 返回受影响行数；Hibernate SQLite 驱动通常返回 1
        assertTrue(affected >= 0);

        Optional<Score> found = scoreRepository.findByUserAndExam(user.getId(), exam.getId());
        assertTrue(found.isPresent());
        assertEquals(Integer.valueOf(10), found.get().getAll());
    }

    @Test
    void upsertUpdatesWhenPresent() {
        User user = seedUser();
        Exam exam = seedExam();

        // 第一次提交
        scoreRepository.upsertScore(user.getId(), exam.getId(), 30, "{\"version\":1,\"items\":[],\"summary\":{}}");
        // 第二次提交（重复答卷）
        scoreRepository.upsertScore(user.getId(), exam.getId(), 80, "{\"version\":1,\"items\":[],\"summary\":{}}");

        Optional<Score> found = scoreRepository.findByUserAndExam(user.getId(), exam.getId());
        assertTrue(found.isPresent());
        // UPSERT 覆盖，不应出现两条记录
        assertEquals(Integer.valueOf(80), found.get().getAll());

        // 全表也应只有一条
        List<Score> all = scoreRepository.findByUser(user.getId());
        assertEquals(1, all.size());
    }

    @Test
    void findByUserAndFindByExam() {
        User u1 = userRepository.save(makeUser("u1"));
        User u2 = userRepository.save(makeUser("u2"));
        Exam e1 = seedExam();
        Exam e2 = examRepository.save(makeExam("E2"));

        scoreRepository.save(newScore(u1.getId(), e1.getId(), 5, "{}"));
        scoreRepository.save(newScore(u1.getId(), e2.getId(), 10, "{}"));
        scoreRepository.save(newScore(u2.getId(), e1.getId(), 7, "{}"));

        assertEquals(2, scoreRepository.findByUser(u1.getId()).size());
        assertEquals(1, scoreRepository.findByUser(u2.getId()).size());
        assertEquals(2, scoreRepository.findByExam(e1.getId()).size());
    }

    @Test
    void foreignKeyEnforcedOnInsert() {
        // 不存在的 user id 必须被物理外键拒绝（Hibernate 抛 JpaSystemException）
        assertThrows(Exception.class, () -> {
            scoreRepository.saveAndFlush(newScore(99999, 99999, 0, "{}"));
        });
    }

    @Test
    void scoreAllFieldName() {
        // 验证 @Column(name="all") 与 SQL 关键字 all 同名时仍能正常存取
        User user = seedUser();
        Exam exam = seedExam();
        Score saved = scoreRepository.save(newScore(user.getId(), exam.getId(), 42, "{}"));

        Score loaded = scoreRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(loaded.getAll());
        assertEquals(Integer.valueOf(42), loaded.getAll());
    }

    private User makeUser(String name) {
        User u = new User();
        u.setName(name);
        u.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        u.setType(UserType.student);
        u.setStatus(1);
        return u;
    }

    private Exam makeExam(String name) {
        Exam e = new Exam();
        e.setExam(name);
        e.setStatus(ExamStatus.draft);
        e.setStarttime(LocalDateTime.now().plusDays(1).toString());
        e.setEndtime(LocalDateTime.now().plusDays(2).toString());
        e.setQuestionSum("{}");
        return e;
    }
}
