-- ============================================================
-- 表名：question（题目表）
-- 模块：M02 题库管理
-- 参考：wiki/02-Data-Dictionary.md §4.2 题目表 question
-- 数据库：SQLite（xerial sqlite-jdbc）
-- ============================================================
-- 说明：
--   1. 主键 id 使用 INTEGER PRIMARY KEY AUTOINCREMENT。
--   2. type 为枚举字段，TEXT 存储 QuestionType.name() 字面量：
--        SingleChoice / MultipleChoice / Judge / Fill / Essay
--   3. answer 为 JSON 文本，按 type 自适应 5 种结构（见 §4.2.1）。
--   4. img 为 Boolean（0=无图，1=有图）；img=1 时按 id 在
--        .\Data\img\{id}.{png|jpg|jpeg|gif} 匹配图片文件。
--   5. use / correct 为题内统计冗余字段：
--        - 组卷被抽中时 use += 1；
--        - 学生判分正确时 correct += 1；
--        - 不变量：0 <= correct <= use（CHECK 保证）。
--   6. 本表无物理外键；与 exam 通过 JSON 快照逻辑引用。
-- ============================================================

CREATE TABLE IF NOT EXISTS "question" (
    "id"      INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "type"    TEXT    NOT NULL
                    CHECK ("type" IN ('SingleChoice', 'MultipleChoice', 'Judge', 'Fill', 'Essay')),
    "context" TEXT    NOT NULL,
    "img"     INTEGER NOT NULL DEFAULT 0
                    CHECK ("img" IN (0, 1)),
    "answer"  TEXT    NOT NULL,
    "use"     INTEGER NOT NULL DEFAULT 0
                    CHECK ("use" >= 0),
    "correct" INTEGER NOT NULL DEFAULT 0
                    CHECK ("correct" >= 0 AND "correct" <= "use")
);

-- 索引：按题型检索
CREATE INDEX IF NOT EXISTS "idx_question_type" ON "question" ("type");

-- 索引：热度统计 / 自动组卷按 use 降权
CREATE INDEX IF NOT EXISTS "idx_question_use"  ON "question" ("use");
