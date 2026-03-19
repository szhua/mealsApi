-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    open_id VARCHAR(64) UNIQUE,
    union_id VARCHAR(64),
    phone VARCHAR(20),
    nickname VARCHAR(64),
    avatar VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 分类表
CREATE TABLE IF NOT EXISTS t_category (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) UNIQUE NOT NULL,
    name VARCHAR(32) NOT NULL,
    icon VARCHAR(32),
    sort INT DEFAULT 0
);

-- 菜品表
CREATE TABLE IF NOT EXISTS t_dish (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(64) NOT NULL,
    image VARCHAR(512),
    category_id BIGINT,
    calories INT DEFAULT 0,
    protein INT DEFAULT 0,
    carbs INT DEFAULT 0,
    fat INT DEFAULT 0,
    meal_types VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 规划表
CREATE TABLE IF NOT EXISTS t_plan (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    weekly_reset BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 每日规划表
CREATE TABLE IF NOT EXISTS t_plan_day (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    date DATE NOT NULL,
    breakfast VARCHAR(1024) DEFAULT '[]',
    lunch VARCHAR(1024) DEFAULT '[]',
    dinner VARCHAR(1024) DEFAULT '[]',
    UNIQUE(plan_id, date)
);

-- 打卡记录表
CREATE TABLE IF NOT EXISTS t_streak (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    current INT DEFAULT 0,
    longest INT DEFAULT 0,
    last_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 打卡历史表
CREATE TABLE IF NOT EXISTS t_check_in (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    check_date DATE NOT NULL,
    UNIQUE(user_id, check_date)
);

-- 初始化分类数据 (使用 INSERT ... ON CONFLICT 避免重复插入)
INSERT INTO t_category (code, name, icon, sort) VALUES
('staple', '主食', 'rice', 1),
('meat', '肉类', 'meat', 2),
('vegetable', '蔬菜', 'vegetable', 3),
('soup', '汤类', 'soup', 4),
('fruit', '水果', 'fruit', 5),
('other', '其他', 'other', 6)
ON CONFLICT (code) DO NOTHING;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_dish_user ON t_dish(user_id);
CREATE INDEX IF NOT EXISTS idx_dish_category ON t_dish(category_id);
CREATE INDEX IF NOT EXISTS idx_plan_user ON t_plan(user_id);
CREATE INDEX IF NOT EXISTS idx_plan_date ON t_plan(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_plan_day_plan ON t_plan_day(plan_id);
CREATE INDEX IF NOT EXISTS idx_check_in_user ON t_check_in(user_id);