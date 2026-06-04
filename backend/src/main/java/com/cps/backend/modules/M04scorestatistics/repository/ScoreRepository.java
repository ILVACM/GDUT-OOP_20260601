package com.cps.backend.modules.M04scorestatistics.repository;

import com.cps.backend.modules.M04scorestatistics.entity.Score;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Score 实体的 JPA Repository。
 *
 * <p>参考 02-Data-Dictionary.md §4.4、M04-Score-Statistics。</p>
 *
 * <p><b>关键方法</b>：</p>
 * <ul>
 *   <li>{@link #findByUserAndExam(Integer, Integer)} - 一人一考查询（参考 §4.4 UNIQUE(user, exam)）。</li>
 *   <li>{@link #upsertScore(Integer, Integer, Integer, String)} - 一人一考 UPSERT（重复提交走 UPDATE，参考 §7.5.1）。</li>
 *   <li>{@link #findByUser(Integer)} / {@link #findByExam(Integer)} - 个人成绩查询 / 考试聚合统计（参考 §4.4 索引）。</li>
 * </ul>
 *
 * <p>UPSERT 必须在 Service 层事务内执行（参考 M04 §7.5.1）。</p>
 */
@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {

    /**
     * 按 user + exam 查询（一人一考唯一）。
     */
    @EntityGraph(attributePaths = {})
    Optional<Score> findByUserAndExam(Integer user, Integer exam);

    /**
     * 个人成绩列表。
     */
    @EntityGraph(attributePaths = {})
    List<Score> findByUser(Integer user);

    /**
     * 考试聚合统计（参与人数 / 平均分等）。
     */
    @EntityGraph(attributePaths = {})
    List<Score> findByExam(Integer exam);

    /**
     * 一人一考 UPSERT：若记录已存在则覆盖 all / detail，否则插入。
     * <p>SQLite 语法：{@code ON CONFLICT(user, exam) DO UPDATE SET all = excluded.all, detail = excluded.detail}。</p>
     * <p>调用方必须在 Service 层事务内执行（参考 M04 §7.5.1）。</p>
     */
    @Modifying
    @Query(value = """
            INSERT INTO score ("user", "exam", "all", "detail")
            VALUES (:userId, :examId, :all, :detail)
            ON CONFLICT("user", "exam") DO UPDATE SET
                "all" = excluded."all",
                "detail" = excluded."detail"
            """, nativeQuery = true)
    int upsertScore(@Param("userId") Integer userId,
                    @Param("examId") Integer examId,
                    @Param("all") Integer all,
                    @Param("detail") String detail);
}
