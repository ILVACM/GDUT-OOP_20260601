package com.cps.backend.modules.M02questionbank.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M02questionbank.dto.*;
import com.cps.backend.modules.M02questionbank.entity.Question;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import com.cps.backend.modules.M02questionbank.repository.QuestionRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 参考 M02-Question-Bank.md §7/§8/§9
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

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
    public List<QuestionVO> batchCreate(List<QuestionCreateReq> reqs) {
        if (reqs.size() > 100) {
            throw new BusinessException(4200, "单次批量导入不超过100题");
        }
        return reqs.stream().map(req -> {
            validateAnswerJson(req.type(), req.answer());
            Question question = new Question();
            question.setType(req.type());
            question.setContext(req.context());
            question.setImg(req.img() != null ? req.img() : 0);
            question.setAnswer(req.answer());
            question.setUse(0);
            question.setCorrect(0);
            return toVO(questionRepository.save(question));
        }).toList();
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
