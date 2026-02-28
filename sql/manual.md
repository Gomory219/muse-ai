
# MySQL → PostgreSQL 迁移指南

> 专为习惯MySQL的用户准备的PostgreSQL(psql)快速参考手册

---

## 一、psql 常用命令

### 连接与退出

| 操作 | MySQL | PostgreSQL |
|------|-------|------------|
| 连接数据库 | `mysql -u root -p` | `psql -U postgres -d dbname` |
| 指定主机端口 | `mysql -h host -P 3306` | `psql -h host -p 5432` |
| 退出 | `exit;` 或 `quit;` | `\q` |

### 元数据查询命令

| 操作 | psql命令 | 说明 |
|------|----------|------|
| 帮助 | `\?` | 查看所有psql命令 |
| SQL帮助 | `\h` | 查看SQL语法帮助 |
| 列出数据库 | `\l` 或 `\list` | 类似 `SHOW DATABASES;` |
| 切换数据库 | `\c dbname` | 类似 `USE dbname;` |
| 列出表 | `\dt` | 类似 `SHOW TABLES;` |
| 表结构 | `\d tablename` | 类似 `DESC tablename;` |
| 详细表结构 | `\d+ tablename` | 包含注释、大小等 |
| 索引信息 | `\di tablename` | 类似 `SHOW INDEX` |
| 执行SQL文件 | `\i /path/file.sql` | 类似 `source file.sql` |

### 系统信息

```sql
-- PostgreSQL 版本
SELECT version();

-- 当前用户
SELECT current_user;

-- 当前时间
SELECT now();
```

---

## 二、SQL 方言差异对照

### 2.1 数据类型映射

| MySQL | PostgreSQL | 说明 |
|-------|------------|------|
| `TINYINT` | `SMALLINT` | Postgres无TINYINT |
| `BIGINT` | `BIGINT` | |
| `FLOAT` | `REAL` | |
| `DOUBLE` | `DOUBLE PRECISION` | |
| `DECIMAL(m,n)` | `NUMERIC(m,n)` | |
| `VARCHAR(n)` | `VARCHAR(n)` / `TEXT` | TEXT性能接近VARCHAR |
| `DATETIME` | `TIMESTAMP` | |
| `ENUM` | 自定义枚举或 TEXT + CHECK | |

### 2.2 自增主键

| MySQL | PostgreSQL |
|-------|------------|
| `id INT AUTO_INCREMENT PRIMARY KEY` | `id SERIAL PRIMARY KEY` |
| `id BIGINT AUTO_INCREMENT` | `id BIGSERIAL` |

**获取刚插入的自增ID：**
```sql
-- MySQL
SELECT LAST_INSERT_ID();

-- PostgreSQL (推荐)
INSERT INTO table_name (...) VALUES (...) RETURNING id;
```

### 2.3 注释语法

| MySQL | PostgreSQL |
|-------|------------|
| 行内注释 | `name VARCHAR(50) COMMENT '用户名'` | `COMMENT ON COLUMN table.name IS '用户名';` |
| 表注释 | `CREATE TABLE t (...) COMMENT='表注释';` | `COMMENT ON TABLE t IS '表注释';` |

### 2.4 索引与约束

| MySQL | PostgreSQL |
|-------|------------|
| 唯一索引 | `UNIQUE KEY uk_name (col)` | `CONSTRAINT uk_name UNIQUE (col)` |
| 普通索引 | `INDEX idx_name (col)` | `CREATE INDEX idx_name ON table(col);` |

### 2.5 引号与标识符

| MySQL | PostgreSQL |
|-------|------------|
| 标识符引用 | \`反引号\` | "双引号" |
| 字符串 | '单引号' | '单引号' |
| 大小写敏感 | Windows下不敏感 | 始终敏感（无引号自动转小写） |

**关键字冲突（重要！）：**
```sql
-- 'user' 是PostgreSQL关键字
CREATE TABLE "user" (...);  -- 需要双引号

-- 推荐：使用非关键字
CREATE TABLE users (...);
CREATE TABLE app_user (...);
```

### 2.6 常用函数对照

| 功能 | MySQL | PostgreSQL |
|------|-------|------------|
| 空值处理 | `IFNULL(x,y)` | `COALESCE(x,y)` |
| 字符串拼接 | `CONCAT(a,b)` | `a \|\| b` |
| 随机数 | `RAND()` | `RANDOM()` |
| 当前时间+1天 | `DATE_ADD(NOW(), INTERVAL 1 DAY)` | `NOW() + INTERVAL '1 day'` |
| 日期格式化 | `DATE_FORMAT(dt, '%Y-%m-%d')` | `TO_CHAR(dt, 'YYYY-MM-DD')` |
| 字符串转日期 | `STR_TO_DATE(str, fmt)` | `TO_DATE(str, fmt)` |
| 正则匹配 | `REGEXP` | `~` (区分大小写) / `~*` (不区分) |

### 2.7 布尔值

| MySQL | PostgreSQL |
|-------|------------|
| 使用 TINYINT(1) | 真正的 BOOLEAN 类型 |
| `WHERE is_active = 1` | `WHERE is_active = TRUE` 或 `WHERE is_active` |

### 2.8 自动更新时间戳

```sql
-- MySQL
updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

-- PostgreSQL - 需要触发器
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updateTime = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_table_modtime
    BEFORE UPDATE ON your_table
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();
```

---

## 三、常见陷阱

| 陷阱 | 说明 | 解决方案 |
|------|------|----------|
| 反引号 vs 双引号 | MySQL用反引号，Postgres用双引号 | 避免使用保留字，无需引号 |
| user关键字 | `user`是保留字 | 改用`users`或`app_user` |
| INSERT SET语法 | MySQL支持 | Postgres不支持，用标准INSERT |
| GROUP BY严格度 | MySQL宽松 | Postgres要求所有非聚合列在GROUP BY中 |

---

## 四、psql 快捷速查卡

```
\l     列出所有数据库
\c db  连接数据库
\dt    列出所有表
\d t   查看表结构
\d+ t  表结构+注释
\di    列出索引
\du    列出用户
\x     切换横向显示
\q     退出
```

