-- ============================================================
-- 表名：user（用户表）
-- 模块：M01 用户认证与权限
-- 参考：wiki/02-Data-Dictionary.md §4.1 用户表 user
-- 数据库：SQLite（xerial sqlite-jdbc）
-- ============================================================
-- 说明：
--   1. 主键 id 使用 INTEGER PRIMARY KEY AUTOINCREMENT（SQLite 自增）。
--   2. type 为枚举字段，TEXT 存储 UserType.name() 字面量：
--        student / teacher / admin
--   3. status 为 Boolean（0=禁用，1=启用），INTEGER 存储。
--   4. password 严禁明文存储（应用层 BCrypt 哈希）。
--   5. name 建议 UNIQUE，避免重名。
-- ============================================================

CREATE TABLE IF NOT EXISTS "user" (
    "id"       INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "name"     TEXT    NOT NULL,
    "password" TEXT    NOT NULL,
    "type"     TEXT    NOT NULL DEFAULT 'student'
                     CHECK ("type" IN ('student', 'teacher', 'admin')),
    "status"   INTEGER NOT NULL DEFAULT 1
                     CHECK ("status" IN (0, 1))
);

-- 索引：登录按 name 精确匹配（UNIQUE 防重名）
CREATE UNIQUE INDEX IF NOT EXISTS "idx_user_name" ON "user" ("name");

-- 索引：按角色筛选
CREATE INDEX        IF NOT EXISTS "idx_user_type" ON "user" ("type");
