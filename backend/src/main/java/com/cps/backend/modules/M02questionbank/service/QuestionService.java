package com.cps.backend.modules.M02questionbank.service;

import com.cps.backend.common.api.PageResult;
import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M02questionbank.dto.*;
import com.cps.backend.modules.M02questionbank.entity.Question;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import com.cps.backend.modules.M02questionbank.repository.QuestionRepository;
import com.cps.backend.modules.M03examassembly.repository.ExamRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 参考 M02-Question-Bank.md §7/§8/§9
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final com.cps.backend.modules.M03examassembly.repository.ExamRepository examRepository;

    // 参考 M02-Question-Bank.md §8 业务规则5 — 答案强校验
    @Transactional(rollbackFor = Exception.class)
    public QuestionVO create(QuestionCreateReq req) {
        validateAnswerJson(req.type(), req.answer());
        Question question = new Question();
        question.setType(req.type());
        question.setContext(req.context());
        question.setImg(req.img() != null ? req.img() : 0);
        question.setAnswer(req.answer());
        question.setUse(0);
        question.setCorrect(0);
        Question saved = questionRepository.save(question);
        return toVO(saved);
    }

    // 参考 M02-Question-Bank.md §8 业务规则4 — 批量导入限制
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult batchCreate(List<QuestionCreateReq> reqs) {
        if (reqs.size() > 100) {
            throw new BusinessException(4200, "单次批量导入不超过100题");
        }
        List<BatchImportResult.ImportError> errors = new ArrayList<>();
        List<Question> toSave = new ArrayList<>();

        for (int i = 0; i < reqs.size(); i++) {
            QuestionCreateReq req = reqs.get(i);
            try {
                validateAnswerJson(req.type(), req.answer());
                Question question = new Question();
                question.setType(req.type());
                question.setContext(req.context());
                question.setImg(req.img() != null ? req.img() : 0);
                question.setAnswer(req.answer());
                question.setUse(0);
                question.setCorrect(0);
                toSave.add(question);
            } catch (BusinessException e) {
                errors.add(new BatchImportResult.ImportError(i, e.getMessage()));
            }
        }

        List<Question> saved = questionRepository.saveAll(toSave);
        int successCount = saved.size();
        int failCount = errors.size();

        return new BatchImportResult(successCount, failCount, errors);
    }

    public QuestionVO findById(Integer id) {
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4201, "题目不存在"));
        return toVO(question);
    }

    public Page<QuestionVO> search(QuestionQueryReq req) {
        Pageable pageable = PageRequest.of(req.page(), req.size(), Sort.by("id").descending());
        return questionRepository.searchByKeyword(req.type(), req.keyword(), pageable)
            .map(this::toVO);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionVO update(Integer id, QuestionUpdateReq req) {
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4201, "题目不存在"));
        // 参考 M02-Question-Bank.md §8 业务规则5 — 更新时也需校验 answer JSON
        validateAnswerJson(question.getType(), req.answer());
        question.setContext(req.context());
        question.setImg(req.img() != null ? req.img() : question.getImg());
        question.setAnswer(req.answer());
        Question saved = questionRepository.save(question);
        return toVO(saved);
    }

    // 参考 M02-Question-Bank.md §8 业务规则3 — 硬删除
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        if (!questionRepository.existsById(id)) {
            throw new BusinessException(4201, "题目不存在");
        }
        // 参考 M02-Question-Bank.md §8 业务规则3 — 删除被引用题目的警告
        List<com.cps.backend.modules.M03examassembly.entity.Exam> allExams = examRepository.findAll();
        boolean isReferenced = allExams.stream().anyMatch(exam -> {
            try {
                String qs = exam.getQuestionSum();
                return qs != null && qs.contains("\"questionId\":" + id);
            } catch (Exception e) {
                return false;
            }
        });
        if (isReferenced) {
            log.warn("题目 id={} 已被考试引用，删除后历史考试快照不受影响但统计将停留在删除时", id);
        }
        questionRepository.deleteById(id);
    }

    // 参考 M02-Question-Bank.md §9.2 — 题内统计自维护（组卷时调用）
    @Transactional(rollbackFor = Exception.class)
    public void incrementUse(Integer questionId) {
        int updated = questionRepository.incrementUse(questionId);
        if (updated == 0) {
            throw new BusinessException(4201, "题目不存在，无法更新统计");
        }
    }

    // 参考 M02-Question-Bank.md §9.2 — 题内统计自维护（判分时调用）
    @Transactional(rollbackFor = Exception.class)
    public void incrementCorrect(Integer questionId) {
        int updated = questionRepository.incrementCorrect(questionId);
        if (updated == 0) {
            throw new BusinessException(4201, "题目不存在，无法更新统计");
        }
    }

    // 参考 M03-Exam-Assembly.md §3.3 — 考试编辑时回退 use 统计
    @Transactional(rollbackFor = Exception.class)
    public void decrementUse(Integer questionId) {
        int updated = questionRepository.decrementUse(questionId);
        if (updated == 0) {
            throw new BusinessException(4201, "题目不存在或use已为0，无法回退统计");
        }
    }

    // 参考 M03-Exam-Assembly.md — 自动组卷单题获取模式
    /**
     * 随机获取单道题目（用于自动组卷逐题筛选）。
     * 不操作数据库（仅查询），返回题目预览信息（不含答案）。
     */
    public QuestionPreviewVO getRandomQuestion(QuestionType type, List<Integer> excludedIds) {
        return questionRepository.findRandomQuestion(type, excludedIds)
            .map(q -> new QuestionPreviewVO(
                q.getId(),
                q.getType(),
                q.getContext(),
                q.getImg()
            ))
            .orElseThrow(() -> new BusinessException(4203, "没有符合条件的可用题目"));
    }

    // 参考 M02-Question-Bank.md §10 — 答案反序列化的多态处理
    private void validateAnswerJson(QuestionType type, String answerJson) {
        try {
            Answer answer = switch (type) {
                case SingleChoice -> objectMapper.readValue(answerJson, SingleChoiceAnswer.class);
                case MultipleChoice -> objectMapper.readValue(answerJson, MultipleChoiceAnswer.class);
                case Judge -> objectMapper.readValue(answerJson, JudgeAnswer.class);
                case Fill -> objectMapper.readValue(answerJson, FillAnswer.class);
                case Essay -> objectMapper.readValue(answerJson, EssayAnswer.class);
            };
            // 额外业务校验
            switch (answer) {
                case SingleChoiceAnswer sc -> {
                    if (sc.correctOption() == null || sc.options() == null || sc.options().isEmpty()) {
                        throw new BusinessException(4202, "单选题答案必须包含correctOption和options");
                    }
                    if (!sc.options().contains(sc.correctOption())) {
                        throw new BusinessException(4202, "correctOption必须在options中");
                    }
                }
                case MultipleChoiceAnswer mc -> {
                    if (mc.correctOptions() == null || mc.correctOptions().isEmpty() || mc.options() == null || mc.options().isEmpty()) {
                        throw new BusinessException(4202, "多选题答案必须包含correctOptions和options");
                    }
                    if (!mc.options().containsAll(mc.correctOptions())) {
                        throw new BusinessException(4202, "correctOptions必须在options中");
                    }
                }
                case JudgeAnswer j -> {
                    if (j.correct() == null) {
                        throw new BusinessException(4202, "判断题答案必须包含correct");
                    }
                }
                case FillAnswer f -> {
                    if (f.blanks() == null || f.blanks().isEmpty()) {
                        throw new BusinessException(4202, "填空题答案必须包含blanks");
                    }
                }
                case EssayAnswer e -> {
                    if (e.reference() == null || e.reference().isBlank()) {
                        throw new BusinessException(4202, "简答题答案必须包含reference");
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (JacksonException e) {
            throw new BusinessException(4202, "答案JSON格式错误: " + e.getMessage());
        }
    }

    // Entity → VO 转换，参考 01-Global-Standards.md §4.3 DTO 隔离
    private QuestionVO toVO(Question question) {
        Double accuracy = question.getUse() > 0
            ? (double) question.getCorrect() / question.getUse()
            : null;
        return new QuestionVO(
            question.getId(),
            question.getType(),
            question.getContext(),
            question.getImg(),
            question.getAnswer(),
            question.getUse(),
            question.getCorrect(),
            accuracy
        );
    }
}
