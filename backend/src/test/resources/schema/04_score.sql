-- ============================================================
-- 表名：score（分数表）
-- 模块：M04 分数与统计
-- 参考：wiki/02-Data-Dictionary.md §4.4 分数表 score
-- 数据库：SQLite（xerial sqlite-jdbc）
-- ============================================================
-- 说明：
--   1. 主键 id 使用 INTEGER PRIMARY KEY AUTOINCREMENT。
--   2. user / exam 为物理外键（INTEGER）→ user.id / exam.id；
--      4 张表中仅有的两处物理外键。
--   3. all 为总分（INTEGER，非负）；与 SQL 关键字 ALL 同名，必须双引号转义。
--   4. detail 为 JSON 文本，承载逐题明细 + summary（见 §4.4.1）。
--   5. UNIQUE (user, exam) 保证一人一考仅一条记录；重复提交走 UPSERT。
--   6. 重要：SQLite 外键默认关闭，必须在每次连接执行：
--        PRAGMA foreign_keys = ON;
--      外键才会真正生效。
--   7. 删除 user / exam 前必须先清理本表记录。
-- ============================================================

CREATE TABLE IF NOT EXISTS "score" (
    "id"     INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "user"   INTEGER NOT NULL,
    "exam"   INTEGER NOT NULL,
    "all"    INTEGER NOT NULL
                  CHECK ("all" >= 0),
    "detail" TEXT    NOT NULL,
    FOREIGN KEY ("user") REFERENCES "user"("id")   ON DELETE RESTRICT,
    FOREIGN KEY ("exam") REFERENCES "exam"("id")   ON DELETE RESTRICT
);

-- 唯一约束：一人一考仅一条
CREATE UNIQUE INDEX IF NOT EXISTS "uk_score_user_exam" ON "score" ("user", "exam");

-- 索引：个人成绩查询
CREATE INDEX        IF NOT EXISTS "idx_score_user" ON "score" ("user");

-- 索引：考试聚合统计
CREATE INDEX        IF NOT EXISTS "idx_score_exam" ON "score" ("exam");
