package com.cps.backend.modules.M03examassembly.dto;

import java.util.List;

// 参考 M03-Exam-Assembly.md §5.2 — 学生视角考试 VO（不含答案）
public record ExamForStudentVO(
    Integer id,
    String exam,
    String starttime,
    String endtime,
    List<ExamQuestionForStudentVO> questionItems
) {}
