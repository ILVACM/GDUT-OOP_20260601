package com.cps.backend.modules.M03examassembly.repository;

import com.cps.backend.modules.M03examassembly.entity.Exam;
import com.cps.backend.modules.M03examassembly.enums.ExamStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Exam 实体的 JPA Repository。
 *
 * <p>参考 02-Data-Dictionary.md §4.3、M03-Exam-Assembly。</p>
 *
 * <p><b>关键方法</b>：</p>
 * <ul>
 *   <li>{@link #findByStatus(ExamStatus)} - 按状态筛选（参考 01-Global-Standards.md §4.4 示例）。</li>
 *   <li>{@link #findByTimeWindow(String, String)} - 时间窗查询（状态机自动判定，参考 §4.3.2、§7.4）。</li>
 * </ul>
 *
 * <p>注意：{@code publish}→{@code running}→{@code done} 状态转换由查询时实时计算（参考 §4.3.2）；
 * 本 Repository 仅暴露显式 status 字段的查询，状态机业务逻辑由 Service 层负责（参考 M03）。</p>
 */
@Repository
public interface ExamRepository extends JpaRepository<Exam, Integer> {

    /**
     * 按状态查询。
     * <p>参考 01-Global-Standards.md §4.4 Repository 示例。</p>
     */
    @EntityGraph(attributePaths = {})
    List<Exam> findByStatus(ExamStatus status);

    /**
     * 时间窗查询：返回 {@code starttime} 在 {@code [from, to]} 区间内的考试（按 {@code starttime} 升序）。
     * <p>参数为 ISO 8601 字符串，用于定时任务扫描、按时间窗筛选（参考 §4.3.2、§7.4）。</p>
     */
    @EntityGraph(attributePaths = {})
    @Query("SELECT e FROM Exam e WHERE e.starttime BETWEEN :from AND :to ORDER BY e.starttime ASC")
    List<Exam> findByTimeWindow(@Param("from") String from,
                                 @Param("to") String to);

    /**
     * 状态非 done 的考试列表（用于状态机定时同步）。
     */
    @EntityGraph(attributePaths = {})
    List<Exam> findByStatusNot(ExamStatus status);
}
