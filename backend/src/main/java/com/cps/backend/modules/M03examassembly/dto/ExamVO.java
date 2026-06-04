package com.cps.backend.modules.M03examassembly.dto;

import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import java.util.List;

// 参考 M03-Exam-Assembly.md §5.2 — 教师视角考试 VO
public record ExamVO(
    Integer id,
    String exam,
    ExamStatus status,
    String starttime,
    String endtime,
    List<ExamQuestionVO> questionItems,
    Integer totalQuestions,
    Integer totalScore
) {}
