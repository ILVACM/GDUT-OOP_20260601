package com.cps.backend.modules.M04scorestatistics.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M01userauth.dto.RegisterReq;
import com.cps.backend.modules.M01userauth.dto.UserVO;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M01userauth.service.UserService;
import com.cps.backend.modules.M02questionbank.dto.QuestionCreateReq;
import com.cps.backend.modules.M02questionbank.dto.QuestionVO;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import com.cps.backend.modules.M02questionbank.service.QuestionService;
import com.cps.backend.modules.M03examassembly.dto.*;
import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import com.cps.backend.modules.M03examassembly.service.ExamService;
import com.cps.backend.modules.M04scorestatistics.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// 参考 M04-Score-Statistics.md §7 业务规则
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScoreServiceTest {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private int userCounter = 0;

    // 创建测试用户并返回 userId（解决 FK 约束问题）
    private Integer createTestUser() {
        userCounter++;
        UserVO user = userService.register(new RegisterReq(
            "scoreuser" + userCounter, "password123", UserType.student));
        return user.id();
    }

    private String singleChoiceAnswerJson() {
        return "{\"version\":1,\"type\":\"SingleChoice\",\"correctOption\":\"B\",\"options\":[\"A\",\"B\",\"C\",\"D\"]}";
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

    // 创建一个 running 状态的考试
    private ExamVO createRunningExam() {
        QuestionVO q1 = questionService.create(new QuestionCreateReq(
            QuestionType.Judge, "Judge Q", 0, judgeAnswerJson()));
        QuestionVO q2 = questionService.create(new QuestionCreateReq(
            QuestionType.SingleChoice, "SC Q", 0, singleChoiceAnswerJson()));

        // starttime 在过去，endtime 在未来 → running
        String starttime = LocalDateTime.now().minusHours(1).toString();
        String endtime = LocalDateTime.now().plusHours(1).toString();

        ExamCreateManualReq req = new ExamCreateManualReq(
            "判分测试考试", starttime, endtime,
            List.of(
                new ExamQuestionItemReq(q1.id(), 10),
                new ExamQuestionItemReq(q2.id(), 10)
            ));
        ExamVO exam = examService.createManual(req);
        examService.publish(exam.id());
        return exam;
    }

    @Test
    @DisplayName("提交答卷成功 - 判断题正确")
    void submitJudgeCorrect() {
        ExamVO exam = createRunningExam();
        // 判断题 correct=true，回答 true
        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(exam.questionItems().get(0).questionId(), true)));
        ScoreVO vo = scoreService.submitExam(req, createTestUser());
        assertNotNull(vo.id());
        assertEquals(10, vo.all()); // 判断题正确得10分
    }

    @Test
    @DisplayName("ScoreVO 包含 userName")
    void scoreVOContainsUserName() {
        // 注册一个用户
        UserVO testUser = userService.register(new RegisterReq(
            "scoretestuser", "password123", UserType.student));

        ExamVO exam = createRunningExam();
        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(exam.questionItems().get(0).questionId(), true)));
        ScoreVO vo = scoreService.submitExam(req, testUser.id());

        // 验证 ScoreVO 中 userName 不为 null 且与注册用户名一致
        assertNotNull(vo.userName());
        assertEquals("scoretestuser", vo.userName());
    }

    @Test
    @DisplayName("提交答卷 - 判断题错误")
    void submitJudgeWrong() {
        ExamVO exam = createRunningExam();
        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(exam.questionItems().get(0).questionId(), false)));
        ScoreVO vo = scoreService.submitExam(req, createTestUser());
        assertEquals(0, vo.all()); // 判断题错误得0分
    }

    @Test
    @DisplayName("提交答卷 - 单选题正确")
    void submitSingleChoiceCorrect() {
        ExamVO exam = createRunningExam();
        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(exam.questionItems().get(1).questionId(), "B")));
        ScoreVO vo = scoreService.submitExam(req, createTestUser());
        assertEquals(10, vo.all());
    }

    @Test
    @DisplayName("提交答卷 - 单选题错误")
    void submitSingleChoiceWrong() {
        ExamVO exam = createRunningExam();
        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(exam.questionItems().get(1).questionId(), "A")));
        ScoreVO vo = scoreService.submitExam(req, createTestUser());
        assertEquals(0, vo.all());
    }

    @Test
    @DisplayName("重复提交拒绝")
    void duplicateSubmission() {
        ExamVO exam = createRunningExam();
        Integer userId = createTestUser();
        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(exam.questionItems().get(0).questionId(), true)));
        scoreService.submitExam(req, userId);
        // 同一用户同一考试再次提交
        BusinessException ex = assertThrows(BusinessException.class,
            () -> scoreService.submitExam(req, userId));
        assertEquals(4401, ex.getCode());
    }

    @Test
    @DisplayName("简答题 - 初始score=0, isCorrect=null")
    void essayInitialScore() {
        QuestionVO essayQ = questionService.create(new QuestionCreateReq(
            QuestionType.Essay, "Essay Q", 0, essayAnswerJson()));
        QuestionVO judgeQ = questionService.create(new QuestionCreateReq(
            QuestionType.Judge, "Judge Q", 0, judgeAnswerJson()));

        String starttime = LocalDateTime.now().minusHours(1).toString();
        String endtime = LocalDateTime.now().plusHours(1).toString();

        ExamCreateManualReq examReq = new ExamCreateManualReq(
            "简答题测试", starttime, endtime,
            List.of(
                new ExamQuestionItemReq(essayQ.id(), 20),
                new ExamQuestionItemReq(judgeQ.id(), 10)
            ));
        ExamVO exam = examService.createManual(examReq);
        examService.publish(exam.id());

        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(
                new AnswerItem(essayQ.id(), "我的答案"),
                new AnswerItem(judgeQ.id(), true)
            ));
        ScoreVO vo = scoreService.submitExam(req, createTestUser());
        // 简答题0分 + 判断题10分
        assertEquals(10, vo.all());
        // 检查简答题的 isCorrect 为 null
        DetailItemVO essayItem = vo.detail().stream()
            .filter(d -> d.questionId().equals(essayQ.id()))
            .findFirst().orElse(null);
        assertNotNull(essayItem);
        assertNull(essayItem.isCorrect());
        assertEquals(0, essayItem.score());
    }

    @Test
    @DisplayName("教师评卷 - 更新Essay分数")
    void gradeEssaySuccess() {
        QuestionVO essayQ = questionService.create(new QuestionCreateReq(
            QuestionType.Essay, "Essay Q2", 0, essayAnswerJson()));

        String starttime = LocalDateTime.now().minusHours(1).toString();
        String endtime = LocalDateTime.now().plusHours(1).toString();

        ExamCreateManualReq examReq = new ExamCreateManualReq(
            "评卷测试", starttime, endtime,
            List.of(new ExamQuestionItemReq(essayQ.id(), 20)));
        ExamVO exam = examService.createManual(examReq);
        examService.publish(exam.id());

        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(essayQ.id(), "我的答案")));
        ScoreVO submitted = scoreService.submitExam(req, createTestUser());
        assertEquals(0, submitted.all());

        // 教师评卷
        EssayGradeReq gradeReq = new EssayGradeReq(essayQ.id(), 15, "不错");
        ScoreVO graded = scoreService.gradeEssay(submitted.id(), gradeReq);
        assertEquals(15, graded.all());

        // 检查 isCorrect 更新
        DetailItemVO gradedItem = graded.detail().stream()
            .filter(d -> d.questionId().equals(essayQ.id()))
            .findFirst().orElse(null);
        assertNotNull(gradedItem);
        assertFalse(gradedItem.isCorrect()); // 15 != 20 (maxScore)
    }

    @Test
    @DisplayName("教师评卷 - 满分时isCorrect=true")
    void gradeEssayFullScore() {
        QuestionVO essayQ = questionService.create(new QuestionCreateReq(
            QuestionType.Essay, "Essay Q3", 0, essayAnswerJson()));

        String starttime = LocalDateTime.now().minusHours(1).toString();
        String endtime = LocalDateTime.now().plusHours(1).toString();

        ExamCreateManualReq examReq = new ExamCreateManualReq(
            "满分评卷", starttime, endtime,
            List.of(new ExamQuestionItemReq(essayQ.id(), 20)));
        ExamVO exam = examService.createManual(examReq);
        examService.publish(exam.id());

        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(essayQ.id(), "完美答案")));
        ScoreVO submitted = scoreService.submitExam(req, createTestUser());

        // 教师给满分
        EssayGradeReq gradeReq = new EssayGradeReq(essayQ.id(), 20, "满分");
        ScoreVO graded = scoreService.gradeEssay(submitted.id(), gradeReq);
        assertEquals(20, graded.all());

        DetailItemVO gradedItem = graded.detail().stream()
            .filter(d -> d.questionId().equals(essayQ.id()))
            .findFirst().orElse(null);
        assertNotNull(gradedItem);
        assertTrue(gradedItem.isCorrect());
    }

    @Test
    @DisplayName("判分后question.correct递增")
    void questionCorrectIncremented() {
        QuestionVO q = questionService.create(new QuestionCreateReq(
            QuestionType.Judge, "Correct increment test", 0, judgeAnswerJson()));
        int initialCorrect = q.correct();

        String starttime = LocalDateTime.now().minusHours(1).toString();
        String endtime = LocalDateTime.now().plusHours(1).toString();
        ExamCreateManualReq examReq = new ExamCreateManualReq(
            "correct测试", starttime, endtime,
            List.of(new ExamQuestionItemReq(q.id(), 10)));
        ExamVO exam = examService.createManual(examReq);
        examService.publish(exam.id());

        ExamSubmitReq req = new ExamSubmitReq(exam.id(),
            List.of(new AnswerItem(q.id(), true)));
        scoreService.submitExam(req, createTestUser());

        entityManager.flush();
        entityManager.clear();

        QuestionVO afterQ = questionService.findById(q.id());
        assertEquals(initialCorrect + 1, afterQ.correct());
    }

    @Test
    @DisplayName("考试统计报表")
    void getExamStatistics() {
        ExamVO exam = createRunningExam();
        // 提交答卷
        scoreService.submitExam(
            new ExamSubmitReq(exam.id(),
                List.of(new AnswerItem(exam.questionItems().get(0).questionId(), true))),
            createTestUser());

        ExamStatisticsVO stats = scoreService.getExamStatistics(exam.id());
        assertNotNull(stats);
        assertEquals(exam.id(), stats.examId());
        assertTrue(stats.submitCount() >= 1);
    }

    @Test
    @DisplayName("题目统计")
    void getQuestionStatistics() {
        questionService.create(new QuestionCreateReq(
            QuestionType.Judge, "Stats Q", 0, judgeAnswerJson()));
        List<QuestionStatisticsVO> stats = scoreService.getQuestionStatistics();
        assertFalse(stats.isEmpty());
    }
}
