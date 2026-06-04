package com.cps.backend.modules.M04scorestatistics.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M02questionbank.dto.SingleChoiceAnswer;
import com.cps.backend.modules.M02questionbank.dto.MultipleChoiceAnswer;
import com.cps.backend.modules.M02questionbank.dto.JudgeAnswer;
import com.cps.backend.modules.M02questionbank.dto.FillAnswer;
import com.cps.backend.modules.M02questionbank.dto.EssayAnswer;
import com.cps.backend.modules.M02questionbank.entity.Question;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import com.cps.backend.modules.M02questionbank.repository.QuestionRepository;
import com.cps.backend.modules.M02questionbank.service.QuestionService;
import com.cps.backend.modules.M03examassembly.dto.QuestionSum;
import com.cps.backend.modules.M03examassembly.dto.QuestionSumItem;
import com.cps.backend.modules.M03examassembly.entity.Exam;
import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import com.cps.backend.modules.M03examassembly.repository.ExamRepository;
import com.cps.backend.modules.M03examassembly.service.ExamService;
import com.cps.backend.modules.M04scorestatistics.dto.*;
import com.cps.backend.modules.M04scorestatistics.entity.Score;
import com.cps.backend.modules.M04scorestatistics.repository.ScoreRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// 参考 M04-Score-Statistics.md §3/§4/§6/§7/§8/§9
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final ExamService examService;
    private final ObjectMapper objectMapper;

    // 参考 M04-Score-Statistics.md §3.2 — 答题提交与判分
    @Transactional(rollbackFor = Exception.class)
    public ScoreVO submitExam(ExamSubmitReq req, Integer userId) {
        // 1. 校验考试存在且 running
        Exam exam = examRepository.findById(req.examId())
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        ExamStatus currentStatus = examService.resolveCurrentStatus(exam, LocalDateTime.now());
        if (currentStatus != ExamStatus.running) {
            throw new BusinessException(4301, "考试不在进行中");
        }

        // 2. 重复提交校验
        if (scoreRepository.findByUserAndExam(userId, req.examId()).isPresent()) {
            throw new BusinessException(4401, "已提交过答卷");
        }

        // 3. 解析 exam.question_sum
        QuestionSum sum = parseQuestionSum(exam.getQuestionSum());

        // 4. 批量加载 question 表（防 N+1）
        List<Integer> questionIds = sum.items().stream()
            .map(QuestionSumItem::questionId).toList();
        Map<Integer, Question> qMap = questionRepository.findAllById(questionIds).stream()
            .collect(Collectors.toMap(Question::getId, q -> q));

        // 5. 构建 userAnswer 映射
        Map<Integer, AnswerItem> answerMap = req.answers().stream()
            .collect(Collectors.toMap(AnswerItem::questionId, a -> a, (a, b) -> a));

        // 6. 逐题判分
        int totalScore = 0;
        int correctCount = 0;
        List<DetailItem> detailItems = new ArrayList<>();
        List<Integer> correctQuestionIds = new ArrayList<>();

        for (QuestionSumItem item : sum.items()) {
            Question q = qMap.get(item.questionId());
            AnswerItem userAns = answerMap.get(item.questionId());

            GradingResult gr = gradeOne(q, userAns != null ? userAns.userAnswer() : null, item.score());
            totalScore += gr.score;
            if (Boolean.TRUE.equals(gr.isCorrect)) {
                correctCount++;
                correctQuestionIds.add(item.questionId());
            }
            detailItems.add(new DetailItem(
                item.questionId(),
                userAns != null ? userAns.userAnswer() : null,
                extractCorrectAnswer(q),
                gr.score,
                gr.isCorrect
            ));
        }

        // 7. 构造 score.detail JSON
        double accuracy = sum.items().isEmpty() ? 0 : (double) correctCount / sum.items().size();
        Summary summary = new Summary(correctCount, sum.items().size(), accuracy);
        ScoreDetail scoreDetail = new ScoreDetail(1, detailItems, summary);

        // 8. 持久化 score
        Score score = new Score();
        score.setUser(userId);
        score.setExam(req.examId());
        score.setAll(totalScore);
        score.setDetail(toJson(scoreDetail));
        scoreRepository.save(score);

        // 9. 事务内更新 question.correct
        for (Integer qId : correctQuestionIds) {
            questionService.incrementCorrect(qId);
        }

        return toScoreVO(score, exam, qMap, sum);
    }

    // 参考 M04-Score-Statistics.md §4 — 教师评卷
    @Transactional(rollbackFor = Exception.class)
    public ScoreVO gradeEssay(Integer scoreId, EssayGradeReq req) {
        Score score = scoreRepository.findById(scoreId)
            .orElseThrow(() -> new BusinessException(4401, "分数记录不存在"));

        ScoreDetail detail = parseScoreDetail(score.getDetail());

        // 找到对应题目的 detail item
        DetailItem targetItem = detail.items().stream()
            .filter(i -> i.questionId().equals(req.questionId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(4401, "答卷中不存在该题目"));

        Boolean wasCorrect = targetItem.isCorrect();

        // 获取该题在考试中的满分
        Exam exam = examRepository.findById(score.getExam())
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        QuestionSum sum = parseQuestionSum(exam.getQuestionSum());
        int maxScore = sum.items().stream()
            .filter(i -> i.questionId().equals(req.questionId()))
            .mapToInt(QuestionSumItem::score)
            .findFirst()
            .orElse(0);

        Boolean newIsCorrect = req.score().equals(maxScore) ? Boolean.TRUE : Boolean.FALSE;

        // 构造更新后的 items
        List<DetailItem> updatedItems = detail.items().stream()
            .map(i -> i.questionId().equals(req.questionId())
                ? new DetailItem(i.questionId(), i.userAnswer(), i.correctAnswer(), req.score(), newIsCorrect)
                : i)
            .toList();

        int newTotal = updatedItems.stream()
            .mapToInt(i -> i.score() != null ? i.score() : 0)
            .sum();
        int newCorrectCount = (int) updatedItems.stream()
            .filter(i -> Boolean.TRUE.equals(i.isCorrect()))
            .count();
        double newAccuracy = updatedItems.isEmpty() ? 0 : (double) newCorrectCount / updatedItems.size();

        ScoreDetail updatedDetail = new ScoreDetail(1, updatedItems,
            new Summary(newCorrectCount, updatedItems.size(), newAccuracy));

        score.setAll(newTotal);
        score.setDetail(toJson(updatedDetail));
        scoreRepository.save(score);

        // 题内统计自维护：仅 isCorrect 从非 true 变为 true 时 correct += 1
        if (wasCorrect != Boolean.TRUE && Boolean.TRUE.equals(newIsCorrect)) {
            questionService.incrementCorrect(req.questionId());
        }

        Map<Integer, Question> qMap = questionRepository.findAllById(
            updatedItems.stream().map(DetailItem::questionId).toList()
        ).stream().collect(Collectors.toMap(Question::getId, q -> q));

        return toScoreVO(score, exam, qMap, sum);
    }

    // 分数查询：个人所有成绩
    public List<ScoreVO> findByUser(Integer userId) {
        List<Score> scores = scoreRepository.findByUser(userId);
        return scores.stream().map(score -> {
            Exam exam = examRepository.findById(score.getExam()).orElse(null);
            QuestionSum sum = exam != null ? parseQuestionSum(exam.getQuestionSum()) : null;
            Map<Integer, Question> qMap = sum != null
                ? questionRepository.findAllById(
                    sum.items().stream().map(QuestionSumItem::questionId).toList()
                ).stream().collect(Collectors.toMap(Question::getId, q -> q))
                : Map.of();
            return toScoreVO(score, exam, qMap, sum);
        }).toList();
    }

    // 分数查询：某考试所有考生成绩
    public List<ScoreVO> findByExam(Integer examId) {
        List<Score> scores = scoreRepository.findByExam(examId);
        Exam exam = examRepository.findById(examId).orElse(null);
        QuestionSum sum = exam != null ? parseQuestionSum(exam.getQuestionSum()) : null;
        Map<Integer, Question> qMap = sum != null
            ? questionRepository.findAllById(
                sum.items().stream().map(QuestionSumItem::questionId).toList()
            ).stream().collect(Collectors.toMap(Question::getId, q -> q))
            : Map.of();
        return scores.stream().map(score -> toScoreVO(score, exam, qMap, sum)).toList();
    }

    // 分数详情
    public ScoreVO findById(Integer id) {
        Score score = scoreRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4401, "分数记录不存在"));
        Exam exam = examRepository.findById(score.getExam()).orElse(null);
        QuestionSum sum = exam != null ? parseQuestionSum(exam.getQuestionSum()) : null;
        Map<Integer, Question> qMap = sum != null
            ? questionRepository.findAllById(
                sum.items().stream().map(QuestionSumItem::questionId).toList()
            ).stream().collect(Collectors.toMap(Question::getId, q -> q))
            : Map.of();
        return toScoreVO(score, exam, qMap, sum);
    }

    // 参考 M04-Score-Statistics.md §6.2 — 错题集
    public List<MistakeItemVO> findMistakes(Integer userId) {
        List<Score> scores = scoreRepository.findByUser(userId);
        // 使用 LinkedHashMap 保持插入顺序同时去重
        Map<Integer, MistakeItemVO> mistakeMap = new LinkedHashMap<>();

        for (Score score : scores) {
            Exam exam = examRepository.findById(score.getExam()).orElse(null);
            if (exam == null) continue;
            QuestionSum sum = parseQuestionSum(exam.getQuestionSum());
            ScoreDetail detail = parseScoreDetail(score.getDetail());

            Map<Integer, Question> qMap = questionRepository.findAllById(
                sum.items().stream().map(QuestionSumItem::questionId).toList()
            ).stream().collect(Collectors.toMap(Question::getId, q -> q));

            for (DetailItem item : detail.items()) {
                if (Boolean.FALSE.equals(item.isCorrect())) {
                    // 去重：同一题只保留最近一次错误记录
                    mistakeMap.put(item.questionId(), new MistakeItemVO(
                        item.questionId(),
                        qMap.get(item.questionId()) != null ? qMap.get(item.questionId()).getType() : null,
                        qMap.get(item.questionId()) != null ? qMap.get(item.questionId()).getContext() : null,
                        extractOptions(qMap.get(item.questionId())),
                        item.userAnswer(),
                        item.correctAnswer(),
                        score.getExam(),
                        exam.getExam()
                    ));
                }
            }
        }
        return new ArrayList<>(mistakeMap.values());
    }

    // 参考 M04-Score-Statistics.md §6.3 — 考试统计报表
    public ExamStatisticsVO getExamStatistics(Integer examId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        List<Score> scores = scoreRepository.findByExam(examId);
        QuestionSum sum = parseQuestionSum(exam.getQuestionSum());
        int maxScore = sum.totalScore();

        if (scores.isEmpty()) {
            return new ExamStatisticsVO(examId, exam.getExam(), 0, 0,
                0.0, 0.0, maxScore, 0, 0, Map.of());
        }

        List<Integer> scoreValues = scores.stream()
            .map(Score::getAll)
            .sorted()
            .toList();

        double average = scoreValues.stream().mapToInt(Integer::intValue).average().orElse(0);
        int min = scoreValues.getFirst();
        int max = scoreValues.getLast();
        int median = scoreValues.get(scoreValues.size() / 2);

        // 通过率：60% 为及格线
        double passLine = maxScore * 0.6;
        long passCount = scoreValues.stream().filter(s -> s >= passLine).count();
        double passRate = (double) passCount / scores.size();

        // 分数分布：按 10 分一档
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (int i = 0; i <= maxScore; i += 10) {
            int rangeEnd = Math.min(i + 9, maxScore);
            String key = i + "-" + rangeEnd;
            int finalI = i;
            long count = scoreValues.stream()
                .filter(s -> s >= finalI && s <= rangeEnd)
                .count();
            distribution.put(key, (int) count);
        }

        return new ExamStatisticsVO(
            examId, exam.getExam(),
            scores.size(), scores.size(),
            passRate, average, max, min, median, distribution
        );
    }

    // 题目统计
    public List<QuestionStatisticsVO> getQuestionStatistics() {
        List<Question> questions = questionRepository.findAll();
        return questions.stream()
            .map(q -> new QuestionStatisticsVO(
                q.getId(),
                q.getType(),
                q.getUse(),
                q.getCorrect(),
                q.getUse() > 0 ? (double) q.getCorrect() / q.getUse() : null
            ))
            .toList();
    }

    // ===== 私有辅助方法 =====

    // 判分结果内部 record
    private record GradingResult(int score, Boolean isCorrect) {}

    // 参考 M04-Score-Statistics.md §3.3 — 判分规则（按题型）
    private GradingResult gradeOne(Question question, Object userAnswer, int maxScore) {
        if (userAnswer == null) {
            return new GradingResult(0, false);
        }

        try {
            String answerJson = question.getAnswer();
            return switch (question.getType()) {
                case SingleChoice -> {
                    SingleChoiceAnswer ans = objectMapper.readValue(answerJson, SingleChoiceAnswer.class);
                    boolean correct = ans.correctOption().equals(userAnswer.toString());
                    yield new GradingResult(correct ? maxScore : 0, correct);
                }
                case MultipleChoice -> {
                    MultipleChoiceAnswer ans = objectMapper.readValue(answerJson, MultipleChoiceAnswer.class);
                    @SuppressWarnings("unchecked")
                    List<String> userOpts = (List<String>) userAnswer;
                    Set<String> userSet = new HashSet<>(userOpts);
                    Set<String> correctSet = new HashSet<>(ans.correctOptions());
                    boolean correct = userSet.equals(correctSet);
                    yield new GradingResult(correct ? maxScore : 0, correct);
                }
                case Judge -> {
                    JudgeAnswer ans = objectMapper.readValue(answerJson, JudgeAnswer.class);
                    boolean correct = ans.correct().equals(userAnswer);
                    yield new GradingResult(correct ? maxScore : 0, correct);
                }
                case Fill -> {
                    FillAnswer ans = objectMapper.readValue(answerJson, FillAnswer.class);
                    @SuppressWarnings("unchecked")
                    List<String> userBlanks = (List<String>) userAnswer;
                    boolean correct = ans.blanks().equals(userBlanks);
                    yield new GradingResult(correct ? maxScore : 0, correct);
                }
                case Essay -> {
                    // Essay 题初始 score=0, isCorrect=null，待教师评卷
                    yield new GradingResult(0, null);
                }
            };
        } catch (Exception e) {
            // JSON 解析失败，视为答错
            return new GradingResult(0, false);
        }
    }

    // 从 question.answer JSON 中提取标准答案
    private Object extractCorrectAnswer(Question question) {
        try {
            tools.jackson.databind.JsonNode node = objectMapper.readTree(question.getAnswer());
            return switch (question.getType()) {
                case SingleChoice -> node.has("correctOption") ? node.get("correctOption").asText() : null;
                case MultipleChoice -> objectMapper.treeToValue(node.get("correctOptions"), List.class);
                case Judge -> node.has("correct") ? node.get("correct").asBoolean() : null;
                case Fill -> objectMapper.treeToValue(node.get("blanks"), List.class);
                case Essay -> node.has("reference") ? node.get("reference").asText() : null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    // 从 question.answer JSON 中提取 options
    private List<String> extractOptions(Question question) {
        if (question == null) return null;
        try {
            tools.jackson.databind.JsonNode node = objectMapper.readTree(question.getAnswer());
            if (node.has("options")) {
                @SuppressWarnings("unchecked")
                List<String> opts = objectMapper.treeToValue(node.get("options"), List.class);
                return opts;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private QuestionSum parseQuestionSum(String json) {
        try {
            return objectMapper.readValue(json, QuestionSum.class);
        } catch (tools.jackson.core.JacksonException e) {
            throw new BusinessException(5000, "考试题目快照解析失败");
        }
    }

    private ScoreDetail parseScoreDetail(String json) {
        try {
            return objectMapper.readValue(json, ScoreDetail.class);
        } catch (tools.jackson.core.JacksonException e) {
            throw new BusinessException(5000, "答题明细解析失败");
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (tools.jackson.core.JacksonException e) {
            throw new BusinessException(5000, "JSON序列化失败");
        }
    }

    // Entity → VO 转换
    private ScoreVO toScoreVO(Score score, Exam exam, Map<Integer, Question> qMap, QuestionSum sum) {
        ScoreDetail detail = parseScoreDetail(score.getDetail());
        int maxScore = sum != null ? sum.totalScore() : 0;
        double accuracy = maxScore > 0 ? (double) score.getAll() / maxScore : 0;

        List<DetailItemVO> detailVOs = detail.items().stream()
            .map(item -> {
                Question q = qMap.get(item.questionId());
                int itemMaxScore = 0;
                if (sum != null) {
                    itemMaxScore = sum.items().stream()
                        .filter(i -> i.questionId().equals(item.questionId()))
                        .mapToInt(QuestionSumItem::score)
                        .findFirst().orElse(0);
                }
                return new DetailItemVO(
                    item.questionId(),
                    q != null ? q.getType() : null,
                    q != null ? q.getContext() : null,
                    item.userAnswer(),
                    item.correctAnswer(),
                    item.score(),
                    itemMaxScore,
                    item.isCorrect()
                );
            })
            .toList();

        return new ScoreVO(
            score.getId(),
            score.getUser(),
            null, // userName 需跨模块查 user 表，Controller 层补充
            score.getExam(),
            exam != null ? exam.getExam() : null,
            score.getAll(),
            maxScore,
            accuracy,
            detailVOs
        );
    }
}
