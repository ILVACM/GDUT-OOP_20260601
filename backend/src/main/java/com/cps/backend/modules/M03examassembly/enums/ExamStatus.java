package com.cps.backend.modules.M03examassembly.enums;

import lombok.Getter;

/**
 * 考试状态枚举。
 * <p>
 * SQLite 中以字符串字面量存储（与 {@link #name()} 一致）。
 * 对应字段：{@code exam.status}。
 * </p>
 *
 * <p>状态机：</p>
 * <pre>
 *   draft  --发布--&gt;  publish  --(starttime &lt;= now &lt; endtime)--&gt;  running  --(now &gt;= endtime)--&gt;  done
 *   draft  &lt;--撤回--  publish
 *   done 为终态。
 * </pre>
 *
 * <p>参考 02-Data-Dictionary.md §5.3 考试状态枚举、§4.3.2 状态机。</p>
 */
@Getter
public enum ExamStatus {

    /** 草稿，未发布：可编辑、删除、发布 */
    draft("草稿"),

    /** 已发布，等待开考：可撤回为 draft */
    publish("已发布"),

    /** 进行中（{@code starttime <= now < endtime}）：仅查看、提交答卷 */
    running("进行中"),

    /** 已结束（{@code now >= endtime}）：仅查看、统计；终态 */
    done("已结束");

    private final String description;

    ExamStatus(String description) {
        this.description = description;
    }
}
