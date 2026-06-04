-- ============================================================
-- 表名：exam（考试表）
-- 模块：M03 考试与组卷
-- 参考：wiki/02-Data-Dictionary.md §4.3 考试表 exam
-- 数据库：SQLite（xerial sqlite-jdbc）
-- ============================================================
-- 说明：
--   1. 主键 id 使用 INTEGER PRIMARY KEY AUTOINCREMENT。
--   2. exam（列名）为考试名称（与 v1.x 的 name 重命名以避免关键字冲突）。
--   3. status 为枚举字段，TEXT 存储 ExamStatus.name() 字面量：
--        draft / publish / running / done
--      状态机：
--        draft  --发布-->  publish  --(starttime<=now<endtime)-->  running  --(now>=endtime)-->  done
--        draft  <--撤回--  publish
--        done 为终态。
--   4. starttime / endtime 为 ISO 8601 字符串（TEXT 存储）。
--   5. question_sum 为 JSON 快照（见 §4.3.1），组卷时一次性写入，
--      题目后续修改/删除不影响已组卷考试。
--   6. 本表无物理外键；与 score 通过 score.exam 物理外键反向引用。
-- ============================================================

CREATE TABLE IF NOT EXISTS "exam" (
    "id"            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "exam"          TEXT    NOT NULL,
    "status"        TEXT    NOT NULL DEFAULT 'draft'
                          CHECK ("status" IN ('draft', 'publish', 'running', 'done')),
    "starttime"     TEXT    NOT NULL,
    "endtime"       TEXT    NOT NULL,
    "question_sum"  TEXT    NOT NULL,
    CHECK ("endtime" > "starttime")
);

-- 索引：按状态筛选
CREATE INDEX IF NOT EXISTS "idx_exam_status" ON "exam" ("status");

-- 索引：时间窗查询（发布/进行中/已结束扫描）
CREATE INDEX IF NOT EXISTS "idx_exam_time"   ON "exam" ("starttime", "endtime");
