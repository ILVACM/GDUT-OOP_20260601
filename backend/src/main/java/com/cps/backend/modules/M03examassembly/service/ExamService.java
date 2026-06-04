package com.cps.backend.modules.M03examassembly.service;

import com.cps.backend.common.api.PageResult;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
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

    @Value("${user.dir}")
    private String userDir;

    // 参考 02-Data-Dictionary.md §4.2.2 — 支持的图片扩展名
    private static final String[] IMG_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif"};

    // 参考 M03-Exam-Assembly.md §3.1 — 手动组卷
    @Transactional(rollbackFor = Exception.class)
    public ExamVO createManualExam(ExamCreateManualReq req) {
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

    // 别名方法，用于兼容旧测试代码
    @Transactional(rollbackFor = Exception.class)
    public ExamVO createManual(ExamCreateManualReq req) {
        return createManualExam(req);
    }

    // 参考 M03-Exam-Assembly.md §3.2 — 自动组卷
    @Transactional(rollbackFor = Exception.class)
    public ExamVO createAutoExam(ExamCreateAutoReq req) {
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

    // 别名方法，用于兼容旧测试代码
    @Transactional(rollbackFor = Exception.class)
    public ExamVO createAuto(ExamCreateAutoReq req) {
        return createAutoExam(req);
    }

    public ExamVO getExamById(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        return toVOWithResolvedStatus(exam);
    }

    // 参考 M03-Exam-Assembly.md §8.2 — 学生视角预览（剔除答案）
    public ExamForStudentVO getExamForStudent(Integer id) {
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
                // 参考 02-Data-Dictionary.md §4.2.2 — 图片 URL 生成
                String imageUrl = resolveImageUrl(q.getId(), q.getImg());
                return new ExamQuestionForStudentVO(
                    q.getId(),
                    q.getType(),
                    q.getContext(),
                    q.getImg(),
                    imageUrl,
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
    public void publishExam(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        if (exam.getStatus() != ExamStatus.draft) {
            throw new BusinessException(4303, "仅草稿状态可发布");
        }
        exam.setStatus(ExamStatus.publish);
        examRepository.save(exam);
    }

    // 别名方法，用于兼容旧测试代码
    @Transactional(rollbackFor = Exception.class)
    public ExamVO publish(Integer id) {
        publishExam(id);
        return getExamById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdrawExam(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        if (exam.getStatus() != ExamStatus.publish) {
            throw new BusinessException(4303, "仅已发布状态可撤回");
        }
        exam.setStatus(ExamStatus.draft);
        examRepository.save(exam);
    }

    // 别名方法，用于兼容旧测试代码
    public ExamVO withdraw(Integer id) {
        withdrawExam(id);
        return getExamById(id);
    }

    // 参考 M03-Exam-Assembly.md §7 业务规则1 — 草稿可改、发布后不可改
    @Transactional(rollbackFor = Exception.class)
    public ExamVO updateExam(Integer id, ExamCreateManualReq req) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));

        // 1. 仅 draft 状态可编辑
        if (exam.getStatus() != ExamStatus.draft) {
            throw new BusinessException(4303, "仅草稿状态可编辑");
        }

        // 2. 校验时间
        validateTimeWindow(req.starttime(), req.endtime());

        // 3. 校验所有 questionId 存在
        List<Integer> questionIds = req.items().stream().map(ExamQuestionItemReq::questionId).toList();
        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new BusinessException(4301, "部分题目不存在");
        }

        // 4. 构造新的 question_sum 快照
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

        // 5. 处理 use 统计：旧题目 decrement，新题目 increment
        QuestionSum oldSum = parseQuestionSum(exam.getQuestionSum());
        Set<Integer> oldQuestionIds = oldSum.items().stream()
            .map(QuestionSumItem::questionId).collect(Collectors.toSet());
        Set<Integer> newQuestionIds = items.stream()
            .map(QuestionSumItem::questionId).collect(Collectors.toSet());

        // 移除的题目 use -= 1
        for (Integer qId : oldQuestionIds) {
            if (!newQuestionIds.contains(qId)) {
                questionService.decrementUse(qId);
            }
        }
        // 新增的题目 use += 1
        for (Integer qId : newQuestionIds) {
            if (!oldQuestionIds.contains(qId)) {
                questionService.incrementUse(qId);
            }
        }

        // 6. 更新 exam
        exam.setExam(req.exam());
        exam.setStarttime(req.starttime());
        exam.setEndtime(req.endtime());
        exam.setQuestionSum(toJson(questionSum));
        Exam saved = examRepository.save(exam);

        return toVO(saved);
    }

    // 参考 M03-Exam-Assembly.md §7 业务规则5 — 仅 draft 可删除
    @Transactional(rollbackFor = Exception.class)
    public void deleteExam(Integer id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4301, "考试不存在"));
        if (exam.getStatus() != ExamStatus.draft) {
            throw new BusinessException(4303, "仅草稿状态可删除");
        }
        // 删除前回退所有题目的 use 统计
        QuestionSum sum = parseQuestionSum(exam.getQuestionSum());
        for (QuestionSumItem item : sum.items()) {
            questionService.decrementUse(item.questionId());
        }
        examRepository.delete(exam);
    }

    // 别名方法，用于兼容旧测试代码
    public void delete(Integer id) {
        deleteExam(id);
    }

    // 别名方法，用于兼容旧测试代码
    public ExamVO findById(Integer id) {
        return getExamById(id);
    }

    // 别名方法，用于兼容旧测试代码
    public ExamVO edit(Integer id, ExamCreateManualReq req) {
        return updateExam(id, req);
    }

    // 参考 M03-Exam-Assembly.md §7.4 — 分页查询考试列表（教师/管理员）
    public PageResult<ExamVO> listExams(Integer page, Integer size, ExamStatus status) {
        List<Exam> exams;
        if (status != null) {
            // 按状态过滤
            exams = examRepository.findByStatus(status);
        } else {
            exams = examRepository.findAll(Sort.by("id").descending());
        }
        List<ExamVO> voList = exams.stream()
            .map(this::toVOWithResolvedStatus)
            .toList();
        return toPageResult(voList, page, size);
    }

    // 学生可参加的考试列表
    public List<ExamForStudentVO> listAvailableExams() {
        LocalDateTime now = LocalDateTime.now();
        List<Exam> exams = examRepository.findByStatusNot(ExamStatus.draft);
        return exams.stream()
            .filter(e -> {
                ExamStatus status = resolveCurrentStatus(e, now);
                return status == ExamStatus.publish || status == ExamStatus.running;
            })
            .map(e -> {
                try {
                    return getExamForStudent(e.getId());
                } catch (BusinessException ex) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    // 手动构造分页结果（因需要内存中状态解析）
    private PageResult<ExamVO> toPageResult(List<ExamVO> content, int page, int size) {
        PageResult<ExamVO> r = new PageResult<>();
        r.setContent(content);
        r.setTotalElements(content.size());
        r.setTotalPages((int) Math.ceil((double) content.size() / size));
        r.setPage(page);
        r.setSize(size);
        return r;
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

    /**
     * 根据题目 ID 和 img 标志解析图片 URL。
     * 参考 02-Data-Dictionary.md §4.2.2 — img 路径匹配规则
     * img=1 时，在 Data/img/ 目录下查找 {questionId}.{ext} 图片
     * 支持 .png / .jpg / .jpeg / .gif 扩展名，找到第一个存在的即返回
     * @return 相对 URL 路径（如 /api/v1/images/4.png），若无图则返回 null
     */
    private String resolveImageUrl(Integer questionId, Integer img) {
        if (img == null || img != 1) {
            return null;
        }
        // 在 Data/img/ 目录下查找对应扩展名的图片
        String imgDir = userDir + "/../Data/img/";
        for (String ext : IMG_EXTENSIONS) {
            File file = new File(imgDir + questionId + ext);
            if (file.exists()) {
                return "/api/v1/images/" + questionId + ext;
            }
        }
        return null; // 标记有图但文件不存在
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
