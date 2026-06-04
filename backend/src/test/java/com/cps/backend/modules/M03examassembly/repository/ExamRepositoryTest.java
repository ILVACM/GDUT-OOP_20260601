package com.cps.backend.modules.M03examassembly.repository;

import com.cps.backend.modules.M03examassembly.entity.Exam;
import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ExamRepository} 单元测试。
 *
 * <p>使用真实 SQLite（{@code test} profile，{@code ddl-auto=create-drop}）。</p>
 *
 * <p>参考 02-Data-Dictionary.md §4.3、§4.3.1、§4.3.2、§7.4。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExamRepositoryTest {

    @Autowired
    private ExamRepository examRepository;

    private Exam newExam(String name, ExamStatus status, String start, String end, String qSum) {
        Exam e = new Exam();
        e.setExam(name);
        e.setStatus(status);
        e.setStarttime(start);
        e.setEndtime(end);
        e.setQuestionSum(qSum);
        return e;
    }

    @Test
    void saveAndFindByStatus() {
        examRepository.save(newExam("Mid-term", ExamStatus.draft,
                LocalDateTime.now().plusDays(1).toString(), LocalDateTime.now().plusDays(2).toString(),
                "{\"version\":1,\"items\":[],\"totalQuestions\":0,\"totalScore\":0}"));
        examRepository.save(newExam("Final", ExamStatus.publish,
                LocalDateTime.now().plusDays(7).toString(), LocalDateTime.now().plusDays(8).toString(),
                "{\"version\":1,\"items\":[],\"totalQuestions\":0,\"totalScore\":0}"));

        List<Exam> drafts = examRepository.findByStatus(ExamStatus.draft);
        assertEquals(1, drafts.size());
        assertEquals("Mid-term", drafts.get(0).getExam());

        List<Exam> published = examRepository.findByStatus(ExamStatus.publish);
        assertEquals(1, published.size());
        assertEquals("Final", published.get(0).getExam());
    }

    @Test
    void timeWindowQuery() {
        LocalDateTime now = LocalDateTime.now();
        examRepository.save(newExam("E1", ExamStatus.draft, now.plusDays(1).toString(), now.plusDays(2).toString(), "{}"));
        examRepository.save(newExam("E2", ExamStatus.draft, now.plusDays(5).toString(), now.plusDays(6).toString(), "{}"));
        examRepository.save(newExam("E3", ExamStatus.draft, now.plusDays(20).toString(), now.plusDays(21).toString(), "{}"));

        // 查询 [now+3, now+10] 时间窗
        List<Exam> inWindow = examRepository.findByTimeWindow(now.plusDays(3).toString(), now.plusDays(10).toString());
        assertEquals(1, inWindow.size());
        assertEquals("E2", inWindow.get(0).getExam());
    }

    @Test
    void questionSumJsonRoundTrip() {
        String snapshot = "{\"version\":1,\"items\":[{\"questionId\":1,\"score\":5,\"type\":\"SingleChoice\"}],\"totalQuestions\":1,\"totalScore\":5}";
        Exam saved = examRepository.save(newExam("Snap Test", ExamStatus.draft,
                LocalDateTime.now().plusDays(1).toString(), LocalDateTime.now().plusDays(2).toString(), snapshot));

        Exam loaded = examRepository.findById(saved.getId()).orElseThrow();
        // 验证 columnDefinition="TEXT" 下 JSON 文本原样存取
        assertEquals(snapshot, loaded.getQuestionSum());
        assertTrue(loaded.getQuestionSum().contains("questionId"));
    }

    @Test
    void allFourStatusesPersist() {
        for (ExamStatus s : ExamStatus.values()) {
            examRepository.save(newExam("Exam-" + s.name(), s,
                    LocalDateTime.now().plusDays(1).toString(), LocalDateTime.now().plusDays(2).toString(), "{}"));
        }
        // 4 个状态枚举值全部能正确写入与读出
        for (ExamStatus s : ExamStatus.values()) {
            List<Exam> list = examRepository.findByStatus(s);
            assertEquals(1, list.size());
            assertEquals(s, list.get(0).getStatus());
        }
    }

    @Test
    void statusNotQuery() {
        examRepository.save(newExam("D", ExamStatus.draft, LocalDateTime.now().plusDays(1).toString(), LocalDateTime.now().plusDays(2).toString(), "{}"));
        examRepository.save(newExam("Dn", ExamStatus.done, LocalDateTime.now().minusDays(2).toString(), LocalDateTime.now().minusDays(1).toString(), "{}"));

        List<Exam> notDone = examRepository.findByStatusNot(ExamStatus.done);
        assertNotNull(notDone);
        assertTrue(notDone.stream().allMatch(e -> e.getStatus() != ExamStatus.done));
        assertEquals(1, notDone.size());
        assertEquals("D", notDone.get(0).getExam());
    }
}
