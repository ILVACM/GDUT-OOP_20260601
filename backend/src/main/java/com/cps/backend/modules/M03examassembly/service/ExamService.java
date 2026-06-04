package com.cps.backend.modules.M03examassembly.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M02questionbank.entity.Question;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import com.cps.backend.modules.M02questionbank.repository.QuestionRepository;
import com.cps.backend.modules.M02questionbank.service.QuestionService;
import com.cps.backend.modules.M03examassembly.dto.*;
import com.cps.backend.modules.M03examassembly.entity.Exam;
import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import com.cps.backend.modules.M03examassembly.repository.ExamRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// 参考 M03-Exam-Assembly.md §3/§6/§7/§8
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    // 参考 M03-Exam-Assembly.md §3.1 — 手动组卷
    @Transactional(rollbackFor = Exception.class)
    public ExamVO createManual(ExamCreateManualReq req) {
        // 校验时间
        validateTimeWindow(req.starttime(), req.endtime());

        // 校验所有 questionId 存在
        List<Integer> questionIds = req.items().stream().map(ExamQuestionItemReq::questionId).toList();
        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new BusinessException(4301, "部分题目不存在");
        }

        // 构造 question_sum JSON 快照
        Map<Integer, Question> qMap = questions.stream()
            .collect(Collectors.toMap(Question::getId, q -> q));
        List<QuestionSumItem> items = req.items().stream()
            .map(item -> new QuestionSumItem(
                item.questionId(),
                item.score(),
                qMap.get(item.questionId()).getType()
            ))
            .toList();
        int totalScore = items.stream().mapToInt(QuestionSumItem::score).sum();
        QuestionSum questionSum = new QuestionSum(1, items, items.size(), totalScore);

        // 落库 exam，status = draft
        Exam exam = new Exam();
        exam.setExam(req.exam());
        exam.setStatus(ExamStatus.draft);
        exam.setStarttime(req.starttime());
        exam.setEndtime(req.endtime());
        exam.setQuestionSum(toJson(questionSum));
        Exam saved = examRepository.save(exam);

        // 参考 M03-Exam-Assembly.md §3.3 — 事务内为每题 use += 1
        for (Integer qId : questionIds) {
            questionService.incrementUse(qId);
        }

        return toVO(saved);
    }

    // 参考 M03-Exam-Assembly.md §3.2 — 自动组卷
    @Transactional(rollbackFor = Exception.class)
    public ExamVO createAuto(ExamCreateAutoReq req) {
        validateTimeWindow(req.starttime(), req.endtime());
        AutoRule rule = req.autoRule();

        // 1. 候选集：按 typeFilter 过滤
        List<Question> candidates;
        if (rule.typeFilter() != null && !rule.typeFilter().isEmpty()) {
            candidates = questionRepository.findByTypeIn(rule.typeFilter());
        } else {
            candidates = questionRepository.findAll();
        }

        // 2. 校验候选数是否足够
        if (candidates.size() < rule.totalQuestions()) {
            throw new BusinessException(4302,
                "题库中可用题目不足：需要 " + rule.totalQuestions() + " 道，实际 " + candidates.size() + " 道");
        }

        // 3. 加权随机或完全随机
        List<Question> picked;
        if (Boolean.TRUE.equals(rule.usePenalty())) {
            picked = weightedRandomPick(candidates, rule.totalQuestions());
        } else {
            List<Question> shuffled = new ArrayList<>(candidates);
            Collections.shuffle(shuffled);
            picked = shuffled.subList(0, rule.totalQuestions());
        }

        // 4. 等分 + 余数分摊
        int scoreEach = rule.totalScore() / rule.totalQuestions();
        int remainder = rule.totalScore() % rule.totalQuestions();

        List<QuestionSumItem> items = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++) {
            int score = scoreEach + (i < remainder ? 1 : 0);
            items.add(new QuestionSumItem(picked.get(i).getId(), score, picked.get(i).getType()));
        }

        QuestionSum questionSum = new QuestionSum(1, items, items.size(), rule.totalScore());

        Exam exam = new Exam();
        exam.setExam(req.exam());
        exam.setStatus(ExamStatus.draft);
        exam.setStarttime(req.starttime());
        exam.setEndtime(req.endtime());
        exam.setQuestionSum(toJson(questionSum));
        Exam saved = examRepository.save(exam);

        // 事务内为每题 use += 1
        for (Question q : picked) {
            questionService.incrementUse(q.getId());
        }

        return toVO(saved);
    }

    public ExamVO findById(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        return toVOWithResolvedStatus(exam);
    }

    // 参考 M03-Exam-Assembly.md §8.2 — 学生视角预览（剔除答案）
    public ExamForStudentVO findForStudent(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));

        ExamStatus currentStatus = resolveCurrentStatus(exam, LocalDateTime.now());
        if (currentStatus != ExamStatus.publish && currentStatus != ExamStatus.running) {
            throw new BusinessException(4301, "考试不在可参加状态");
        }

        QuestionSum sum = parseQuestionSum(exam.getQuestionSum());
        List<Integer> qIds = sum.items().stream().map(QuestionSumItem::questionId).toList();
        // 防N+1：批量加载
        Map<Integer, Question> qMap = questionRepository.findAllById(qIds).stream()
            .collect(Collectors.toMap(Question::getId, q -> q));

        List<ExamQuestionForStudentVO> questionVOs = sum.items().stream()
            .map(item -> {
                Question q = qMap.get(item.questionId());
                if (q == null) return null;
                // 参考 M03-Exam-Assembly.md §7 业务规则7 — 答案下发安全：剔除判分关键字段
                Object options = extractOptionsOnly(q);
                return new ExamQuestionForStudentVO(
                    q.getId(),
                    q.getType(),
                    q.getContext(),
                    q.getImg(),
                    options,
                    item.score()
                );
            })
            .filter(Objects::nonNull)
            .toList();

        return new ExamForStudentVO(
            exam.getId(),
            exam.getExam(),
            exam.getStarttime(),
            exam.getEndtime(),
            questionVOs
        );
    }

    // 参考 M03-Exam-Assembly.md §7 业务规则1 — 仅 draft 可发布
    @Transactional(rollbackFor = Exception.class)
    public ExamVO publish(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        if (exam.getStatus() != ExamStatus.draft) {
            throw new BusinessException(4303, "仅草稿状态可发布");
        }
        exam.setStatus(ExamStatus.publish);
        Exam saved = examRepository.save(exam);
        return toVO(saved);
    }

    // 参考 M03-Exam-Assembly.md §7 业务规则2 — 仅 publish 可撤回
    @Transactional(rollbackFor = Exception.class)
    public ExamVO withdraw(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        if (exam.getStatus() != ExamStatus.publish) {
            throw new BusinessException(4303, "仅已发布状态可撤回");
        }
        exam.setStatus(ExamStatus.draft);
        Exam saved = examRepository.save(exam);
        return toVO(saved);
    }

    // 参考 M03-Exam-Assembly.md §7 业务规则5 — 仅 draft 可删除
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        if (exam.getStatus() != ExamStatus.draft) {
            throw new BusinessException(4303, "仅草稿状态可删除");
        }
        examRepository.delete(exam);
    }

    public List<ExamVO> findByStatus(ExamStatus status) {
        return examRepository.findByStatus(status).stream()
            .map(this::toVOWithResolvedStatus)
            .toList();
    }

    public List<ExamVO> findAll() {
        return examRepository.findAll().stream()
            .map(this::toVOWithResolvedStatus)
            .toList();
    }

    // 学生可参加的考试列表
    public List<ExamForStudentVO> findAvailableForStudent() {
        LocalDateTime now = LocalDateTime.now();
        List<Exam> exams = examRepository.findByStatusNot(ExamStatus.draft);
        return exams.stream()
            .filter(e -> {
                ExamStatus status = resolveCurrentStatus(e, now);
                return status == ExamStatus.publish || status == ExamStatus.running;
            })
            .map(e -> {
                try {
                    return findForStudent(e.getId());
                } catch (BusinessException ex) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    // 参考 M03-Exam-Assembly.md §8.3 — 状态实时判定
    public ExamStatus resolveCurrentStatus(Exam exam, LocalDateTime now) {
        if (exam.getStatus() == ExamStatus.draft || exam.getStatus() == ExamStatus.done) {
            return exam.getStatus();
        }
        // v3.0.0：starttime/endtime 为 String（ISO 8601），需解析为 LocalDateTime 后比较
        LocalDateTime starttime = LocalDateTime.parse(exam.getStarttime());
        LocalDateTime endtime = LocalDateTime.parse(exam.getEndtime());
        if (now.isBefore(starttime)) return ExamStatus.publish;
        if (now.isAfter(endtime) || now.equals(endtime)) return ExamStatus.done;
        return ExamStatus.running;
    }

    // ===== 私有辅助方法 =====

    private void validateTimeWindow(String starttime, String endtime) {
        try {
            LocalDateTime start = LocalDateTime.parse(starttime);
            LocalDateTime end = LocalDateTime.parse(endtime);
            if (!end.isAfter(start)) {
                throw new BusinessException(4300, "结束时间必须晚于开始时间");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(4300, "时间格式错误，需为ISO 8601格式");
        }
    }

    // 参考 M03-Exam-Assembly.md §3.2 — use 降权加权随机
    private List<Question> weightedRandomPick(List<Question> candidates, int count) {
        // 权重 = 1 / (1 + use)，热点题抽中概率降低
        List<Double> weights = candidates.stream()
            .map(q -> 1.0 / (1 + q.getUse()))
            .toList();
        double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();

        List<Question> picked = new ArrayList<>();
        List<Question> remaining = new ArrayList<>(candidates);
        List<Double> remainingWeights = new ArrayList<>(weights);

        Random random = new Random();
        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            double currentTotal = remainingWeights.stream().mapToDouble(Double::doubleValue).sum();
            double r = random.nextDouble() * currentTotal;
            double cumulative = 0;
            int selectedIndex = 0;
            for (int j = 0; j < remainingWeights.size(); j++) {
                cumulative += remainingWeights.get(j);
                if (r <= cumulative) {
                    selectedIndex = j;
                    break;
                }
            }
            picked.add(remaining.remove(selectedIndex));
            remainingWeights.remove(selectedIndex);
        }
        return picked;
    }

    // 从 question.answer JSON 中仅提取 options（用于学生视图，不含答案）
    private Object extractOptionsOnly(Question question) {
        try {
            tools.jackson.databind.JsonNode node = objectMapper.readTree(question.getAnswer());
            if (node.has("options")) {
                return objectMapper.treeToValue(node.get("options"), List.class);
            }
        } catch (Exception e) {
            // 解析失败返回 null
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

    private String toJson(QuestionSum questionSum) {
        try {
            return objectMapper.writeValueAsString(questionSum);
        } catch (tools.jackson.core.JacksonException e) {
            throw new BusinessException(5000, "考试题目快照序列化失败");
        }
    }

    private ExamVO toVO(Exam exam) {
        QuestionSum sum = parseQuestionSum(exam.getQuestionSum());
        List<ExamQuestionVO> items = sum.items().stream()
            .map(i -> new ExamQuestionVO(i.questionId(), i.type(), i.score()))
            .toList();
        return new ExamVO(
            exam.getId(),
            exam.getExam(),
            exam.getStatus(),
            exam.getStarttime(),
            exam.getEndtime(),
            items,
            sum.totalQuestions(),
            sum.totalScore()
        );
    }

    private ExamVO toVOWithResolvedStatus(Exam exam) {
        ExamStatus resolved = resolveCurrentStatus(exam, LocalDateTime.now());
        QuestionSum sum = parseQuestionSum(exam.getQuestionSum());
        List<ExamQuestionVO> items = sum.items().stream()
            .map(i -> new ExamQuestionVO(i.questionId(), i.type(), i.score()))
            .toList();
        return new ExamVO(
            exam.getId(),
            exam.getExam(),
            resolved,
            exam.getStarttime(),
            exam.getEndtime(),
            items,
            sum.totalQuestions(),
            sum.totalScore()
        );
    }
}
