package com.cps.backend.modules.M02questionbank.repository;

import com.cps.backend.modules.M02questionbank.entity.Question;
import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Question 实体的 JPA Repository。
 *
 * <p>参考 02-Data-Dictionary.md §4.2、M02-Question-Bank。</p>
 *
 * <p><b>关键方法</b>：</p>
 * <ul>
 *   <li>{@link #findByType(QuestionType)} - 按题型筛选，对应 §4.2 索引 idx_question_type。</li>
 *   <li>{@link #searchByKeyword(QuestionType, String, Pageable)} - 按 type + context LIKE 关键字分页检索（参考 §7.2）。</li>
 *   <li>{@link #incrementUse(Integer)} - 组卷时 {@code use += 1}（参考 §4.2.3、§7.3.3）。</li>
 *   <li>{@link #incrementCorrect(Integer)} - 判分正确时 {@code correct += 1}（参考 §4.2.3、§7.5.1）。</li>
 * </ul>
 *
 * <p>题内统计自维护必须在 Service 层事务内执行（参考 M04 §7.5.1）；本接口仅提供基础设施。</p>
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    /**
     * 按题型查询。
     */
    @EntityGraph(attributePaths = {})
    List<Question> findByType(QuestionType type);

    /**
     * 按多个题型查询（用于自动组卷的 typeFilter）。
     */
    @EntityGraph(attributePaths = {})
    List<Question> findByTypeIn(List<QuestionType> types);

    /**
     * 按题型 + 关键字（context LIKE）分页检索。
     * <p>{@code type} 可为 null，表示不限制题型。</p>
     * <p>{@code keyword} 可为 null 或空串，表示不限制关键字。</p>
     */
    @EntityGraph(attributePaths = {})
    @Query("""
            SELECT q FROM Question q
            WHERE (:type IS NULL OR q.type = :type)
              AND (:keyword IS NULL OR :keyword = '' OR q.context LIKE CONCAT('%', :keyword, '%'))
            ORDER BY q.id ASC
            """)
    Page<Question> searchByKeyword(@Param("type") QuestionType type,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    /**
     * 组卷时被抽中，{@code use += 1}。
     * <p>DB CHECK 保证 {@code 0 <= use}（参考 §4.2）。</p>
     * <p>调用方必须在 Service 层事务内执行（参考 §4.2.3 / M04 §7.5.1）。</p>
     */
    @Modifying
    @Query("UPDATE Question q SET q.use = q.use + 1 WHERE q.id = :id")
    int incrementUse(@Param("id") Integer id);

    /**
     * 学生判分正确时，{@code correct += 1}。
     * <p>DB CHECK 保证 {@code 0 <= correct <= use}（参考 §4.2）。</p>
     * <p>调用方必须在 Service 层事务内执行（参考 M04 §7.5.1）。</p>
     */
    @Modifying
    @Query("UPDATE Question q SET q.correct = q.correct + 1 WHERE q.id = :id")
    int incrementCorrect(@Param("id") Integer id);

    /**
     * 考试编辑时回退 use 统计，{@code use -= 1}。
     * <p>仅当 {@code use > 0} 时才执行更新，防止出现负数违反 DB CHECK。</p>
     * <p>参考 M03-Exam-Assembly.md §3.3 — 考试编辑时回退 use 统计。</p>
     */
    @Modifying
    @Query("UPDATE Question q SET q.use = q.use - 1 WHERE q.id = :id AND q.use > 0")
    int decrementUse(@Param("id") Integer id);
}
